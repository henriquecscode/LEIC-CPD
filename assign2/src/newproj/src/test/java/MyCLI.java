import client.TestClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class MyCLI {

    public static void main(String[] args) {
        ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(6);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in));

        // Reading data using readLine
        String name = null;
        System.out.println("Starting my CLI");
        while(true) {
            try {
                name = reader.readLine();

                // Printing the read line
                System.out.println(name);
                String[] newArgs = name.split(" ");
                threadPool.submit(() -> {

                    TestClient.doMain(newArgs);
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
