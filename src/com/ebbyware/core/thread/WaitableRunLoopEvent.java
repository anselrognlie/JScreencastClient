package com.ebbyware.core.thread;

public interface WaitableRunLoopEvent extends RunLoopEvent {
    void waitForHandled();
    void notifyHandled();
}
