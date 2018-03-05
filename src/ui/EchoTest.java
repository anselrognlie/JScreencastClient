package ui;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.SelectionKey;
import java.nio.charset.*;

import com.ebbyware.core.net.*;
import com.ebbyware.core.thread.*;

public class EchoTest {
    int readMax = 5;
    boolean done = false;
    boolean okToSend = false;

    public static void main(String[] args) {
        EchoTest app = new EchoTest();
        try {
            app.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() throws IOException {
        short port = 13888;
        int ops = SelectionKey.OP_READ | SelectionKey.OP_WRITE;

        AddressIpv4 addr = new AddressIpv4(port);
        UdpNonblockingSocket socket = new UdpNonblockingSocket(addr, ops,
                (cbSocket, cbOp) -> {
                    this.handleCallback(cbSocket, cbOp);
                });
        socket.setBroadcast(true);

        RunLoopSource rls = new RunLoopSocketSource(socket);

        RunLoop rl = RunLoop.currentRunLoop();
        rl.addSource(rls);
        
        RunLoopTimer t = RunLoopTimer.createRepeatingTimer("pulsed send", 1000, () -> {
            if (! okToSend) { return; }

            ByteBuffer buf = ByteBuffer.allocate(8192);
            String msg = "言葉";
            CharBuffer cbuf = CharBuffer.wrap(msg.toCharArray());

            CharsetEncoder enc = Charset.forName("UTF-8").newEncoder();
            enc.encode(cbuf, buf, true);
            buf.flip();

            try {
                socket.send(buf,
                        new AddressIpv4(
                                (Inet4Address) InetAddress.getByAddress(
                                        new byte[] { (byte) 255, (byte) 255,
                                                (byte) 255, (byte) 255 }),
                                (short) 13887));
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.err.println(String.format("sent data %1s", msg));
        });
        
        rl.addTimer(t);

        while (!done) {
            rl.runFor(16);
        }

        // rl.shutdown();
        rl.removeSource(rls);
        rl.removeTimer(t);
        
        System.err.println("done.");
    }

    private void handleWrite(UdpNonblockingSocket socket) throws IOException {
        System.err.println("recv'd write notification.");
        okToSend = true;
    }

    private void handleRead(UdpNonblockingSocket socket) throws IOException {
        if (done) {
            return;
        }

        ByteBuffer buf = ByteBuffer.allocate(8192);
        AddressIpv4 from = socket.receive(buf);
        // if (from == null) { return; }
        buf.flip();
        CharsetDecoder dec = Charset.forName("UTF-8").newDecoder();

        CharBuffer cbuf = dec.decode(buf);
        String msg = cbuf.toString();

        System.err.println(String.format("msg from %1s: %2s", from, msg));

        --readMax;
        if (readMax <= 0) {
            done = true;
            return;
        }
    }

    private void handleCallback(UdpNonblockingSocket socket, int operation) {
        if (operation == SelectionKey.OP_READ) {
            try {
                handleRead(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (operation == SelectionKey.OP_WRITE) {
            try {
                handleWrite(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
