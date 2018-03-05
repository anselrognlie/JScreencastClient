package com.ebbyware.core.thread;

class TimerRunLoopEvent implements RunLoopEvent {
    
    private RunLoopTimerRecord record;

    public TimerRunLoopEvent(RunLoopTimerRecord record) {
        this.record = record;
    }

    @Override
    public void invoke() {
        RunLoopTimer rlt = record.getRunLoopTimer();
        
        // this must be called on the main/gui thread
        rlt.getTask().run();
        record.completeEvent();
    }

    @Override
    public boolean shouldReplace(RunLoopEvent other) {
        // make sure the other is also a TimerRunLoopEvent
        if (! (other instanceof TimerRunLoopEvent)) {
            return false;
        }
        
        // replace if this event is from the same timer
        RunLoopTimer rlt = record.getRunLoopTimer();
        TimerRunLoopEvent event = (TimerRunLoopEvent)other;
        RunLoopTimer rltOther = event.record.getRunLoopTimer();
        
        return (rlt == rltOther);
    }

    @Override
    public String getDescription() {
        return "timer event";
    }

    @Override
    public void setHandled() {
        // nop
    }

}
