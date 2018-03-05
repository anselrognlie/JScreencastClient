package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.ebbyware.core.container.Box;
import com.ebbyware.core.thread.RunLoop;
import com.ebbyware.core.thread.RunLoopTimer;

public class RunLoopUiTest extends JFrame {
    
    /**
     * 
     */
    private static final long serialVersionUID = 5093611760191725446L;
    
    private JTextArea textArea;
    private RunLoop runloop;
    private RunLoopTimer rlt;
    
    public RunLoopUiTest() {
        initComponents();
        initRunLoop();
    }

    private void initComponents() {
        // Create and set up the window.
        setTitle("RunLoopUiTest");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        textArea = new JTextArea(5, 20);
        JScrollPane scrollPane = new JScrollPane(textArea); 
        textArea.setEditable(false);

        scrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(400, 250));

        getContentPane().add(scrollPane, BorderLayout.CENTER);
        pack();
    }
    
    private void initRunLoop() {
        try {
            runloop = RunLoop.currentRunLoop(this);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Box<Boolean> isDone = new Box<>(false);
        
        Runnable r = new Runnable() {
            
            int count = 0;
            
            @Override
            public void run() {
                if (isDone.get()) { return; }
                
                ++count;
                
                textArea.append(String.format("Times: %1d\n", count));
                if (count >= 10) {
                    isDone.set(true);
                    shutdownRunLoop();
                } else {
                    // reschedule next run
                    rlt.setNextFireDate((new Date()).getTime() + 1000);
                }
            }
        };
        
        rlt = RunLoopTimer.createRepeatingTimerWithDelay("run counter", 10000, 0, r);
        
        runloop.addTimer(rlt);
    }
    
    private void shutdownRunLoop() {
        textArea.append("done.\n");
        
        runloop.removeTimer(rlt);
    }

    public static void main(String[] args) {
        // Schedule a job for the event-dispatching thread:
        // creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new RunLoopUiTest();
                frame.setVisible(true);
            }
        });
    }
}
