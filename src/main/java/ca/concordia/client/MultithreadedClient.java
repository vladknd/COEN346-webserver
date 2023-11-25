package ca.concordia.client;

import java.io.*;
import java.net.*;

public class MultithreadedClient implements Runnable {

    private static final String POST_DATA = "account=1234&value=1000&toAccount=5678&toValue=500";

    @Override
    public void run() {
        try {
            // Establish a connection to the server
            Socket socket = new Socket("localhost", 5005);

            // Create an output stream to send the request
            OutputStream out = socket.getOutputStream();

            // Create a PrintWriter to write the request
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));

            // Send the POST request
            writer.println("POST /submit HTTP/1.1");
            writer.println("Host: localhost:8080");
            writer.println("Content-Type: application/x-www-form-urlencoded");
            writer.println("Content-Length: " + POST_DATA.length());
            writer.println();
            writer.println(POST_DATA);
            writer.flush();

            // Create an input stream to read the response
            InputStream in = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            // Read and print the response
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // Close the streams and socket
            reader.close();
            writer.close();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int numThreads = 1000;

        for (int i = 0; i < numThreads; i++) {
            Thread thread = new Thread(new MultithreadedClient());
            thread.start();
        }
    }
}