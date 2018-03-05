package com.ebbyware.service.echo;

import java.nio.ByteBuffer;

import com.ebbyware.core.log.Log;
import com.ebbyware.core.net.AddressIpv4;
import com.ebbyware.core.net.UdpChannel;
import com.ebbyware.service.echo.protocol.EchoProtocol;
import com.ebbyware.service.echo.protocol.EchoProtocolHandler;
import com.ebbyware.service.echo.protocol.packet.EchoMessagePacket;

public class EchoClient extends UdpChannel implements EchoProtocolHandler {
    
    private int maxRecv = 5;
    private boolean done = false;
    private boolean awaiting = false;

    @Override
    protected void handlePacketData(ByteBuffer buf, AddressIpv4 address) {
        EchoProtocol.protocol().handlePacketData(buf, address, this);
    }

    @Override
    protected boolean getEnableBroadcast() {
        return true;
    }
    
    @Override
    protected void handleRetriesExceeded() {
        awaiting = false;

        Log.log("timed out.");
    }

    @Override
    public void processEchoMessage(EchoMessagePacket echoMesssagePacket,
            AddressIpv4 fromAddress) {
        if (done) { return; }
        
        completeAction();
        
        awaiting = false;
        
        Log.log(echoMesssagePacket.getMessage());
        
        --maxRecv;
        if (maxRecv == 0) {
            done = true;
        }
    }
    
    public boolean isDone() {
        return done;
    }
    
    public boolean isAwaitingResponse() {
        return awaiting;
    }

    public void sendMessage(String message) {
        if (done) { return; }
        
        awaiting = true;
        
        // broadcast the message
        EchoMessagePacket packet = new EchoMessagePacket(message);
        
        repeatAction(500, 3, () -> {
            broadcastPacketData(packet.getData(), EchoProtocol.PORT);
        });
    }
}
