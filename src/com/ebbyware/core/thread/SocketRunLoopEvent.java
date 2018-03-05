package com.ebbyware.core.thread;

import java.nio.channels.SelectionKey;

public class SocketRunLoopEvent implements WaitableRunLoopEvent {

    private RunLoopSocketSource source;
    private int operation;
    private Object monitor = new Object();
    private boolean handled = false;

    public SocketRunLoopEvent(RunLoopSocketSource source, int operation) {
        this.source = source;
        this.operation = operation;
    }

    @Override
    public void invoke() {
        source.operationOccured(operation);
    }

    @Override
    public boolean shouldReplace(RunLoopEvent other) {
        // allow one event per operation type per source

        if (!(other instanceof SocketRunLoopEvent)) {
            return false;
        }

        SocketRunLoopEvent event = (SocketRunLoopEvent) other;
        if (source == event.source && operation == event.operation) {
            return true;
        }

        return false;
    }

    @Override
    public String getDescription() {
        if (operation == SelectionKey.OP_READ) {
            return "socket read event";
        } else if (operation == SelectionKey.OP_WRITE) {
            return "socket write event";
        } else {
            return "unknown socket event";
        }
    }

    @Override
    public void waitForHandled() {
        synchronized (monitor) {
            while (!handled) {
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void notifyHandled() {
        synchronized (monitor) {
            handled = true;
            monitor.notifyAll();
        }
    }

    @Override
    public void setHandled() {
        notifyHandled();
    }

}
