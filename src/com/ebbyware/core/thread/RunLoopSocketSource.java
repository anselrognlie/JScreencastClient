package com.ebbyware.core.thread;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import com.ebbyware.core.net.UdpNonBlockingSocketStatusSink;
import com.ebbyware.core.net.UdpNonblockingSocket;

public class RunLoopSocketSource
        implements RunLoopSource, UdpNonBlockingSocketStatusSink {
    
    private UdpNonblockingSocket socket;
    private RunLoop host;
    private SelectionKey key;
    private RegistrationChannel channel;
    private boolean allowWriteNotification = true;

    public RunLoopSocketSource(UdpNonblockingSocket socket) {
        this.socket = socket;
        
        socket.setSink(this);
    }

    public void cancel() {
        if (host == null) {
            throw new UnsupportedOperationException(
                    "attempt to unregister unregistered RunLoopSource");
        }

        key.cancel();

        socket.setSink(null);
        socket = null;
        host = null;
        key = null;
    }

    public void register(RunLoop runloop) {
        if (host != null) {
            throw new UnsupportedOperationException(
                    "attempt to add registered RunLoopSource to different RunLoop");
        }
        host = runloop;

        Selector selector = runloop.getSelector();
        socket.queryChannel(this); // populates channel

        try {
            key = channel.register(selector, socket.getOperations(), this);
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        }
    }

    public void operationOccured(int operation) {
        // if the socket has requested write filtering, only allow one
        // write notification to be sent until a send error occurs

        if (operation == SelectionKey.OP_WRITE) {
            if (socket.getFilterWriteNotifications() && !allowWriteNotification) {
                return;
            }

            allowWriteNotification = false;
        }

        socket.getCallback().run(socket, operation);
    }

    public WaitableRunLoopEvent createEvent(int operation) {
        return new SocketRunLoopEvent(this, operation);
    }

    public RunLoop getHost() {
        return host;
    }

    public void notifyChannel(RegistrationChannel registrationChannel) {
        channel = registrationChannel;
    }

    @Override
    public void notifySendError() {
        // there was a send error, so re-allow write notifications
        allowWriteNotification = true;
    }
}
