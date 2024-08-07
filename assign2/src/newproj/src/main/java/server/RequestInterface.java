package server;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface RequestInterface {
    // UPD CLUSTER KEYWORDS
    String JOIN = "JOIN";
    String LEAVE = "LEAVE";
    String MEMBERSHIP_UPDATE = "MEMBERSHIP";

    // TCP OUTER CLIENT KEYWORDS
    String CLIENT_PUT = "put";
    String CLIENT_GET = "get";
    String CLIENT_DELETE = "delete";
    List<String> CLIENTOPS = new ArrayList<>(Arrays.asList(CLIENT_PUT, CLIENT_GET, CLIENT_DELETE));

    // TCP ADMIN KEYWORDS
    String ADMIN_JOIN = "join";
    String ADMIN_LEAVE = "leave";
    String ADMIN_CREATE = "create";
    List<String> ADMINOPS = new ArrayList<>(Arrays.asList(ADMIN_JOIN, ADMIN_LEAVE, ADMIN_CREATE));

    // TCP CLUSTER OPERATIONS KEYWORDS;
    String PUT = "cluster_put";
    String PUT_INFORM = "put_inform";
    String DELETE = "cluster_delete";
    String DELETE_INFORM = "delete_inform";
    String GET = "cluster_get";
    String GET_INFORM = "get_inform";
    String GET_RESPONSE = "ack_get";
    String PUT_RESPONSE = "ack_put";
    List<String> RELAYOPS = new ArrayList<>(
            Arrays.asList(PUT, PUT_INFORM, DELETE, DELETE_INFORM, GET, GET_INFORM, GET_RESPONSE, PUT_RESPONSE));

    // TCP REPLICATION KEYWORDS
    String KEY_REPLICATION_JOIN = "key_rep_join";
    String KEY_REPLICATION_LEAVE = "key_rep_leave";
    String KEY_REPLICATION_DELETE_JOIN = "key_rep_del_join";
    String KEY_REPLICATION_DELETE_LEAVE = "key_rep_del_leave";
    List<String> REPLICATIONOPS = new ArrayList<>(Arrays.asList(KEY_REPLICATION_JOIN, KEY_REPLICATION_DELETE_JOIN,
            KEY_REPLICATION_LEAVE, KEY_REPLICATION_DELETE_LEAVE));

    // TCP KEYWORDS
    String MEMBERSHIP = "Membership";

    void receiveData(Object data);

    void sendData(DatagramPacket packet);

    void broadcastToCluster(String data);
}
