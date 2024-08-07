package server;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Node {

    public MembershipProtocol protocol;
    public RequestHandler requestHandler;
    public Memory memory;
    public ThreadPoolExecutor threadPool;
    public String nodeId;

    public Node(InetAddress ipMcastAddr, int ipMcastPort, String nodeId, int storePort,
            HashMap<String, String> configs) {
        this.nodeId = nodeId;
        this.threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(6);
        memory = new Memory(this);
        protocol = new MembershipProtocol(this, ipMcastAddr, ipMcastPort, nodeId, storePort, configs);
        requestHandler = new RequestHandler(this, ipMcastAddr, ipMcastPort, storePort);
    }

    public MembershipProtocol getProtocol() {
        return this.protocol;
    }

    public RequestHandler getRequestHandler() {
        return this.requestHandler;
    }

    public Memory getMemory() {
        return this.memory;
    }

    public String getNodeId() {
        return this.nodeId;
    }
}
