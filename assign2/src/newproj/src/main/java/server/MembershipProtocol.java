package server;

import java.awt.print.PrinterAbortException;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.Random;

import static java.lang.Math.min;

enum NodeDecision {
    ACT,
    RELAY
}

public class MembershipProtocol implements MembershipProtocolInterface {
    Node node;
    NodeData nodeData;
    MembershipData membershipData;
    List<MembershipData> joiningMembershipData; // For receiving 3 confirmations on join
    InetAddress ipMcastAddr;
    int ipMcastPort, storePort;
    String nodeId;
    int counter = 0; // Even means join, odd means leave.
    int port;
    int joinTries, joinConfirmations = 0;
    int JOIN_TIMEOUT = 3, JOIN_TRIES = 3;
    int MEMBERSHIP_PERIOD = 1; // Broadcast everywhere every 1 second(s) (seems like a lot and k(k-1)/2 total
                               // transmissions)
    int DEFAULT_NEED_JOIN_CONFIRMATIONS = 3;
    int GET_TIMEOUT_TIMER = 3;
    Boolean nodeInCluster;
    Integer neededJoinConfirmations = DEFAULT_NEED_JOIN_CONFIRMATIONS;
    long lastMembershipUpdate;

    Random random;
    String lastAttemptedJoinId = "";
    Map<Integer, Socket> unhandledRequests;
    Map<Integer, Timer> unhandledRequestsTimer;
    Integer requestNum;
    Timer membershipTimer;
    HashMap<String, String> configs;

    /*
     * We need to convert the Inet4Address into a string to then give to the memory
     */

    MembershipProtocol(Node node, InetAddress ipMcastAddr, int ipMcastPort, String nodeId, int storePort,
            HashMap<String, String> configs) {
        MembershipData.node = node;
        this.node = node;
        this.ipMcastAddr = ipMcastAddr;
        this.ipMcastPort = ipMcastPort;
        this.nodeId = nodeId;
        this.storePort = storePort;
        this.configs = configs;
        this.random = new Random();
        this.requestNum = 0;
        initializeCounter();
        resetProtocol();
    }

    private void resetProtocol() {
        nodeInCluster = false;
        membershipData = new MembershipData();
        unhandledRequests = new HashMap<>();
        unhandledRequestsTimer = new HashMap<>();
        membershipTimer = new Timer();
        joiningMembershipData = new ArrayList<>();
    }

    private void initializeCounter() {
        Integer tempCounter = this.node.getMemory().getCounter();
        if (tempCounter % 2 == 1) {
            tempCounter += 1;
            this.node.getMemory().storeCounter(tempCounter);
        }
        this.counter = tempCounter;
    }

    private Integer getRequest() {
        requestNum++;
        return requestNum;
    }

    private void confirmRequest(Integer requestNum) {
        Timer timeoutTimer = this.unhandledRequestsTimer.get(requestNum);
        if (timeoutTimer != null) {
            timeoutTimer.cancel();
            timeoutTimer.purge();
            this.unhandledRequestsTimer.remove(requestNum);
        }
        this.unhandledRequests.remove(requestNum);
    }

    public void incrementCounter() {
        counter++;
        this.node.getMemory().storeCounter(counter);
    }

    public void joinNetwork() {
        this.joinTries = JOIN_TRIES;
        ServerSocket s = this.node.getRequestHandler().connectionMembershipProtocolSocket;
        this.port = s.getLocalPort();
        String message = RequestHandler.JOIN + '\n' + nodeId + "\n" + counter + '\n' + port + '\n';

        this.node.getRequestHandler().broadcastToCluster(message);
        joinTries = JOIN_TRIES;
        this.node.threadPool.submit(() -> this.joinNetworkTries(message));
    }

