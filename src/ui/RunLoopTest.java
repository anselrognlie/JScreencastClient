package ui;

import java.io.IOException;
import java.util.Date;

import com.ebbyware.core.container.Box;
import com.ebbyware.core.thread.RunLoop;
import com.ebbyware.core.thread.RunLoopTimer;

public class RunLoopTest {

    private RunLoopTimer rlt;

    public static void main(String[] args) {
        RunLoopTest app = new RunLoopTest();
        app.run();
    }

    public void run() {
        // System.out.println("Main thread: " + Thread.currentThread().getId());

        RunLoop runloop;
        Box<Boolean> isDone = new Box<>(false);

        try {
            runloop = RunLoop.currentRunLoop();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Runnable r = new Runnable() {

            int count = 0;

            @Override
            public void run() {
                if (isDone.get()) {
                    return;
                }

                ++count;

                // System.out.println("Timer task thread: " +
                // Thread.currentThread().getId());
                System.out.println("Times: " + count);
                if (count >= 10) {
                    isDone.set(true);
                } else {
                    // reschedule next run
                    rlt.setNextFireDate((new Date()).getTime() + 500);
                }
            }
        };

        rlt = RunLoopTimer.createRepeatingTimerWithDelay("run counter", 10000, 0, r);

        runloop.addTimer(rlt);

        while (!isDone.get()) {
            runloop.runFor(1000);
            System.out.println("loop.");
        }

        System.out.print("done.");

        // runloop.shutdown();
        runloop.removeTimer(rlt);
    }
}
