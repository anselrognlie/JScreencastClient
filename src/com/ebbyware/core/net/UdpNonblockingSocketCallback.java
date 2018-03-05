package com.ebbyware.core.net;

public interface UdpNonblockingSocketCallback {
    public void run(UdpNonblockingSocket socket, int operation);
}
