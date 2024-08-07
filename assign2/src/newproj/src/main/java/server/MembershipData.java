package server;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class MembershipData {
    List<String> clusterCircle;
    Map<String, NodeData> clusterNodes;
    Map<String, Integer> clusterNodesCounters;
    List<Log> membershipLog;
    String nodeId;
    static Node node;
    Lock logsLock = new ReentrantLock();

    MembershipData() {
        this.initVars();
    }

    MembershipData(String string) {
        this.initVars();
        String[] dataArray = string.split("\n");
        List<String> dataListString = Arrays.asList(dataArray);
        String sizes = dataListString.get(0);
        String[] sizesArray = sizes.split(" ");
        Integer addressesSize = Integer.valueOf(sizesArray[0]);
        Integer logsSize = Integer.valueOf(sizesArray[1]);
        List<NodeData> clusterData = new ArrayList<>();
        if (addressesSize + logsSize + 1 + 1 != dataListString.size()) {
            System.out.println("Loading of membership data might be invalid");
        }
        for (int i = 0; i < addressesSize; i++) {
            String address = dataListString.get(i + 1);
            NodeData node = new NodeData(address);
            this.createNode(node);
        }
        for (int i = 0; i < logsSize; i++) {
            Integer logsIndex = addressesSize + 1;
            String logString = dataListString.get(logsIndex + i);
            Log log = new Log(logString);
            membershipLog.add(log);
            clusterNodesCounters.put(log.getNodeId(), log.getCounter());

        }
    }

    private void initVars() {
        clusterCircle = new ArrayList<>();
        clusterNodes = new Hashtable<>();
        membershipLog = new ArrayList<Log>();
        clusterNodesCounters = new HashMap<>();
    }

    public static String hashAddress(InetAddress address) {
        String toHash = address.getHostAddress();
        String hash = Hash.getHash(toHash);
        return hash;
    }

    public static String hashNodeId(String id) {
        return Hash.getHash(id);
    }

    public void createNode(NodeData data) {
        InetAddress address = data.getAddress();
        String hash = MembershipData.hashNodeId(data.getId());

        if (clusterCircle.size() == 0) {
            clusterCircle.add(hash);
            clusterNodes.put(hash, data);
            return;
        }
        for (int i = 0; i < clusterCircle.size(); i++) {
            String nodeHash = clusterCircle.get(i);
            if (Hash.hbt(nodeHash, hash)) {
                clusterCircle.add(i, hash);
                clusterNodes.put(hash, data);
                return;
            } else if (hash.equals(nodeHash)) {
                // Node already existed
                return;
            }
        }
        clusterCircle.add(hash);
        clusterNodes.put(hash, data);
    }

    public void addNode(NodeData data, int counter) {
        this.createNode(data);
        logJoin(data.getId(), counter);

    }

    private void logJoin(String id, int counter) {
        Log log = new Log(MembershipProtocolInterface.Ops.JOIN, id, counter);
        this.replaceLog(log, counter);
    }

    public NodeData removeNode(NodeData node, int counter) {
        String hash = MembershipData.hashNodeId(node.getId());
        NodeData suc = getSucessor(node);
        NodeData removedNode = clusterNodes.get(hash);
        logLeave(removedNode.getId(), counter);
        clusterNodesCounters.put(removedNode.getId(), counter);
        clusterNodes.remove(hash);
        clusterCircle.remove(hash);
        return suc;
    }

    private void logLeave(String id, int counter) {
        Log log = new Log(MembershipProtocolInterface.Ops.LEAVE, id, counter);
        this.replaceLog(log, counter);
    }

    private void replaceLog(Log log, int counter) {
        // Maybe a log lock. Concurrency is duplicating the log?
        logsLock.lock();
        Optional<Log> currentLog = membershipLog.stream().filter(s -> s.getNodeId().equals(log.getNodeId()))
                .findFirst();
        if (currentLog.isPresent()) {
            membershipLog.remove(currentLog.get());
        }
        membershipLog.add(log);
        clusterNodesCounters.put(log.getNodeId(), counter);
        MembershipData.node.getMemory()
                .storeLog(membershipLog.stream().map(e -> e.toString()).collect(Collectors.joining("\n")));
        logsLock.unlock();
    }

    private List<Log> getLogs() {
        int totalSize = membershipLog.size();
        int sizeToGet = Math.min(32, membershipLog.size());
        return membershipLog.subList(totalSize - sizeToGet, totalSize);
    }

    public List<NodeData> getNodes() {
        return new ArrayList<NodeData>(clusterNodes.values());
    }

    public NodeData getNode(String nodeId) {
        String hash = MembershipData.hashNodeId(nodeId);
        return clusterNodes.get(hash);
    }

    public Integer getCurrentNodes() {
        Integer active = 0;
        for (Integer value : clusterNodesCounters.values()) {
            if (value % 2 == 0) {
                active++;
            }
        }
        return active;
    }

    public NodeData getSucessor(String hash) {
        Boolean foundSuc = false;
        String sucHash = "";
        for (String node : clusterCircle) {
            if (Hash.hbe(node, hash)) {
                sucHash = node;
                foundSuc = true;
                break;
            }
        }

        if (!foundSuc) {
            sucHash = clusterCircle.get(0);
        }
        NodeData sucAddr = clusterNodes.get(sucHash);
        return sucAddr;
    }

    public NodeData getSucessor(NodeData node) {
        String hash = MembershipData.hashNodeId(node.getId());
        return this.getSucessor(hash);
    }

    public NodeData getPrev(String hash) {
        Boolean didFindNode = false;
        String foundNode = "";
        String prevNodeHash = "";
        for (int i = 0; i < clusterCircle.size(); i++) {
            String node = clusterCircle.get(i);
            if (Hash.hbe(node, hash)) {
                didFindNode = true;
                foundNode = node;
                if (i == 0) {
                    prevNodeHash = clusterCircle.get(clusterCircle.size() - 1);
                } else {
                    prevNodeHash = clusterCircle.get(i - 1);
                }
                break;
            }
        }
        if (!didFindNode) {
            prevNodeHash = clusterCircle.get(clusterCircle.size() - 1);
        }

        NodeData prevNode = clusterNodes.get(prevNodeHash);
        return prevNode;
    }

    public NodeData getPrev(NodeData node) {
        String hash = MembershipData.hashNodeId(node.getId());
        return this.getPrev(hash);
    }

    public NodeData getNext(String hash) {
        Boolean didFindNode = false;
        String foundNode = "";
        String nextNodeHash = "";
        for (int i = 0; i < clusterCircle.size(); i++) {
            String node = clusterCircle.get(i);
            if (Hash.hbe(node, hash)) {
                didFindNode = true;
                foundNode = node;
                if (i == clusterCircle.size() - 1) {
                    nextNodeHash = clusterCircle.get(0);
                } else {
                    nextNodeHash = clusterCircle.get(i + 1);
                }
                break;
            }
        }
        if (!didFindNode) {
            nextNodeHash = clusterCircle.get(1);
        }

        NodeData nextNode = clusterNodes.get(nextNodeHash);
        return nextNode;
    }

    public NodeData getNext(NodeData node) {
        String hash = MembershipData.hashNodeId(node.getId());
        return this.getNext(hash);
    }

    public Boolean mergeMembershipData(MembershipData data) {
        List<Log> logs = data.getLogs();
        List<NodeData> listOfNodes = data.getNodes();
        Boolean changedMembership = false;
        for (Log log : logs) {
            Integer index = membershipLog.indexOf(log);
            if (index != -1) {
                continue;
            } else {
                // If the event is older, skip it
                Integer nodeCounter = clusterNodesCounters.get(log.getNodeId());
                if (nodeCounter == null) {
                    // Means that the log is not present at all
                    replaceLog(log, log.getCounter());
                } else {
                    if (nodeCounter >= log.getCounter()) {
                        // Means that we have a more updated version or the same version
                        continue;
                    } else {
                        changedMembership = true;
                        // We need to remove or add to the cluster depending on the counter
                        replaceLog(log, log.getCounter());
                        if (log.getCounter() % 2 == 1) {
                            NodeData nodeToRemove = clusterNodes.get(log.getNodeId());
                            if (nodeToRemove != null) {
                                removeNode(nodeToRemove, log.getCounter());
                            }
                        }
                    }
                }

            }
        }
        for (NodeData node : listOfNodes) {
            createNode(node);
        }
        return changedMembership;
    }

    public String toString() {
        // Method to send the membership data
        String data = "";
        List<Log> logs = new ArrayList<>(this.getLogs());
        data += clusterCircle.size() + " " + logs.size() + '\n';
        List<String> revClusterCircle = new ArrayList<>(clusterCircle);
        Collections.reverse(revClusterCircle);
        for (String hash : revClusterCircle) {
            NodeData nodeData = clusterNodes.get(hash);
            data += nodeData.toString() + '\n';
        }
        logsLock.lock();
        for (int i = 0; i < logs.size(); i++) {
            data += logs.get(i).toString() + '\n';
        }
        logsLock.unlock();

        return data;
    }
}