    public void joinNetworkTries(String message) {
        System.out.println("Trying to join network");
        joinConfirmations = 0;
        while (this.joinTries != 0) {
            try {
                Thread.sleep(this.JOIN_TIMEOUT * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (joinConfirmations >= neededJoinConfirmations) {
                // Success
                System.out.println("Success joining network");
                neededJoinConfirmations = DEFAULT_NEED_JOIN_CONFIRMATIONS;
                this.nodeManagedToJoin();
                nodeInCluster = true;
                return;
            } else {
                System.out.println("Couldn't connect in attempt " + String.valueOf(JOIN_TRIES - joinTries + 1));
                this.node.getRequestHandler().broadcastToCluster(message);
                if (!configs.getOrDefault("notimeout", "false").equals("true")) {
                    joinTries--;
                }
            }
        }
        // Failure
        if (!nodeInCluster) {
            System.out.println("Failed to join existing network");
            this.localCreateNetwork();
            nodeInCluster = true;
        }

    }

    public void nodeManagedToJoin() {
        this.incrementCounter();
        ServerSocket s = this.node.getRequestHandler().connectionMembershipProtocolSocket;
        this.nodeData = new NodeData(s.getInetAddress(), s.getLocalPort(), this.nodeId);
        System.out.println("Node adopted id of " + nodeData.getId() + "key of " + Hash.getHash(this.nodeData.getId()));
        // this.membershipData = new MembershipData();
        membershipDataintegrateData();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                // Makes the membership message and sends it
                String message = "";
                message += RequestInterface.MEMBERSHIP_UPDATE + '\n';
                message += nodeId + '\n';
                message += membershipData.toString();
                node.getRequestHandler().broadcastToCluster(message);
            }
        };
        membershipTimer.scheduleAtFixedRate(task, 0, MEMBERSHIP_PERIOD);
        this.node.getRequestHandler().createPublicMembershipSocket();
    }

    public void membershipDataintegrateData() {
        for (MembershipData data : joiningMembershipData) {
            membershipData.mergeMembershipData(data);
        }
        joiningMembershipData = new ArrayList<>();
    }

    public void confirmJoinNetwork(Object data) {
        joinConfirmations++;
    }

    public void localLeaveNetwork() {
        /*
         * A B C D
         * If C leaves, B is left without its right neighbor and D is left without its
         * left neighbor
         * So B's new right neighbor needs to be D
         */
        // Order of operations needs to be carefully handled

        NodeData prev = membershipData.getPrev(nodeData);
        NodeData suc = membershipData.getNext(nodeData);
        NodeData nextSuc = membershipData.getNext(suc);

        HashSet<NodeData> neighbours = new HashSet<>(Arrays.asList(prev, suc, nextSuc));
        neighbours.remove(this.nodeData);
        this.membershipData.removeNode(this.nodeData, counter);
        for (NodeData neighbour : neighbours) {
            this.nodeReplicateOwnKeysTo(neighbour, false);
        }
        String message = RequestHandler.LEAVE + '\n' + this.nodeData.getId() + '\n' + counter + '\n';
        this.node.getRequestHandler().broadcastToCluster(message);
        this.node.getRequestHandler().closePublicMembershipSocket();
        this.incrementCounter(); // Besides adding the counter we need to inform the neighbors
        resetProtocol();
    }

    public void localNodeLeftReplicateKeys(InetAddress nodeAddress) {
        NodeData suc = membershipData.getSucessor(nodeData);
        NodeData nextSuc = membershipData.getSucessor(suc);
        this.nodeReplicateOwnKeysTo(suc, false);
        this.nodeReplicateOwnKeysTo(nextSuc, false);
    }

    public void nodeLeftNetwork(InetAddress nodeAddress, String nodeId, int counter) {
        lastAttemptedJoinId = "";
        NodeData newKeyHolder = this.membershipData.getPrev(MembershipData.hashNodeId(nodeId));

        NodeData data = membershipData.getNode(nodeId);
        if (data != null) {
            this.nodeLeftNetworkReplicateKeys(data);

        }
        this.membershipData.removeNode(data, counter);
    }

    public void nodeLeftNetworkReplicateKeys(NodeData data) {
        System.out.println("A node has left the network. Ensuring replication");
        NodeData prev = membershipData.getPrev(data);
        NodeData suc = membershipData.getNext(data);
        if (nodeData.equals(prev)) {
            // If we are its predecessor
            // Send to its sucessor
            if (suc.equals(this.nodeData)) {
                return;
            }
            this.nodeReplicateOwnKeysTo(suc, false);
        } else if (nodeData.equals(suc)) {
            // If we are its sucessor
            // Send to its predecessor
            if (suc.equals(this.nodeData)) {
                return;
            }
            this.nodeReplicateOwnKeysTo(prev, false);
        }
    }

