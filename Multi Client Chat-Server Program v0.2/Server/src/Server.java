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
    private static final String serverPassword = "password";

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
        private Socket client;

        public ClientHandler(Socket client) {
            System.out.println("New client connected at " + client.getInetAddress());
            LogHandler("New client connected at " + client.getInetAddress());
            this.client = client;


        }

        @Override
        public void run() {
            Socket client = this.client;
            BufferedReader clientInput = null;
            PrintWriter clientOutput = null;
            String username = "";
            Boolean continueThread = true;

            try {
                clientInput = new BufferedReader(new InputStreamReader(client.getInputStream()));
                LogHandler("Input stream opened for client at " + client.getInetAddress());
                clientOutput = new PrintWriter(client.getOutputStream(), true);
                LogHandler("Output stream opened for client at " + client.getInetAddress());
            } catch(IOException e) {
                LogHandler("Error connecting to client input/output stream. " + e.getMessage());
            }

            try {
                String sPassword = clientInput.readLine();
                username = clientInput.readLine();
                //Code 0 means all is good.
                //Code 1 means the server password is incorrect.
                //Code 2 means username is already taken.
                if(sPassword.equals(serverPassword)) {
                    if(!usernames.contains(username)) {
                        clientOutput.println("0");
                        clientConnections.add(clientOutput);
                        usernames.add(username);
                    } else {
                        clientOutput.println("2");
                        continueThread = false;
                    }
                } else {
                    clientOutput.println("1");
                    continueThread = false;
                }
            } catch(IOException e) {}

            if(continueThread) {
                try {
                    while(continueThread) {
                        String msg = clientInput.readLine();
                        for(PrintWriter clientOut : clientConnections) {
                            clientOut.println(username + ": " + msg);
                        }
                    }
                } catch(IOException e) {
                    continueThread = false;
                    usernames.remove(username);
                    clientConnections.remove(clientOutput);
                    try {
                        clientInput.close();
                        clientOutput.close();
                        client.close();
                    } catch(Exception f) {}
                }
            }
        }
    }
}
