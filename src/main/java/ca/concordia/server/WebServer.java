package ca.concordia.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WebServer {

    private static final int PORT = 5005;
    private static final int THREAD_POOL_SIZE = 1000;
    private final Lock lock = new ReentrantLock();

    private final ExecutorService executorService;

    public WebServer() {
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    private static Map<Integer, Account> accountMap = new HashMap<>();

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                System.out.println("Waiting for a client to connect...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client...");

                // Use executor service to handle the connection in a separate thread
                executorService.submit(() -> handleConnection(clientSocket));
            }
        }
    }

    private void handleConnection(Socket clientSocket) {
        String threadName = Thread.currentThread().getName();
        System.out.println("Handling connection in thread: " + threadName);
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                OutputStream out = clientSocket.getOutputStream()
        ) {
            String request = in.readLine();
            if (request != null) {
                if (request.startsWith("GET")) {
                    // Handle GET request
                    handleGetRequest(out);
                } else if (request.startsWith("POST")) {
                    // Handle POST request
                    handlePostRequest(in, out);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleGetRequest(OutputStream out) throws IOException {
        // Respond with a basic HTML page
        System.out.println("Handling GET request");
        String response = "HTTP/1.1 200 OK\r\n\r\n" +
                "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<title>Concordia Transfers</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "\n" +
                "<h1>Welcome to Concordia Transfers</h1>\n" +
                "<p>Select the account and amount to transfer</p>\n" +
                "\n" +
                "<form action=\"/submit\" method=\"post\">\n" +
                "        <label for=\"account\">Account:</label>\n" +
                "        <input type=\"text\" id=\"account\" name=\"account\"><br><br>\n" +
                "\n" +
                "        <label for=\"value\">Value:</label>\n" +
                "        <input type=\"text\" id=\"value\" name=\"value\"><br><br>\n" +
                "\n" +
                "        <label for=\"toAccount\">To Account:</label>\n" +
                "        <input type=\"text\" id=\"toAccount\" name=\"toAccount\"><br><br>\n" +
                "\n" +
                "        <input type=\"submit\" value=\"Submit\">\n" +
                "    </form>\n" +
                "</body>\n" +
                "</html>\n";
        out.write(response.getBytes());
        out.flush();
    }

    private void handlePostRequest(BufferedReader in, OutputStream out) throws IOException {
        System.out.println("Handling post request");
        StringBuilder requestBody = new StringBuilder();
        int contentLength = 0;
        String line;

        // Read headers to get content length
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith("Content-Length")) {
                contentLength = Integer.parseInt(line.substring(line.indexOf(' ') + 1));
            }
        }

        // Read the request body based on content length
        for (int i = 0; i < contentLength; i++) {
            requestBody.append((char) in.read());
        }

        System.out.println(requestBody.toString());
        // Parse the request body as URL-encoded parameters
        String[] params = requestBody.toString().split("&");
        String account = null, value = null, toAccount = null, toValue = null;

        for (String param : params) {
            String[] parts = param.split("=");
            if (parts.length == 2) {
                String key = URLDecoder.decode(parts[0], "UTF-8");
                String val = URLDecoder.decode(parts[1], "UTF-8");

                switch (key) {
                    case "account":
                        account = val;
                        break;
                    case "value":
                        value = val;
                        break;
                    case "toAccount":
                        toAccount = val;
                        break;
                }
            }
        }

        String responseContent = "<html><body><h1>Thank you for using Concordia Transfers</h1>";
        // Check if both accounts exist
        if ((account != null) & (toAccount != null) & (value != null)) {
            boolean sourceAccountExists = doesAccountExist(Integer.parseInt(account));
            boolean targetAccountExists = doesAccountExist(Integer.parseInt(toAccount));
            if (!sourceAccountExists) {
                responseContent = responseContent + "<html><body><h2>Source Account does not exist</h2>";
                System.out.println("Account does not exist");
            } else if (!targetAccountExists) {
                responseContent = responseContent + "<html><body><h2>Destination Account does not exist</h2>";
                System.out.println("Destination Account does not exist");
            }
            else {
                Account source = accountMap.get(Integer.parseInt(account));
                Account destination = accountMap.get(Integer.parseInt(toAccount));
                if (Integer.parseInt(value) < 0) {
                    responseContent = responseContent + "<html><body><h2>Cannot transfer negative sum</h2>";
                    System.out.println("Cannot transfer negative sum");
                }
                else if (Integer.parseInt(value) > source.getBalance() ){
                    responseContent = responseContent + "<html><body><h2>No sufficient funds in Source Account</h2>";
                } else {

                    source.transferFunds(destination,Integer.parseInt(value));

                    System.out.println("Updated Accounts balance:");
                    printMap();

                    responseContent = responseContent +
                            "<h2>Received Form Inputs:</h2>"+
                            "<p>Source Account: " + account + "</p>" +
                            "<p>Value: " + value + "</p>" +
                            "<p>Destination Account: " + toAccount + "</p>" +
                            "<p>Source Account New Balance: " + source.getBalance() + "</p>" +
                            "<p>Destination Account New Balance: " + destination.getBalance() + "</p>" +
                            "</body></html>";
                }
            }
        }


        // Create the response
        /*responseContent = "<html><body><h1>Thank you for using Concordia Transfers</h1>" +
                "<h2>Received Form Inputs:</h2>"+
                "<p>Account: " + account + "</p>" +
                "<p>Value: " + value + "</p>" +
                "<p>To Account: " + toAccount + "</p>" +
                "</body></html>";*/

        // Respond with the received form inputs
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: " + responseContent.length() + "\r\n" +
                "Content-Type: text/html\r\n\r\n" +
                responseContent;

        out.write(response.getBytes());
        out.flush();
    }


    public static Map<Integer, Account> readFile(String filePath) {
        Map<Integer, Account> accountMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    // Skip the first line (header)
                    firstLine = false;
                    continue;
                }

                // Split the line into account ID and balance
                String[] parts = line.split(",");
                int accountId = Integer.parseInt(parts[0].trim());
                int balance = Integer.parseInt(parts[1].trim());

                // Create an Account object and put it into the HashMap
                Account account = new Account(balance, accountId);
                accountMap.put(accountId, account);
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace(); // Handle exceptions appropriately
        }

        return accountMap;
    }

    //check if account exists
    public boolean doesAccountExist(int accountId) {
        return accountMap.containsKey(accountId);
    }

    public static void printMap() {
        for (Map.Entry<Integer, Account> entry : accountMap.entrySet()) {
            int accountId = entry.getKey();
            Account account = entry.getValue();
            int balance = account.getBalance();

            System.out.println("Account ID: " + accountId + ", Balance: " + balance);
        }
    }

    public static void main(String[] args) {

        //Setup data structure using file input
        String filePath = "src/main/resources/data.txt";
        accountMap = readFile(filePath);

        printMap();
        System.out.println("Accounts initialized");

        WebServer server = new WebServer();
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }




}