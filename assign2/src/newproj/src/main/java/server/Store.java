package server;

import java.net.*;
import java.util.HashMap;

public class Store {
    static InetAddress ipMcastAddr;
    static int ipMcastPort;
    static String nodeId;
    static int storePort;
    static MembershipProtocol distribution;

    public static void main(String[] args) throws UnknownHostException, SocketException {
        Node server;
        HashMap<String, String> configs = new HashMap<>();
        parseArgs(args, configs);
        server = new Node(ipMcastAddr, ipMcastPort, nodeId, storePort, configs);
    }

    public static void parseArgs(String[] args, HashMap<String, String> configs) throws UnknownHostException {
        if (args.length < 4) {
            System.out.println("ERROR");
        }
        String[] strings = args[0].split("\\.");
        byte[] ip = new byte[4];
        for (int i = 0; i < 4; i++) {
            ip[i] = Integer.valueOf(strings[i]).byteValue();
        }
        ipMcastAddr = InetAddress.getByAddress(ip);
        System.out.println(ipMcastAddr);
        ipMcastPort = Integer.parseInt(args[1]);
        System.out.println(ipMcastPort);
        nodeId = args[2];
        System.out.println(nodeId);
        storePort = Integer.parseInt(args[3]);
        System.out.println(storePort);
        if (args.length > 4) {
            for (int i = 4; i < args.length; i++) {
                String arg = args[i];
                if (arg.equals("--noTimeout")) {
                    configs.put("notimeout", "true");
                }
            }
        }
    }

    /**
     * $ java Store java Store <IP_mcast_addr> <IP_mcast_port> <node_id>
     * <Store_prt>
     * ARGUMENTS
     * 0 - IP_mcast_addr - multicast IP address
     * 1 - IP_mcast_port - multicast IP port
     * 2 - node_id - node identifier
     * 3 - Store_prt - Store port
     *
     */
}
