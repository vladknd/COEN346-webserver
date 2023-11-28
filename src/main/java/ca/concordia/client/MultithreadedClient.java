package ca.concordia.client;

import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MultithreadedClient implements Runnable {
    private final CountDownLatch startSignal;

    private Lock lck = new ReentrantLock();
    private static int n = 0;

    //private static final String POST_DATA = "account=123&value=1&toAccount=321"; //comment if TEST uncommented

    //---------------------------TEST-----------------
     private static final String[] POST_DATA_ARRAY = {
            "account=123&value=1&toAccount=321",
            "account=321&value=1&toAccount=123",
    };

    //allow only 100 threads to request server connection
    //server has limited capacity (trial and error to find optimal)
    private static final Semaphore connectionSemaphore = new Semaphore(50);

    public MultithreadedClient(CountDownLatch startSignal) {
        this.startSignal = startSignal;
    }

    @Override
    public void run() {
        try {
            startSignal.await();

            // Acquire a permit from the semaphore, blocking if necessary
            connectionSemaphore.acquire();

            // Your thread's logic goes here
            System.out.println("Thread started executing...");
            lck.lock();
            try (Socket socket = new Socket("localhost", 5005);
                 OutputStream out = socket.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
                 InputStream in = socket.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                // Send the POST request
                writer.println("POST /submit HTTP/1.1");
                writer.println("Host: localhost:8080");
                writer.println("Content-Type: application/x-www-form-urlencoded");

//                //-----------------Following lines should be commented if we uncomment TEST
//                writer.println("Content-Length: " + POST_DATA.length());
//                writer.println();
//                writer.println(POST_DATA);

                //---------------------TEST-------------------------------------
                writer.println("Content-Length: " + POST_DATA_ARRAY[0].length());
                //Random random = new Random();
                writer.println();
                System.out.println(POST_DATA_ARRAY[n]);
                writer.println(POST_DATA_ARRAY[n]);
                n=1-n;
                System.out.println(n);




                writer.flush();

                // Read and print the response
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }

            } finally {
                // Release the permit in a finally block to ensure it's released even if an exception occurs
                connectionSemaphore.release();
                lck.unlock();
            }

        } catch (IOException | InterruptedException e) {
            // Handle exceptions according to your application's requirements
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int numThreads = 1000;
        CountDownLatch startSignal = new CountDownLatch(1);

        // Create a thread pool with a fixed number of threads
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        for (int i = 0; i < numThreads; i++) {
            executorService.submit(new MultithreadedClient(startSignal));
        }

        // Release the latch to allow all threads to start simultaneously
        startSignal.countDown();

        // Shutdown the thread pool when it's no longer needed
        executorService.shutdown();
    }
}
