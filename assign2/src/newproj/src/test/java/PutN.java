import client.TestClient;

public class PutN {
    public static void main(String[] args) {
        String[] testClientArgs = new String[3];
        for(int i = 0; i < 100; i++){
            testClientArgs[0] = "127.0.0.1:1235";
            testClientArgs[1] = "put";
            testClientArgs[2] = "text" + String.valueOf(i);
            TestClient.doMain(testClientArgs);
        }
    }
}
