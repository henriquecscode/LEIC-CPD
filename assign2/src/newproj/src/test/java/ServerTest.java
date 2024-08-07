import static org.junit.jupiter.api.Assertions.assertTrue;

import client.TestClient;
import org.junit.jupiter.api.Test;

import server.Message;
import server.Node;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ServerTest {
    TestClient TestClient;
    String McastAddrName = "230.0.0.0";
    String localAddress = "127.0.0.1";
    InetAddress ipMcastAddr;
    Integer ipMcastPort = 1234;
    Node server;

    public static void main(String[] args) {

    }


    public Integer getStorePort(String nodeId) {
        return Integer.parseInt(nodeId) + ipMcastPort;
    }

    public Node setupNode(String nodeId) {
        try {
            ipMcastAddr = InetAddress.getByName(McastAddrName);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        Integer storePort = getStorePort(nodeId);
        HashMap<String, String> configs = new HashMap<>();
        configs.put("notimeout", "true");
        server = new Node(ipMcastAddr, ipMcastPort, nodeId, storePort, configs);
        return server;

    }
    
    public Socket getSocket(String nodeId){
        Integer port = ipMcastPort + Integer.valueOf(nodeId);
        Socket socket = null;
        try {
            socket = new Socket(localAddress, port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return socket;
    }

    public Message setUpSingleArgument(String nodeId, String argument) {
        byte[] buf = argument.getBytes();
        List<String> headers = new ArrayList<>(Arrays.asList(argument));
        Message message = new Message(headers, "");
        return message;
    }

    public Message setUpCreate(String nodeId) {
        String create = "create";
        Message message = setUpSingleArgument(nodeId, create);
        return message;
    }

    public Message setUpJoin(String nodeId) {
        String join = "join";
        Message message = setUpSingleArgument(nodeId, join);
        return message;
    }

    public void testCreateNetwork() {
        String nodeId = "1";
        setupNode(nodeId);
        Message message = setUpCreate(nodeId);
        server.getProtocol().informMessage(message, getSocket(nodeId));
    }

    public void testJoinNetwork(int id) {
        String nodeId = String.valueOf(id);
        setupNode(nodeId);
        Message message = setUpJoin(nodeId);
        server.getProtocol().informMessage(message, getSocket(nodeId));
    }
}
