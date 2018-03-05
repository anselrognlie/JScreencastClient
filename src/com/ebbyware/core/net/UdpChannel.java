package com.ebbyware.core.net;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Date;

import com.ebbyware.core.log.Log;
import com.ebbyware.core.thread.*;

public class UdpChannel {

    // thread for receiving datagram socket
    // timer for reporting "timeouts"
    // buffer for pending data to send
    // internal send/receive datagram socket

    private RunLoop runloop;
    private UdpNonblockingSocket localSocket;
    private RunLoopSocketSource socketSource;
    private RunLoopTimer timeoutTimer;
    private ArrayList<BufferedPacket> transmitBuffer;
    private short listenerPort;
    private boolean enableBroadcast;
    private Runnable timeoutOperation;
    private boolean canWrite;
    private AddressIpv4 boundAddress;
    private long timerInterval;
    private boolean timerEnabled;
    private int timeoutRepeatCount;

    public UdpChannel() {
        localSocket = null;
        socketSource = null;
        timeoutTimer = null;
        runloop = null;
        canWrite = false;
        timerInterval = 0;
        timerEnabled = false;
        transmitBuffer = new ArrayList<>();
        boundAddress = null;
    }

    public AddressIpv4 getBoundAddress() {
        return boundAddress;
    }

    public void start() throws IOException {
        startOnRunLoop(RunLoop.currentRunLoop());
    }

    public void startOnRunLoop(RunLoop runloop) throws IOException {
        this.runloop = runloop;

        startSocket();
    }

    public void stop() throws IOException {
        stopSocket();
    }

    // private ////////////////////////////////////////////////////////

    private void startSocket() throws IOException {
        Log.log("starting socket...");

        listenerPort = getListenerPort();

        AddressIpv4 addr = new AddressIpv4(listenerPort);
        localSocket = new UdpNonblockingSocket(addr,
                SelectionKey.OP_READ | SelectionKey.OP_WRITE,
                new UdpNonblockingSocketCallback() {

                    @Override
                    public void run(UdpNonblockingSocket socket,
                            int operation) {
                        handleSocketCallback(socket, operation);
                    }
                });

        enableBroadcast = getEnableBroadcast();
        localSocket.setBroadcast(enableBroadcast);

        boundAddress = localSocket.getBoundAddress();

        socketSource = new RunLoopSocketSource(localSocket);
        runloop.addSource(socketSource);

        startTimeoutTimer();

        Log.log("listening...");
    }

    private void startTimeoutTimer() {
        Log.log("starting timer...");

        long tenYear = 1000 * 60 * 60 * 24 * 365 * 10;
        timeoutTimer = RunLoopTimer.createRepeatingTimerWithDelay("timeout timer", tenYear,
                tenYear, () -> {
                    handleTimeoutCallback();
                });

        runloop.addTimer(timeoutTimer);
    }

    private void stopSocket() throws IOException {

        Log.log("shutting down...");

        if (timeoutTimer != null) {
            runloop.removeTimer(timeoutTimer);
            timeoutTimer = null;
        }

        timeoutOperation = null;

        if (socketSource != null) {
            runloop.removeSource(socketSource);
            socketSource = null;
        }

        if (localSocket != null) {
            localSocket.close();
            localSocket = null;
        }

        Log.log("stopped.");
    }

    private void handleSocketCallback(UdpNonblockingSocket socket,
            int operation) {
        if (operation == SelectionKey.OP_READ) {
            Log.log("received packet.");

            // who connected? don't know until we try to read

            // read data
            ByteBuffer buf = ByteBuffer.allocate(8192);
            AddressIpv4 address;
            try {
                address = localSocket.receive(buf);

                // we got a packet, so disable timer until next send
                timerEnabled = false;

                // notify overridden handler
                handlePacketData(buf, address);

            } catch (IOException e) {
                Log.log(e.getMessage());
                e.printStackTrace();
            }

        } else if (operation == SelectionKey.OP_WRITE) {
            canWrite = true;
            sendBufferedData();
        }
    }

    private void sendBufferedData() {

        // if no data, or unable to send, just exit for now
        if (!canWrite || transmitBuffer.size() == 0) {
            return;
        }

        BufferedPacket packet = transmitBuffer.get(0);
        ByteBuffer buf = packet.getData();

        try {
            ByteBuffer sendData = ByteBuffer
                    .allocate(buf.capacity()).put(buf)
                    .flip();
            localSocket.send(sendData, packet.getAddress());
        } catch (IOException e) {
            canWrite = false;
            return;
        } finally {
            buf.rewind();
        }

        transmitBuffer.remove(0);
        rescheduleTimeout();
    }

    private void rescheduleTimeout() {
        // if the interval is non zero, schedule the next firing
        if (timerInterval > 0) {
            timerEnabled = true;
            Date now = new Date();
            timeoutTimer.setNextFireDate(now.getTime() + timerInterval);
        } else {
            timerEnabled = false;
        }
    }

    private void handleTimeoutCallback() {
        // if the interval is 0, then the client isn't interested in timeouts
        if (!timerEnabled) {
            return;
        }

        // are we automatically handling the timeout?
        if (timeoutOperation != null) {
            if (timeoutRepeatCount > 0) {
                --timeoutRepeatCount;
                timeoutOperation.run();
            } else {
                completeAction();
                handleRetriesExceeded();
            }
        } else {
            // we don't have any registered handler, so pass this on to the
            // subclass
            handleTimeout();
        }
    }

    // protected interface ////////////////////////////////////////////

    protected void sendPacketData(ByteBuffer data, AddressIpv4 address) {
        // buffer the data until we're allowed to send
        transmitBuffer.add(new BufferedPacket(data, address));

        // attempt to send buffered data
        sendBufferedData();
    }

    protected void broadcastPacketData(ByteBuffer data, short port) {
        // do nothing if we aren't enabled for broadcast
        if (!enableBroadcast) {
            return;
        }

        try {
            AddressIpv4 address = new AddressIpv4(
                    (Inet4Address) InetAddress.getByAddress(new byte[] {
                            (byte) 255, (byte) 255, (byte) 255, (byte) 255 }),
                    port);

            sendPacketData(data, address);

        } catch (UnknownHostException e) {
            // should never hit this
            e.printStackTrace();
        }
    }

    protected void setTimeout(long interval) {
        timerInterval = interval;

        rescheduleTimeout();
    }

    protected void repeatAction(long timeout, int times, Runnable action) {
        timeoutRepeatCount = times;
        timeoutOperation = action;
        setTimeout(timeout);

        timeoutOperation.run();
    }

    protected void completeAction() {
        timeoutOperation = null;
        timeoutRepeatCount = 0;
        setTimeout(0);
    }

    // used if we got a packet, which halts timeouts, but it turns out to be
    // invalid
    protected void resumeAction() {
        if (timeoutOperation != null) {
            timerEnabled = true;
            timeoutOperation.run();
        }
    }

    // protected overrides ////////////////////////////////////////////

    protected short getListenerPort() {
        // no override required if any port is ok
        return 0;
    }

    protected boolean getEnableBroadcast() {
        // no override required if no broadcast required
        return false;
    }

    protected void handlePacketData(ByteBuffer buf, AddressIpv4 address) {
        throw new UnsupportedOperationException(
                "subclass must override handlePacketData", null);
    }

    protected void handleTimeout() {
        // does not require override if no timeout handling required
    }

    protected void handleRetriesExceeded() {
        // does not require override if no timeout handling required
    }

}
