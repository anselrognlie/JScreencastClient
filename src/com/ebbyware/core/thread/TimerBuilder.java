package com.ebbyware.core.thread;

import java.util.Date;
import java.util.TimerTask;

class TimerBuilder {

    public static void updateTimer(RunLoopTimerRecord record) {
        RunLoop rl = record.getHost();

        TimerTask prevTask = record.getTask();
        if (prevTask != null) {
            prevTask.cancel();
        }

        TimerTask task = createTask(record);

        record.setTask(task);

        rl.scheduleTask(record);
    }

    private static TimerTask createTask(RunLoopTimerRecord record) {
        return new TimerTask() {
            @Override
            public void run() {
                RunLoopTimer rlt = record.getRunLoopTimer();
                RunLoop rl = record.getHost();

                rlt.setScheduled(false);

                rl.raiseTimerEvent(record);
                
                record.awaitEvent();
                
                // if the task has been canceled, don't reschedule
                if (rlt.isCanceled()) {
                    return;
                }

                // if the timer hasn't been rescheduled, either
                if (!rlt.isScheduled()) {
                    if (rlt.doesRepeat()) {
                        // refresh task
                        TimerTask task = createTask(record);
                        record.setTask(task);
                        
                        // 1. reschedule if periodic
                        rlt.setNextFireDate((new Date()).getTime() + rlt.getPeriod());
                    } else {
                        // 2. or prepare to invalidate
                    }
                }
            }
        };
    }

}
