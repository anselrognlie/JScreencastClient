package com.ebbyware.core.thread;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class RunLoop implements Runnable {

    // always acquire locks in the following order:
    // timerLock, sourceLock, ...
    // use orderedLock()

    private ArrayList<RunLoopEvent> queuedEvents;
    private ArrayList<RunLoopTimerRecord> timerTasks;
    private ArrayList<RunLoopSource> sources;
    private Object eventLock = new Object();
    private Object timerLock = new Object();
    private Object sourceLock = new Object();
    private Object runForLock = new Object();
    private Object monitor = new Object();
    private Timer timerForRunLoopTimers;
    private Timer timerForRunFor;
    private TimerTask runForTask;
    private Thread threadForNioSelectors;
    private Selector selector;
    private boolean done;
    private boolean swingEnabled = false;
    
    private static RunLoop current = null;

    private RunLoop(boolean swingEnabled) throws IOException {
        queuedEvents = new ArrayList<>();
        timerTasks = new ArrayList<>();
        sources = new ArrayList<>();
        done = false;
        this.swingEnabled = swingEnabled;
        timerForRunLoopTimers = new Timer("timerForRunLoopTimers", true);
        timerForRunFor = new Timer("timerForRunFor", true);

        initNioThread();
    }
    
    public static RunLoop currentRunLoop() throws IOException {
        if (current == null) {
            current = new RunLoop(false);
        }
        
        return current; 
    }

    public static RunLoop currentRunLoop(JFrame frame) throws IOException {
        if (current == null) {
            current = new RunLoop(frame != null);
        }
        
        return current; 
    }

    public boolean isDone() {
        return done;
    }

    public void shutdown() {
        setDone(true);

        // clean up all enqueued tasks
        shutdownTimers();
        timerTasks.clear();

        shutdownSources();
        sources.clear();

        // clean up run for
        synchronized (runForLock) {
            if (runForTask != null) {
                runForTask.cancel();
                runForTask = null;
            }
            timerForRunFor.cancel();
        }

        drainEventQueue();

        // notify any pending monitors
        synchronized (monitor) {
            monitor.notifyAll();
        }
    }

    public void addTimer(RunLoopTimer rlt) {
        // if this is already added to another loop, error
        if (rlt.getHost() != null) {
            throw new UnsupportedOperationException(
                    "Timer already added to RunLoop");
        }

        // add to timer list
        RunLoopTimerRecord rec = new RunLoopTimerRecord();
        rec.setRunLoopTimer(rlt);
        rec.setHost(this);
        rlt.setHost(rec);

        // create threaded timer to raise events
        TimerBuilder.updateTimer(rec);

        synchronized (timerLock) {
            timerTasks.add(rec);
        }
    }

    public void removeTimer(RunLoopTimer rlt) {
        boolean hosted = false;

        // does this timer even belong to this run loop?
        RunLoopTimerRecord rltHost;
        if ((rltHost = rlt.getHost()) != null) {
            if (rltHost.getHost() == this) {
                hosted = true;
            }
        }
        if (!hosted) {
            throw new UnsupportedOperationException(
                    "attempt to remove Timer from non-host RunLoop");
        }

        synchronized (timerLock) {
            // find the record that hosts this timer
            RunLoopTimerRecord found = null;
            int index = 0;
            for (RunLoopTimerRecord rec : timerTasks) {
                if (rec.getRunLoopTimer() == rlt) {
                    found = rec;
                    break;
                }

                ++index;
            }

            if (found == null) {
                throw new UnsupportedOperationException(
                        "unable to find Timer in host RunLoop");
            }

            found.getTask().cancel();
            timerTasks.remove(index);
            rlt.setHost(null);
            rlt.setCanceled(true);
        }

        checkIsDone();

        synchronized (monitor) {
            monitor.notifyAll();
        }
    }

    private void checkIsDone() {
        orderedLock(() -> {
            boolean doneFlag = true;

            // if we removed the last timer, and there are no other sources,
            // this loop is done
            if (timerTasks.size() > 0) {
                doneFlag = false;
            }

            if (sources.size() > 0) {
                doneFlag = false;
            }

            if (done) {
                timerForRunLoopTimers.cancel();
            }

            setDone(doneFlag);
        });

        if (done) {
            drainEventQueue();
        }
    }

    private void setDone(boolean isDone) {
        synchronized (monitor) {
            // update done and notify anything waiting
            done = isDone;
            monitor.notifyAll();
        }
    }

    private void orderedLock(Runnable task) {
        synchronized (timerLock) {
            synchronized (sourceLock) {
                task.run();
            }
        }
    }

    void raiseTimerEvent(RunLoopTimerRecord record) {
        // the scheduled timer should place a run loop timer event in
        // the queued events (replacing any as-yet unhandled event)
        // and notify the main thread

        // System.out.println("Raise timer thread: " +
        // Thread.currentThread().getId());
        TimerRunLoopEvent event = new TimerRunLoopEvent(record);
        enqueueEvent(event);
    }

    void scheduleTask(RunLoopTimerRecord record) {
        RunLoopTimer rlt = record.getRunLoopTimer();
        rlt.setScheduled(true);
        timerForRunLoopTimers.schedule(record.getTask(),
                new Date(rlt.getFireDate()));
    }

    void updateTask(RunLoopTimerRecord record) {
        TimerBuilder.updateTimer(record);
    }

    public void run() {
        while (!done) {
            // are there any pending events?
            processEvents();

            // wait for event notification
            waitForEvent();
        }
    }

    public void runFor(long ms) {
        if (done) {
            return;
        }

        // schedule a timer to fire when the requested time has expired
        // it will notify the monitor to allow main thread execution to
        // continue if no events have arrived
        synchronized (runForLock) {
            runForTask = new TimerTask() {
                @Override
                public void run() {
                    synchronized (monitor) {
                        monitor.notify();
                    }
                }
            };

            timerForRunFor.schedule(runForTask, ms);
        }

        // process pending events
        processEvents();

        // wait for event notification
        waitForEvent();

        synchronized (runForLock) {
            if (runForTask != null) {
                runForTask.cancel();
                runForTask = null;
            }
        }

        // process any event that got queued
        processEvents();
    }

    private void enqueueEvent(RunLoopEvent event) {
        if (!swingEnabled) {
            synchronized (eventLock) {
                // find any events that should be replaced
                ArrayList<RunLoopEvent> removeList = new ArrayList<>(
                        queuedEvents.size());
                for (RunLoopEvent evt : queuedEvents) {
                    if (event.shouldReplace(evt)) {
                        removeList.add(evt);
                    }
                }

                // remove them
                for (RunLoopEvent evt : removeList) {
                    queuedEvents.remove(evt);
                    // evt.setHandled();
                }

                // add the new event
                queuedEvents.add(event);

                // System.err.println(String.format("added event: %1s",
                // event.getDescription()));
                // System.err.println(String.format("queue length: %1d",
                // queuedEvents.size()));
            }

            synchronized (monitor) {
                monitor.notify();
            }
        } else {
            // dispatch directly to the ui thread
            SwingUtilities.invokeLater(() -> {
                event.invoke();
                event.setHandled();
            });
        }
    }

    private void processEvents() {
        RunLoopEvent event;
        while ((event = getNextEvent()) != null) {
            // handle the event
            handleEvent(event);

            // System.err.println(String.format("processed event: %1s",
            // event.getDescription()));
        }
    }

    private RunLoopEvent getNextEvent() {
        synchronized (eventLock) {
            if (queuedEvents.size() > 0) {
                RunLoopEvent event = queuedEvents.remove(0);
                return event;
            }
        }

        return null;
    }

    private void drainEventQueue() {
        synchronized (eventLock) {
            for (RunLoopEvent event : queuedEvents) {
                event.setHandled();
            }

            queuedEvents.clear();
        }
    }

    private void handleEvent(RunLoopEvent event) {
        // this is only called on the main thread, so just invoke the event
        event.invoke();
        event.setHandled();
    }

    private void shutdownTimers() {
        synchronized (timerLock) {
            for (RunLoopTimerRecord rec : timerTasks) {
                rec.getTask().cancel();
            }

            timerForRunLoopTimers.cancel();
        }
    }

    private void waitForEvent() {
        synchronized (monitor) {
            if (!done) {
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void shutdownSources() {
        synchronized (sourceLock) {
            for (RunLoopSource source : sources) {
                source.cancel();
            }
        }
    }

    private void initNioThread() throws IOException {
        // set up vars for handling nio selectors
        selector = Selector.open();

        // create the thread itself
        threadForNioSelectors = new Thread(() -> {
            do {
                try {
                    int readyCount = selector.selectNow();
                    if (readyCount > 0) {
                        try {
                            // there are things to process
                            Set<SelectionKey> selectedKeys = selector
                                    .selectedKeys();
                            Iterator<SelectionKey> keyIter = selectedKeys
                                    .iterator();

                            while (keyIter.hasNext()) {
                                SelectionKey key = keyIter.next();
                                RunLoopSource rls = (RunLoopSource) key
                                        .attachment();

                                if (key.isReadable()) {
                                    // enqueue read event
                                    WaitableRunLoopEvent event = rls
                                            .createEvent(SelectionKey.OP_READ);
                                    enqueueEvent(event);
                                    event.waitForHandled();
                                }

                                if (key.isWritable()) {
                                    // enqueue write event
                                    WaitableRunLoopEvent event = rls
                                            .createEvent(SelectionKey.OP_WRITE);
                                    enqueueEvent(event);
                                    event.waitForHandled();
                                }

                                keyIter.remove();
                            }

                            selectedKeys.clear();
                        } catch (CancelledKeyException e) {
                            // could log, but we don't really care
                            //e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } while (!done);
        }, "threadForNioSelectors");

        // threadForNioSelectors.setDaemon(true);
        threadForNioSelectors.start();
    }

    public void addSource(RunLoopSource rls) {
        synchronized (sourceLock) {
            sources.add(rls);
            rls.register(this);
        }
    }

    Selector getSelector() {
        return selector;
    }

    public void removeSource(RunLoopSource rls) {
        boolean hosted = false;

        // does this source even belong to this run loop?
        hosted = (rls.getHost() == this);

        if (!hosted) {
            throw new UnsupportedOperationException(
                    "attempt to remove RunLoopSource from non-host RunLoop");
        }

        synchronized (sourceLock) {
            // find the record that hosts this timer
            RunLoopSource found = null;
            int index = 0;
            for (RunLoopSource src : sources) {
                if (src == rls) {
                    found = src;
                    break;
                }

                ++index;
            }

            if (found == null) {
                throw new UnsupportedOperationException(
                        "unable to find RunLoopSource in host RunLoop");
            }

            found.cancel();
            sources.remove(index);
        }

        checkIsDone();

        synchronized (monitor) {
            monitor.notify();
        }
    }
}
