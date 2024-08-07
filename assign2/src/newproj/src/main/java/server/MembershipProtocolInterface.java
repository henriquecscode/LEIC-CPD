package server;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;

public interface MembershipProtocolInterface {
    Inet4Address left = null;
    Inet4Address right = null;
    ExecutorService threadPool = null;

    enum Ops {
        JOIN,
        LEAVE,
        JOIN_FAIL
    }

    void joinNetwork();

    /**
     * When leaving the network must inform of its two neighbours, so that those can
     * propagate the information.
     * For example, if node 2 left, then it must say that it was linked to 1 on its
     * left and 3 on its right.
     * Therefore, node 3 will adopt 1 as its new left and node 1 will adopt 3 as its
     * new right
     */
    void localLeaveNetwork();

    /**
     * Sees if the node that left is a neighbour and, if so what changes to the
     * cluster should be made to assure replication
     * Factor of 3
     * Left might become always responsible to become the "main" node where the keys
     * were "created" and propagate to its
     * left. Idea might be moving in the memory from the left directory to the right
     * directory?
     */
    void nodeLeftNetwork(InetAddress nodeAddress, String nodeId, int counter);

    /**
     * A node has joined the network somewhere (we still need to define how this
     * somewhere is handled, maybe a random?)
     * Might need to replicate some keys with that node and inform the previous
     * neighbour that it is no longer a neighbour
     */
    void nodeJoinedNetwork(InetAddress nodeAddress, int port, String nodeId, int counter);

    /**
     * This node is informed to drop the keys related to another neighbour.
     * It will also need to find a "new" neighbour to share its keys with?
     */
    void nodeDropNeighbour(InetAddress nodeAddress);
}
