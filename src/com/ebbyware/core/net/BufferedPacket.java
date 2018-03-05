package com.ebbyware.core.net;

import java.nio.ByteBuffer;

public class BufferedPacket {
    private ByteBuffer data;
    private AddressIpv4 address;
    
    public BufferedPacket(ByteBuffer data, AddressIpv4 address) {
        this.data = ByteBuffer.allocate(data.capacity()).put(data).flip();
        this.address = address.clone();
    }
    
    public ByteBuffer getData() {
        return data;
    }
    
    public AddressIpv4 getAddress() {
        return address;
    }
}
