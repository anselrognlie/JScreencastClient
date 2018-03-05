package com.ebbyware.service.echo.protocol.packet;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.ebbyware.core.net.AddressIpv4;
import com.ebbyware.service.echo.protocol.EchoProtocol;
import com.ebbyware.service.echo.protocol.EchoProtocolHandler;

public class EchoMessagePacket implements EchoPacket {

    private static int registrationToken;

    private String message;

    public static void register(EchoProtocol protocol) {
        registrationToken = protocol.registerPacket(
                EchoMessagePacket::parsePacket,
                EchoMessagePacket::isMessagePacket);
    }

    public static void unregister(EchoProtocol protocol) {
        protocol.unregisterPacket(registrationToken);
    }

    public EchoMessagePacket(String message) {
        this.message = message;
    }

    @Override
    public void process(EchoProtocolHandler handler, AddressIpv4 fromAddress) {
        handler.processEchoMessage(this, fromAddress);
    }

    public String getMessage() {
        return message;
    }

    private static EchoPacket parsePacket(ByteBuffer data,
            AddressIpv4 fromAddress) {
        // the entire buffer holds our message, so load it
        String message = new String(data.array(), Charset.forName("UTF-8"));
        return new EchoMessagePacket(message);
    }

    private static boolean isMessagePacket(ByteBuffer data) {
        // this is the only packet type we support
        // and we really have no way to check it, so just
        // assume that it is
        
        return true;
    }

    @Override
    public ByteBuffer getData() {
        // convert our message to a UTF-8 encoded string and add to buffer
        byte[] bytes = message.getBytes(Charset.forName("UTF-8"));
        
        return ByteBuffer.allocate(bytes.length).put(bytes).flip();
    }
}
