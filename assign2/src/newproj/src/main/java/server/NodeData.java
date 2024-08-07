package server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class NodeData {

    String SEP = ":";
    InetAddress address;
    int port;
    String id;

    NodeData(InetAddress TCPaddress, int port, String id) {
        if (ipBytesToString(TCPaddress.getAddress()).equals("0.0.0.0")) {
            try {
                this.address = InetAddress.getByAddress(ipStringToBytes("192.168.56.1"));
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        } else {
            this.address = TCPaddress;
        }
        this.port = port;
        this.id = id;
    }

    NodeData(String data) {
        String[] vars = data.split(SEP);
        try {
            this.address = InetAddress.getByAddress(ipStringToBytes(vars[0]));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        this.port = Integer.valueOf(vars[2]);
        this.id = vars[3];
    }

    public String getStringAddress() {
        return ipBytesToString(address.getAddress()) + SEP + address.getHostAddress();
    }

    public InetAddress getAddress() {
        return this.address;
    }

    public Integer getPort() {
        return this.port;
    }

    public String getId() {
        return this.id;
    }

    @Override
    public String toString() {
        return this.getStringAddress() + SEP + port + SEP + id;
    }

    public byte[] ipStringToBytes(String ipString) {
        String[] strings = ipString.split("\\.");
        byte[] ip = new byte[4];
        for (int i = 0; i < 4; i++) {
            ip[i] = Integer.valueOf(strings[i]).byteValue();
        }
        return ip;
    }

    public String ipBytesToString(byte[] data) {
        String fields[] = new String[4];
        for (int i = 0; i < 4; i++) {
            fields[i] = String.valueOf(Byte.toUnsignedInt(data[i]));
        }
        String result = String.join(".", fields);
        return result;
    }

    public boolean equals(NodeData data) {
        Boolean isEqual = this.getStringAddress().equals(data.getStringAddress()) && this.port == data.getPort()
                && this.id.equals(data.getId());
        return isEqual;
    }
}
