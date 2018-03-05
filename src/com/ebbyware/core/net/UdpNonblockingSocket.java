package com.ebbyware.core.net;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import com.ebbyware.core.thread.RegistrationChannel;
import com.ebbyware.core.thread.RunLoopSocketSource;

public class UdpNonblockingSocket {
    private DatagramChannel channel;
    private AddressIpv4 address;
    private int operations;
    private UdpNonblockingSocketCallback callback;
    private boolean filterWriteNotifications = true;
    private UdpNonBlockingSocketStatusSink sink;

    public UdpNonblockingSocket(AddressIpv4 address, int operations,
            UdpNonblockingSocketCallback callback) throws IOException {
        this.address = address;
        this.operations = operations;
        this.callback = callback;
        
        channel = DatagramChannel.open();
        channel.socket().bind(this.address.getSocketAddress());
        channel.configureBlocking(false);
    }

    public UdpNonblockingSocketCallback getCallback() {
        return callback;
    }

    public int getOperations() {
        return operations;
    }
    
    public AddressIpv4 receive(ByteBuffer buf) throws IOException {
        SocketAddress address = channel.receive(buf);
        if (address != null) {
            return new AddressIpv4((InetSocketAddress)address);
        }
        
        return null;
    }

    public void send(ByteBuffer buf, AddressIpv4 address) throws IOException {
        SocketAddress saddr = address.getSocketAddress();
        if (channel.send(buf, saddr) == 0) {
            // couldn't send
            notifySendError();
        }
    }

    private void notifySendError() {
        if (sink != null) {
            sink.notifySendError();
        }
    }

    public void setBroadcast(boolean b) {
        try {
            channel.socket().setBroadcast(b);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
    
    public boolean getBroadcast() {
        boolean broadcast = false;
        
        try {
            broadcast = channel.socket().getBroadcast();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        
        return broadcast;
    }
    
    public boolean getFilterWriteNotifications() {
        return filterWriteNotifications;
    }
    
    public void setFilterWriteNotifications(boolean filter) {
        filterWriteNotifications = filter;
    }
    
    public void queryChannel(RunLoopSocketSource source) {
        source.notifyChannel(new RegistrationChannel() {
            
            @Override
            public SelectionKey register(Selector selector, int operations,
                    Object context) throws ClosedChannelException {
                return channel.register(selector, operations, context);
            }
        });
    }
    
    public void setSink(UdpNonBlockingSocketStatusSink sink) {
        this.sink = sink;
    }

    public AddressIpv4 getBoundAddress() {
        return new AddressIpv4((Inet4Address)channel.socket().getInetAddress());
    }

    public void close() throws IOException {
        sink = null;
        callback = null;
        channel.close();
    }
}
