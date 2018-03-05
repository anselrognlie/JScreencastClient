package ui;

import java.io.IOException;

import com.ebbyware.core.log.Log;
import com.ebbyware.core.thread.RunLoop;
import com.ebbyware.service.echo.EchoClient;

public class EchoChannelTest {
    public static void main(String[] args) {
        
        try {
            RunLoop runloop = RunLoop.currentRunLoop();
            
            EchoClient client = new EchoClient();
            client.startOnRunLoop(runloop);
            
            while (! client.isDone()) {
                if (! client.isAwaitingResponse()) {
                    client.sendMessage("言葉");
                }
                
                runloop.runFor(16);
            }
            
            client.stop();
            
            Log.log("done.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
}
