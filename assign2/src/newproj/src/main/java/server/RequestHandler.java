package server;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.net.*;

public class RequestHandler implements RequestInterface {
    Node node;
    NodeData nodeData;
    InetAddress ipMcastAddr;
    int ipMcastPort;
    int storePort;
    ServerSocket connectionMembershipProtocolSocket;
    MulticastSocket publicMembershipProtocolSocket;
    Socket socket;
    DatagramSocket publicClientClusterSocket;
    Thread clientThread, membershipThread;
    int CLIENT_BUF_SIZE = 2048, MEMBERSHIP_BUF_SIZE = 2048;
    Boolean publicMembershipProtocolSocketListen = false;

    public RequestHandler(Node node, InetAddress ipMcastAddr, int ipMcastPort, int storePort) {
        this.node = node;
        this.ipMcastAddr = ipMcastAddr;
        this.ipMcastPort = ipMcastPort;
        this.storePort = storePort;
        this.openSocket();

        this.node.threadPool.submit(() -> {
            this.receiveMessages();
        });

    }

    public void receiveClientData() {
        Socket realSocket = this.socket;

        System.out.println("Running client socket thread");

        while (true) {
            InputStream input = null;
            try {
                input = realSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            this.node.threadPool.submit(() -> {
                this.processClientData(reader);
            });
        }

    }

    public void processClientData(BufferedReader packet) {// this doesn't look like it's done

        /*
         * From TestClient:
         * 
         * if (type.equals(Type.DELETE) || type.equals(Type.PUT) ||
         * type.equals(Type.GET)) {
         * send = this.getTypeName() + "," + this.argument;
         * } else {
         * send = this.getTypeName();
         * }
         * 
         * byte[] buf = send.getBytes();
         */

        String s = "";
        try {
            s = packet.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Integer opSplit = s.indexOf("\n");
        String op = s.substring(0, opSplit);
        String data = s.substring(opSplit + 1);

        if (CLIENTOPS.contains(op)) {
            // this.node.getProtocol().relayOperation(packet, op, data);
        }

        if (ADMINOPS.contains(op)) {
            // this.node.getProtocol().adminMembershipOperation(packet, op, s);
        }

    }

    public void receiveMembershipDataData() {
        byte[] rbuf;
        DatagramSocket socket = this.publicMembershipProtocolSocket;
        System.out.println("Running membership socket thread");
        while (publicMembershipProtocolSocketListen) {
            rbuf = new byte[MEMBERSHIP_BUF_SIZE];
            DatagramPacket packet = new DatagramPacket(rbuf, rbuf.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                // e.printStackTrace();
            }

            this.node.threadPool.submit(() -> {
                this.processMembershipData(packet);
            });
        }
        System.out.println("Stopped listening to public membership protocol socket");

    }

    public void processMembershipData(DatagramPacket packet) {
        byte[] data = packet.getData();
        if (packet.getAddress() == null) {
            return;
        }

        String message = new String(data, StandardCharsets.UTF_8);

        String[] lines = message.split("\n"); // Membership should be fine, for files not so much
        String command = lines[0];
        if (command.equals(JOIN)) {
            processJoinCommand(packet, message);
        } else if (command.equals(LEAVE)) {
            processLeaveCommand(packet, message);
        } else if (command.equals(MEMBERSHIP_UPDATE)) {
            processMembershipCommand(packet, message);
        }
    }

    public void processLeaveCommand(DatagramPacket packet, String message) {
        InetAddress sender = packet.getAddress();
        String[] lines = message.split("\n");
        String nodeId = lines[1];
        int counter = Integer.parseInt(lines[2]);
        this.node.getProtocol().nodeLeftNetwork(sender, nodeId, counter);
        // Create a new TCP socket and use it to send to the advertised port with the
        // data we got
        // Instead let the nodeJoinedNetwork be the one deciding what to do, is probably
        // better
    }

    public void processJoinCommand(DatagramPacket packet, String message) {

        InetAddress sender = packet.getAddress();
        String[] lines = message.split("\n"); // Membership should be fine, for files not so much
        int port, counter;
        String nodeId = lines[1];
        counter = Integer.parseInt(lines[2]);
        port = Integer.parseInt(lines[3]);

        this.node.getProtocol().nodeJoinedNetwork(sender, port, nodeId, counter);
        // Create a new TCP socket and use it to send to the advertised port with the
        // data we got
        // Instead let the nodeJoinedNetwork be the one deciding what to do, is probably
        // better
    }

    public void processMembershipCommand(DatagramPacket packet, String message) {
        Integer newLineIndex = message.indexOf("\n");
        String body = message.substring(newLineIndex + 1);
        Integer messageDataIndex = body.indexOf("\n");
        String nodeId = body.substring(0, messageDataIndex);
        String messageData = body.substring(messageDataIndex + 1);
        this.node.getProtocol().nodeUpdatedNetwork(nodeId, messageData);
    }

    public void receiveMessages() {
        Socket socket = null;
        while (true) {
            try {
                socket = this.connectionMembershipProtocolSocket.accept();
            } catch (SocketException e) {
                // We stopped listening to messages
                System.out.println("TCP socket closed: Stopped listening to private cluster socket");
                return;
            } catch (IOException e) {
                // e.printStackTrace();
            }

            InputStream input = null;
            try {
                input = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            Message message = new Message(reader);
            if (message.getMessage().equals("")) {
                // Ditch message
            } else {
                // Inform membershipProtocol
                this.node.getProtocol().informMessage(message, socket);
            }

        }
    }

    public void receiveData(Object data) {
        byte[] rbuf = new byte[255];
        DatagramSocket socket = null;
        DatagramPacket packet = new DatagramPacket(rbuf, rbuf.length);
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        try {
            socket.receive(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String received = new String(packet.getData());
        socket.close();
    }

    public void sendData(DatagramPacket packet) {
        try {
            DatagramSocket socket = new DatagramSocket(0);
            socket.send(packet);
        } catch (SocketException e) {
            // Socket creation
            e.printStackTrace();
        } catch (IOException e) {
            // Socket send
            e.printStackTrace();
        }
    }

    public void sendData(DatagramSocket socket, DatagramPacket packet) {
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcastToCluster(String message) {
        byte[] sbuf = message.getBytes();
        DatagramPacket packet = new DatagramPacket(sbuf, sbuf.length, ipMcastAddr, ipMcastPort);
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(0);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        this.sendData(socket, packet);
        socket.close();

    }

    public void replyJoinMessage(InetAddress address, int port, String body) {
        List<String> headers = new ArrayList<>(Arrays.asList(MEMBERSHIP));
        Message message = new Message(headers, body);
        this.sendTCPMessage(address, port, message);
    }

    public Boolean sendTCPMessage(NodeData node, Message message) {
        return this.sendTCPMessage(node.getAddress(), node.getPort(), message);
    }

    public Boolean sendTCPMessage(InetAddress address, int port, Message message) {
        Socket socket = null;
        try {
            socket = new Socket(address, port);
        } catch (IOException e) {
            return false;
        }
        OutputStream output = null;
        try {
            output = socket.getOutputStream();
        } catch (IOException e) {
            return false;
        }
        PrintWriter writer = new PrintWriter(output, true);
        writer.println(message.getMessage());
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public ServerSocket openSocket() {
        ServerSocket s = null;
        try {
            s = new ServerSocket(storePort);
        } catch (IOException e) {
            e.printStackTrace();
            return s;
        }
        this.connectionMembershipProtocolSocket = s;
        this.nodeData = new NodeData(s.getInetAddress(), s.getLocalPort(), node.getNodeId());
        this.node.threadPool.submit(() -> {
            this.receiveMessages();
        });
        return this.connectionMembershipProtocolSocket;
    }

    public void closeSocket() {
        try {
            this.connectionMembershipProtocolSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createPublicMembershipSocket() {
        try {

            this.publicMembershipProtocolSocket = new MulticastSocket(ipMcastPort);
            InetSocketAddress group = new InetSocketAddress(
                    this.ipMcastAddr,
                    this.ipMcastPort);
            NetworkInterface netInf = NetworkInterface.getByIndex(0);
            this.publicMembershipProtocolSocket.joinGroup(group, netInf);
            this.node.threadPool.submit(() -> {
                publicMembershipProtocolSocketListen = true;
                this.receiveMembershipDataData();
            });
        } catch (SocketException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void closePublicMembershipSocket() {
        publicMembershipProtocolSocketListen = false;
        publicMembershipProtocolSocket.close();
    }

}