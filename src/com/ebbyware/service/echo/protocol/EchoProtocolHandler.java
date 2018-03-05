package com.ebbyware.service.echo.protocol;

import com.ebbyware.core.net.AddressIpv4;
import com.ebbyware.service.echo.protocol.packet.EchoMessagePacket;

public interface EchoProtocolHandler {
    public void processEchoMessage(EchoMessagePacket echoMessagePacket,
            AddressIpv4 fromAddress);
}
