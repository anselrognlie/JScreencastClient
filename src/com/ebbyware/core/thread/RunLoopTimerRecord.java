package com.ebbyware.core.thread;

import java.util.TimerTask;

class RunLoopTimerRecord {
    private RunLoopTimer rlt;
    private TimerTask task;
    private Object monitor = new Object();
    private RunLoop host;
    private boolean completed;
    
    RunLoopTimer getRunLoopTimer() {
        return rlt;
    }
    
    void setRunLoopTimer(RunLoopTimer rlt) {
        this.rlt = rlt;
    }
    
    TimerTask getTask() {
        return task;
    }
    
    void setTask(TimerTask task) {
        synchronized(monitor) {
            this.task = task;
            completed = false;
        }
    }
    
    RunLoop getHost() {
        return host;
    }

    void setHost(RunLoop host) {
        this.host = host;
    }

    void awaitEvent() {
        synchronized (monitor) {
            if (! completed) {
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    void completeEvent() {
        synchronized (monitor) {
            completed = true;
            monitor.notify();
        }
    }
}
