package server;

import java.net.Inet4Address;

public class IPIdentifier implements Identifier<Inet4Address> {
    Inet4Address ip;

    IPIdentifier(Inet4Address ip) {
        this.ip = ip;
    }

    @Override
    public Inet4Address get() {
        return this.ip;
    }
}
