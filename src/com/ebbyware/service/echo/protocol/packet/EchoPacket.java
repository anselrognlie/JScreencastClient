package com.ebbyware.service.echo.protocol.packet;

import java.nio.ByteBuffer;

import com.ebbyware.core.net.AddressIpv4;
import com.ebbyware.service.echo.protocol.EchoProtocolHandler;

public interface EchoPacket {
    void process(EchoProtocolHandler handler, AddressIpv4 fromAddress);
    
    ByteBuffer getData();
}
