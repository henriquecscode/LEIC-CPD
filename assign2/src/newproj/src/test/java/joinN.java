import java.util.concurrent.TimeUnit;

public class joinN {
    public static void main(String[] args) {
        for (int i = 0;i < 5; i++) {
            ServerTest server = new ServerTest();
            server.testJoinNetwork(4+i);
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}