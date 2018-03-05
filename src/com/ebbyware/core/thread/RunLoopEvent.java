package com.ebbyware.core.thread;

interface RunLoopEvent {
    public void invoke();
    public boolean shouldReplace(RunLoopEvent other);
    public String getDescription();
    public void setHandled();
}
