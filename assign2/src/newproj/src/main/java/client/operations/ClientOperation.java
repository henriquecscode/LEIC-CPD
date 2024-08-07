package client.operations;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import client.operations.Type;
import server.Message;

public class ClientOperation {
    Type type;
    String argument;
    InetAddress nodeAddress;
    Integer nodePort;

    public ClientOperation(Type type, String nodeAccessPoint, String argument) {
        this.type = type;
        this.argument = argument;

        try {
            this.nodeAddress = InetAddress.getByName(nodeAccessPoint.split(":")[0]);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        this.nodePort = Integer.parseInt(nodeAccessPoint.split(":")[1]);
    }

    public void run() {
        Socket realSocket = null;
        try {
            realSocket = new Socket(this.nodeAddress, this.nodePort);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String send;


        OutputStream output = null;
        try {
            output = realSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        PrintWriter writer = new PrintWriter(output, true);


        Message message = null;
        List<String> headers = new ArrayList<>(Arrays.asList(this.getTypeName()));
        if (type.equals(Type.DELETE) || type.equals(Type.PUT) || type.equals(Type.GET)) {
            message = new Message(headers, this.argument);
        } else {
            message = new Message(headers, "");
        }

        System.out.println("Sending message to server");
        System.out.println(message.getMessage());
        writer.println(message.getMessage());

        if (type.equals(Type.PUT) || type.equals(Type.GET)) {
            BufferedReader inFromServer = null;
            try {
                inFromServer = new BufferedReader(new InputStreamReader(realSocket.getInputStream()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            while (true) {
                Message newMessage = new Message(inFromServer);
                if (newMessage.getMessage().equals("")) {
                    //ditch message
                } else {
                    if (newMessage.getBody().length() == 0) {
                        System.out.println("Received empty message from server. File non-existent");
                    } else {
                        // inform membershipProtocol
                        System.out.println("Received message from server");
                        System.out.println(newMessage.getBody());
                    }
                    break;
                }
            }
        }

        try {
            realSocket.close();
        } catch (
                IOException e) {
            throw new RuntimeException(e);
        }

    }


    public Type getType() {
        return this.type;
    }

    public String getTypeName() {
        switch (this.type) {
            case GET:
                return "get";
            case PUT:
                return "put";
            case DELETE:
                return "delete";
            case JOIN:
                return "join";
            case LEAVE:
                return "leave";
            case CREATE:
                return "create";
            default:
                return "";
        }

    }
}
