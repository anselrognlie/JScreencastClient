package com.ebbyware.service.echo.protocol;

import java.nio.ByteBuffer;
import java.util.Hashtable;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.ebbyware.core.container.Pair;
import com.ebbyware.core.net.AddressIpv4;
import com.ebbyware.service.echo.protocol.packet.EchoMessagePacket;
import com.ebbyware.service.echo.protocol.packet.EchoPacket;

public class EchoProtocol {

    public static short PORT = 13887;

    public interface Recognizer extends Function<ByteBuffer, Boolean> {
    }

    public interface Parser
            extends BiFunction<ByteBuffer, AddressIpv4, EchoPacket> {
    }

    public class RegistryPair extends Pair<Parser, Recognizer> {
        public RegistryPair(Parser parser, Recognizer recognizer) {
            super(parser, recognizer);
        }
    }

    private static EchoProtocol singleton = null;
    private static int registeredProtocols = 0;

    private Hashtable<Integer, RegistryPair> handlers;

    public static EchoProtocol protocol() {
        if (singleton == null) {
            singleton = new EchoProtocol();
            EchoMessagePacket.register(singleton);
        }

        return singleton;
    }

    private EchoProtocol() {
        handlers = new Hashtable<>();
    }

    public int registerPacket(Parser parser, Recognizer recognizer) {
        int token = registeredProtocols++;

        handlers.put(token, new RegistryPair(parser, recognizer));

        return token;
    }

    public void unregisterPacket(int registrationToken) {
        handlers.remove(registrationToken);
    }

    public void handlePacketData(ByteBuffer data, AddressIpv4 fromAddress,
            EchoProtocolHandler handler) {
        // extract the packet
        EchoPacket packet = parsePacketData(data, fromAddress);

        // inform the packet to process using the handler
        packet.process(handler, fromAddress);
    }
    
    private EchoPacket parsePacketData(ByteBuffer data, AddressIpv4 fromAddress) {
        EchoPacket packet = null;

        // for each registered handler, check whether the data matches, then try to parse
        for (RegistryPair pair : handlers.values()) {
            if (pair.second().apply(data)) {
                packet = pair.first().apply(data, fromAddress);
                break;
            }
        }

        return packet;
    }
}
