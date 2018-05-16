import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import sun.rmi.log.ReliableLog;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
    private static ArrayList<String> usernames = new ArrayList<String>();
    private static ArrayList<PrintWriter> clientConnections = new ArrayList<PrintWriter>();
    private static ServerSocket incomingConnection;
    private static final int CONNECTION_PORT = 7555;
    private static FileWriter LOG_FILE;

    public static void main(String args[]) {
        try {
            LOG_FILE = new FileWriter(new File("LogFile.txt"),true);

            LogHandler("Opening Connection Socket:");
            incomingConnection = new ServerSocket(CONNECTION_PORT);

            while(true) {
                new ClientHandler(incomingConnection.accept()).start();
            }
        } catch(IOException e) {
            LogHandler("Error opening connection to client. " + e.getMessage());
        }
    }

    private static void LogHandler(String message) {
        try {
            LOG_FILE.write(message + "\n");
        } catch(IOException e) {
            System.out.println("Error writing to log file.");
        }
    }

    static class ClientHandler extends Thread {
        private static Socket client;
        private static String username;
        private static BufferedReader clientInput;
        private static PrintWriter clientOutput;

        public ClientHandler(Socket client) {
            LogHandler("New client connected at " + client.getInetAddress());
            this.client = client;

            try {
                clientInput = new BufferedReader(new InputStreamReader(client.getInputStream()));
                LogHandler("Input stream opened for client at " + client.getInetAddress());
                clientOutput = new PrintWriter(client.getOutputStream(), true);
                LogHandler("Output stream opened for client at " + client.getInetAddress());
            } catch(IOException e) {
                LogHandler("Error connecting to client input/output stream. " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                Boolean usernameTaken = false;
                do {
                    clientOutput.println("Enter a username: ");
                    username = clientInput.readLine();

                    if(usernames.contains(username)) {
                        clientOutput.println("Username taken.");
                        usernameTaken = true;
                    } else {
                        usernames.add(username);
                        clientConnections.add(clientOutput);
                        clientOutput.println("Welcome to the server " + username);
                    }
                } while(usernameTaken == true);
            } catch(IOException e) {
                LogHandler("Error contacting client for username." + e.getMessage());
            }

            try {
                while(true) {
                    String msg = clientInput.readLine();
                    for(PrintWriter clientOut : clientConnections) {
                        clientOut.println(username + ": " + msg + "\n");
                    }
                }
            } catch(IOException e) {
                LogHandler("Error communicating with client. " + e.getMessage());
            }
        }
    }
}
