import client.TestClient;

public class LeaveI {
    public static void main(String[] args) {
        String[] testClientArgs = new String[2];
        testClientArgs[0] = "127.0.0.1:1236";
        testClientArgs[1] = "leave";
        TestClient.doMain(testClientArgs);
    }
}