    public void nodeJoinedNetwork(InetAddress nodeAddress, int port, String nodeId, int counter) {
        // Need to confirm that we received the network join
        Object dataToSend;
        Random rand = new Random();

        if (lastAttemptedJoinId.equals(nodeId)) {
            System.out.println("Same node trying to connect. Won't reply");
            return;
        }

        if (!this.membershipIsUpdated()) {
            return;
        }

        int random = rand.nextInt(1000);
        try {
            Thread.sleep(random);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        lastAttemptedJoinId = nodeId;
        NodeData data = new NodeData(nodeAddress, port, nodeId);
        this.membershipData.addNode(data, counter);
        String message = this.membershipData.toString();
        System.out.println("Sending reply to join");
        this.node.getRequestHandler().replyJoinMessage(nodeAddress, port, message);
        this.joinNodeReplicateKeys(data);
    }

    public void joinNodeReplicateKeys(NodeData data) {
        NodeData prev = membershipData.getPrev(data);
        NodeData suc = membershipData.getNext(data);
        if (nodeData.equals(prev) || nodeData.equals(suc)) {
            System.out.println("Replicating keys to new node");
            try {
                Thread.sleep(this.JOIN_TIMEOUT * 1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (nodeData.equals(prev)) {
                System.out.println("Replicating nodes to previous nodes");
                this.nodeReplicateOwnKeysTo(data, true);
                this.nodeReplicateNodesKeys(data, data, true);
            } else if (nodeData.equals(suc)) {
                System.out.println("Replicating nodes to next node");
                this.nodeReplicateOwnKeysTo(data, true);
            }
        } else {
            System.out.println("New node does not need key replication");
        }
    }

    public void nodeReplicateNodesKeys(NodeData node, NodeData destiny, Boolean isJoin) {
        NodeData keyRangeLimit = membershipData.getPrev(node);
        String hash1 = MembershipData.hashNodeId(keyRangeLimit.getId());
        String hash2 = MembershipData.hashNodeId(node.getId());
        List<List<String>> keysToReplicate = this.node.getMemory().getKeysRange(hash1, hash2);
        this.replicateKeys(destiny, keysToReplicate.get(0), keysToReplicate.get(1), isJoin);

    }

    public void nodeReplicateOwnKeysTo(NodeData node, Boolean isJoin) {
        this.nodeReplicateNodesKeys(nodeData, node, isJoin);
    }

    public void replicateKeys(NodeData node, List<String> keys, List<String> tombstoneKeys, Boolean isJoin) {
        keys.removeAll(tombstoneKeys);
        // No threads because of concurrency in TCP socket
        for (String key : keys) {
            byte[] data = this.node.getMemory().getFromHashed(key);
            String file = new String(data, StandardCharsets.UTF_8);
            String op = isJoin ? RequestHandler.KEY_REPLICATION_JOIN : RequestHandler.KEY_REPLICATION_LEAVE;
            List<String> headers = new ArrayList<>(Arrays.asList(op, nodeId, key));
            Message message = new Message(headers, file);
            System.out.println("Replicating with " + op + " to node " + node.getId() + " the key " + key);
            this.node.getRequestHandler().sendTCPMessage(node, message);
        }
        for (String key : tombstoneKeys) {
            if (this.node.getMemory().hasTombstoneFileFromHashName(key)) {
                String op = isJoin ? RequestHandler.KEY_REPLICATION_DELETE_JOIN
                        : RequestHandler.KEY_REPLICATION_DELETE_LEAVE;
                List<String> headers = new ArrayList<>(Arrays.asList(op, nodeId, key));
                Message message = new Message(headers, "");
                this.node.getRequestHandler().sendTCPMessage(node, message);
            }
        }
    }

    public void localCreateNetwork() {
        System.out.println("Creating cluster");
        ServerSocket s = this.node.getRequestHandler().connectionMembershipProtocolSocket;
        this.nodeData = new NodeData(s.getInetAddress(), s.getLocalPort(), this.nodeId.toString());
        System.out.println(
                "Node adopted id of " + nodeData.getId() + " and key of " + Hash.getHash(this.nodeData.getId()));
        membershipData.addNode(this.nodeData, this.counter);
        this.incrementCounter();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                // Makes the membership message and sends it
                String message = "";
                message += RequestInterface.MEMBERSHIP_UPDATE + '\n' + nodeData.getId() + '\n';
                message += membershipData.toString();
                node.getRequestHandler().broadcastToCluster(message);
            }
        };
        membershipTimer.scheduleAtFixedRate(task, 0, MEMBERSHIP_PERIOD);
        this.node.getRequestHandler().createPublicMembershipSocket();
        nodeInCluster = true;
    }

    public void nodeUpdatedNetwork(String nodeId, String message) {
        if (nodeId.equals(nodeData.getId())) {
            return;
        }
        MembershipData data = new MembershipData(message);
        Boolean changes = membershipData.mergeMembershipData(data);
        lastMembershipUpdate = new Date().getTime();
        if (changes) {
            lastAttemptedJoinId = "";
        }
    }

    public void informMessage(Message message, Socket socket) {
        this.node.threadPool.submit(() -> {
            System.out.println("Successfully received TCP message:");
            this.processMessage(message, socket);
        });
    }

    public void informNeighbours(Message message) {
        HashSet<NodeData> neighbours = new HashSet<>(Arrays.asList(this.getNodesPrev(), this.getNodesNext()));
        neighbours.remove(this.nodeData);
        for (NodeData neighbour : neighbours) {
            this.node.getRequestHandler().sendTCPMessage(neighbour, message); // Inform next
        }

    }

    public void processMessage(Message message, Socket socket) {
        List<String> headers = message.getHeaders();
        String op = headers.get(0);
        String body = message.getBody();
        System.out.println("Op is:" + op);
        if (op.equals(RequestInterface.MEMBERSHIP)) {
            // First time joining the network
            if (!nodeInCluster) {
                confirmJoinNetwork(null);
                System.out.println("Confirming join network: " + joinConfirmations);
                MembershipData joinMembershipData = new MembershipData(body);
                joiningMembershipData.add(joinMembershipData);
                Integer noClusterNodes = joinMembershipData.getCurrentNodes();
                if (noClusterNodes - 1 < neededJoinConfirmations) {
                    neededJoinConfirmations = min(noClusterNodes - 1, neededJoinConfirmations);
                    System.out.println(
                            "Chainging needed joinConfirmations to " + String.valueOf(neededJoinConfirmations));
                }
                lastMembershipUpdate = new Date().getTime();
            }

        } else if (RequestHandler.RELAYOPS.contains(op)) {
            NodeData incomingNodeData = new NodeData(headers.get(1));
            // We received a TCP from another NODE telling us to do something

            NodeDecision whatToDo = clientOperationDecision(incomingNodeData);
            Boolean act = !(op.equals(RequestHandler.PUT_INFORM) || op.equals(RequestHandler.DELETE_INFORM)
                    || op.equals(RequestHandler.GET_INFORM));

            Memory mem = this.node.getMemory();
            if (op.equals(RequestHandler.PUT) || op.equals(RequestHandler.PUT_INFORM)) {

                Integer nodeDataSplit = body.indexOf("\n");
                String senderNodeString = body.substring(0, nodeDataSplit);
                NodeData senderNode = new NodeData(senderNodeString);
                String data = body.substring(nodeDataSplit + 1);
                Integer keySplit = data.indexOf("\n");
                String key = data.substring(0, keySplit);

                if (whatToDo == NodeDecision.RELAY && act) {
                    // Take care of message relaying (and decrease ttl?) or no decrease ttl?
                    String hash = Hash.getHash(key);
                    relayOpToRightDestiny(message, hash);
                    return;
                }

                String file = data.substring(keySplit + 1);
                byte[] fileBytes = file.getBytes(StandardCharsets.UTF_8);

                String reqNum = headers.get(2);
                String hash = mem.put(key, fileBytes);

                if (act) {
                    List<String> newMessageHeaders = new ArrayList<>(
                            Arrays.asList(RequestHandler.PUT_RESPONSE, this.nodeData.toString(), reqNum));
                    Message newMessage = new Message(newMessageHeaders, hash);
                    if (this.nodeData.equals(senderNode)) {
                        this.node.threadPool.submit(() -> {
                            this.processMessage(newMessage, null);
                        });
                    } else {
                        this.node.getRequestHandler().sendTCPMessage(senderNode, newMessage);
                    }

                    message.setHeader(0, RequestHandler.PUT_INFORM);
                    this.informNeighbours(message);// Inform prev

                }
            } else if (op.equals(RequestHandler.GET)) {
                if (act) {
                    String[] bodyParts = body.split("\n");
                    NodeData senderNode = new NodeData(bodyParts[0]);
                    String key = bodyParts[1];
                    if (whatToDo == NodeDecision.RELAY && act) {
                        // Take care of message relaying (and decrease ttl?) or no decrease ttl?
                        relayOpToRightDestiny(message, key);
                        return;
                    }

                    String reqNum = headers.get(2);
                    byte[] file = mem.get(key);
                    String fileString;
                    if (file == null) {
                        fileString = "";
                    } else {
                        fileString = new String(file, StandardCharsets.UTF_8);
                    }
                    // Here, need to proceed and return the file
                    List<String> newMessageHeaders = new ArrayList<>(
                            Arrays.asList(RequestHandler.GET_RESPONSE, this.nodeData.toString(), reqNum));
                    Message newMessage = new Message(newMessageHeaders, fileString);
                    // allow to send a byte message
                    if (this.nodeData.equals(senderNode)) {
                        this.node.threadPool.submit(() -> {
                            this.processMessage(newMessage, null);
                        });
                    } else {
                        this.node.getRequestHandler().sendTCPMessage(senderNode, newMessage);
                    }
                }
            } else if (op.equals(RequestHandler.DELETE) || op.equals(RequestHandler.DELETE_INFORM)) {
                String[] bodyParts = body.split("\n");
                String hash = bodyParts[1];

                if (whatToDo == NodeDecision.RELAY && act) {
                    // Take care of message relaying (and decrease ttl?) or no decrease ttl?
                    relayOpToRightDestiny(message, hash);
                    return;
                }

                mem.del(hash);
                if (act) {
                    message.setHeader(0, RequestHandler.DELETE_INFORM);
                    this.informNeighbours(message);// Inform prev
                }
            } else if (op.equals(RequestHandler.GET_RESPONSE) || op.equals((RequestHandler.PUT_RESPONSE))) {
                // Need to send the response back to the client
                Integer request = Integer.valueOf(headers.get(2));
                Socket originalSocket = this.unhandledRequests.get(request);
                if (originalSocket == null) {
                    return;
                }
                Message responseMessage = new Message(new ArrayList<>(), body);
                BufferedWriter outToClient = null;
                try {
                    System.out.println("Sending reply to client");
                    outToClient = new BufferedWriter(new OutputStreamWriter(originalSocket.getOutputStream()));
                    System.out.println("Sending reply to client");
                    outToClient.write(responseMessage.getMessage());
                    outToClient.newLine();
                    outToClient.flush();
                    confirmRequest(request);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (RequestHandler.REPLICATIONOPS.contains(op)) {
            String replicationNodeId = headers.get(1);
            String key = headers.get(2);
            System.out.println("Received duplication from node " + replicationNodeId + " with key: " + key);
            if (op.equals(RequestHandler.KEY_REPLICATION_JOIN) || op.equals(RequestHandler.KEY_REPLICATION_LEAVE)) {
                String fileString = body;
                byte[] fileContent = fileString.getBytes();
                this.node.getMemory().putReplication(key, fileContent);
            } else if (op.equals(RequestHandler.KEY_REPLICATION_DELETE_JOIN)
                    || op.equals(RequestHandler.KEY_REPLICATION_DELETE_LEAVE)) {
                this.node.getMemory().del(key);
            }
        } else if (RequestHandler.CLIENTOPS.contains(op)) {
            this.node.getProtocol().relayOperation(socket, op, body);

        } else if (RequestHandler.ADMINOPS.contains(op)) {
            this.node.getProtocol().adminMembershipOperation(op);
        }
    }

    private void relayOpToRightDestiny(Message message, String key) {
        NodeData trueNode = membershipData.getSucessor(key);
        // Send the message to the right node
        List<String> newHeaders = message.getHeaders();
        newHeaders.set(1, trueNode.toString());
        Message newMessage = new Message(newHeaders, message.getBody());
        this.node.getRequestHandler().sendTCPMessage(trueNode.getAddress(), trueNode.getPort(), newMessage);
    }

    private NodeData getNodesPrev() {
        return membershipData.getPrev(this.nodeData);
    }

    private NodeData getNodesNext() {
        return membershipData.getNext(this.nodeData);
    }

    private NodeDecision clientOperationDecision(NodeData incomingNodeData) {
        if (incomingNodeData.equals(this.nodeData)) {
            // The message was sent directly to us
            return NodeDecision.ACT;
        } else {
            return NodeDecision.RELAY;
        }
    }

    public void nodeDropNeighbour(InetAddress nodeAddress) {

    }

    public void relayOperation(Socket socket, String OP, String data) {

        // 1 - Check if we have that key
        // 2 - First, using the membershipData (if we are, just the successor or a
        // previous),
        // (We might need to go to memory to make sure, whether or not it is there).
        // If not then proceed to the relay
        String hash = "";
        List<String> headers = null;
        String messageString = "";
        messageString += this.node.getRequestHandler().nodeData.toString();
        messageString += "\n" + data;
        NodeData node = null;
        // Is an OP that the client sends
        if (OP.equals(RequestHandler.CLIENT_DELETE)) {
            // Will need to hash first
            hash = data;
            node = membershipData.getSucessor(hash);
            headers = new ArrayList<>(Arrays.asList(RequestHandler.DELETE, node.toString()));
        } else if (OP.equals(RequestHandler.CLIENT_GET)) {
            // Is already hashed (for some reason this is how it works)
            hash = data;
            node = membershipData.getSucessor(hash);
            int requestNum = getRequest();
            String requestNumStr = Integer.toString(requestNum);
            headers = new ArrayList<>(Arrays.asList(RequestHandler.GET, node.toString(), requestNumStr));
            unhandledRequests.put(requestNum, socket);
            Timer timeoutTimer = new Timer();
            this.unhandledRequestsTimer.put(requestNum, timeoutTimer);
            Message newMessage = new Message(headers, messageString);
            Boolean reachedServer = replyMessage(newMessage, node);
            NodeData prev = membershipData.getPrev(node);
            NodeData next = membershipData.getNext(node);
            HashSet<NodeData> neighbours = new HashSet<>(Arrays.asList(prev, next));
            RequestHandler handler = this.node.getRequestHandler();
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    for (NodeData destinyNeighbour : neighbours) {
                        newMessage.setHeader(1, destinyNeighbour.toString());
                        replyMessage(newMessage, destinyNeighbour);
                    }
                }
            };
            if (reachedServer) {
                timeoutTimer.schedule(timerTask, GET_TIMEOUT_TIMER);
            } else {
                timerTask.run();
            }
            return;
        } else if (OP.equals(RequestHandler.CLIENT_PUT)) {

            Integer keySplit = data.indexOf("\n");
            String key = data.substring(0, keySplit);
            String file = data.substring(keySplit + 1);
            hash = Hash.getHash(key);
            node = membershipData.getSucessor(hash);
            int requestNum = getRequest();
            unhandledRequests.put(requestNum, socket);
            String requestNumStr = Integer.toString(requestNum);
            headers = new ArrayList<>(Arrays.asList(RequestHandler.PUT, node.toString(), requestNumStr));
        }
        Message newMessage = new Message(headers, messageString);
        replyMessage(newMessage, node);
    }

    private Boolean replyMessage(Message message, NodeData node) {
        // See if we need to ask the requestHandler to send the message or,
        // if we are the right node that the message is intended for
        // call the process message locally and in a thread
        if (node.equals(this.nodeData)) {
            this.node.threadPool.submit(() -> {
                this.processMessage(message, null);
            });
            return true;
        } else {
            return this.node.getRequestHandler().sendTCPMessage(node.getAddress(), node.getPort(), message);

        }
    }

    public void adminMembershipOperation(String op) {

        if (op.equals(RequestHandler.ADMIN_JOIN)) {
            this.joinNetwork();
        } else if (op.equals(RequestHandler.ADMIN_LEAVE)) {
            this.localLeaveNetwork();
        } else if (op.equals(RequestHandler.ADMIN_CREATE)) {
            this.localCreateNetwork();
        }
    }

    private Boolean membershipIsUpdated() {
        // Maybe count how much time since previous membership update?
        if (membershipData.getCurrentNodes() <= 1) {
            return true;
        }
        long now = new Date().getTime();
        long passedTime = now - lastMembershipUpdate;
        if (passedTime > MEMBERSHIP_PERIOD * 1.5) {
            System.out.println("I'm not updated :(");
            return false;
        }
        System.out.println("I'm updated! :)");
        return true;

    }
}
