package com.ebbyware.core.net;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class AddressIpv4 implements Cloneable {
    private Inet4Address addressIpv4;
    private short port;
    
    public AddressIpv4(Inet4Address address, short port) {
        this.addressIpv4 = address;
        this.port = port;
    }
    
    public AddressIpv4(Inet4Address address) {
        this(address, (short)0);
    }
    
    public AddressIpv4(short port) {
        this(null, port);
    }
    
    public AddressIpv4() {
        this(null, (short)0);
    }
    
    public AddressIpv4(InetSocketAddress address) {
        addressIpv4 = (Inet4Address)address.getAddress();
        port = (short)address.getPort();
    }

    public void setAddress(Inet4Address address) {
        this.addressIpv4 = address;
    }
    
    public Inet4Address getAddress() {
        return this.addressIpv4;
    }
    
    public SocketAddress getSocketAddress() {
        return new InetSocketAddress(addressIpv4, port);
    }
    
    public void setPort(short port) {
        this.port = port;
    }
    
    public short getPort() {
        return this.port;
    }
    
    public AddressIpv4 clone() {
        return new AddressIpv4(this.addressIpv4, this.port);
    }
    
    public String toString() {
        return String.format("%1s:%2d", addressIpv4.toString(), port);
    }
}
