package com.ebbyware.core.thread;

import java.util.Date;

public class RunLoopTimer {
    // all times are recorded in ms
    private long createdDate;
    private long fireDate;
    private long period;
    private boolean repeats;
    private Runnable task;
    private RunLoopTimerRecord host;
    private boolean scheduled;
    private boolean canceled;
    private String label;
    
    public static RunLoopTimer createTimer(long delay, Runnable task) {
        return createTimer(null, delay, task);
    }

    public static RunLoopTimer createRepeatingTimer(long period, Runnable task) {
        return createRepeatingTimer(null, period, task);
    }

    public static RunLoopTimer createRepeatingTimerWithDelay(long period, long delay, Runnable task) {
        return createRepeatingTimerWithDelay(null, period, delay, task);
    }

    public static RunLoopTimer createTimer(String label, long delay, Runnable task) {
        RunLoopTimer timer = new RunLoopTimer(label, delay, task);
        
        return timer;
    }

    public static RunLoopTimer createRepeatingTimer(String label, long period, Runnable task) {
        RunLoopTimer timer = new RunLoopTimer(label, 0, task);
        timer.setPeriod(period);
        
        return timer;
    }

    public static RunLoopTimer createRepeatingTimerWithDelay(String label, long period, long delay, Runnable task) {
        RunLoopTimer timer = new RunLoopTimer(label, delay, task);
        timer.setPeriod(period);
        
        return timer;
    }
    
    public String getLabel() {
        return label;
    }

    public long getFireDate() {
        return fireDate;
    }
    
    public long getPeriod() {
        return period;
    }

    public boolean doesRepeat() {
        return repeats;
    }
    
    public Runnable getTask() {
        return task;
    }
    
    public boolean isScheduled() {
        return scheduled;
    }
    
    public void setNextFireDate(long fireDate) {
        this.fireDate = fireDate;
        
        updateTimer();
    }
    
    // package visibility /////////////////////////////////////////////
    
    void setHost(RunLoopTimerRecord record) {
        host = record;
    }
    
    RunLoopTimerRecord getHost() {
        return host;
    }
    
    void setScheduled(boolean scheduled) {
        this.scheduled = scheduled;
    }

    boolean isCanceled() {
        return canceled;
    }

    void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
    
    // private ////////////////////////////////////////////////////
    
    private RunLoopTimer(String label, long delay, Runnable task) {
        createdDate = (new Date()).getTime();
        this.label = label;
        fireDate = createdDate + delay;
        this.task = task;
        scheduled = false;
        setCanceled(false);
    }
    
    private void setPeriod(long period) {
        this.period = period;
        
        repeats = (period != 0);
    }
    
    private void updateTimer() {
        // if not in a record, just ignore
        if (host == null) { return; }
        
        host.getHost().updateTask(host);
    }

}
