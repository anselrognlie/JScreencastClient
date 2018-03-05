package com.ebbyware.core.thread;

public interface RunLoopSource {
    void cancel();
    void register(RunLoop runloop);
    void operationOccured(int operation);
    WaitableRunLoopEvent createEvent(int operation);
    RunLoop getHost();
}
