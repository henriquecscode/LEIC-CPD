package client;

import client.operations.ClientOperation;
import client.operations.Type;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class TestClient {

    public ClientOperation ClientOperation;
    private static Random random;

    public static void main(String[] args) {
        doMain(args);
    }

    public static void doMain(String[] args) {
        TestClient TestClient = new TestClient();
        TestClient.random = new Random(69420);

        String OperationName = args[1].toLowerCase();

        ClientOperation Operation = null;

        switch (OperationName) {
            case "put":
                String fileString = TestClient.createRandomFile();
                String data = args[2] + "\n" + fileString;
                Operation = new ClientOperation(Type.PUT, args[0], data);
                break;
            case "delete":
                Operation = new ClientOperation(Type.DELETE, args[0], args[2]);
                break;
            case "get":
                Operation = new ClientOperation(Type.GET, args[0], args[2]);
                break;
            case "join":
                Operation = new ClientOperation(Type.JOIN, args[0], null);
                break;
            case "leave":
                Operation = new ClientOperation(Type.LEAVE, args[0], null);
                break;
            case "create":
                Operation = new ClientOperation(Type.CREATE, args[0], null);
                break;
        }

        Operation.run();

    }

    private static String createRandomFile() {
        Integer fileLen = TestClient.random.nextInt(100);
        String file = "";
        byte[] array = new byte[0];
        for (int i = 0; i < fileLen; i++) {
            Integer lineSize = TestClient.random.nextInt(50);
            array = new byte[lineSize]; // length is bounded by 7
            new Random().nextBytes(array);
            file += new String(array, StandardCharsets.UTF_8) + "\n";
        }
        return file;
    }
}