import javafx.application.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.util.ArrayList;

public class Client extends Application{
    public static void main(String args[]) {
        launch(args);
    }

    private static final Double LABEL_WIDTH = 100.0;
    private static final Double FIELD_WIDTH = 200.0;
    private static final Double HEIGHT = 25.0;

    private static Stage pStage;

    private static Scene loginPage;
    private static BorderPane loginMainContainer;
    private static ImageView logo;
    private static VBox loginInfoContainer;
    private static HBox loginHostnameContainer;
    private static Label loginHostnameLabel;
    private static TextField loginHostnameField;
    private static HBox loginServerPasswordContainer;
    private static Label loginServerPasswordLabel;
    private static PasswordField loginServerPasswordField;
    private static HBox loginUsernameContainer;
    private static Label loginUsernameLabel;
    private static TextField loginUsernameField;
    private static Button loginSubmit;
    private static Label loginStatusLabel;

    private static Scene chatPage;
    private static BorderPane chatMainContainer;
    private static VBox chatInfoContainer;

    private static ListView<String> chatInfoList;
    private static ObservableList<String> messages;
    private static ArrayList<String> messageHistory;

    private static HBox chatInputContainer;
    private static TextField chatInputField;
    private static Button chatSubmit;

    @Override
    public void start(Stage primaryStage) {
        pStage = primaryStage;

        loginHostnameLabel = new Label("Server Hostname: ");
        loginHostnameLabel.setPrefSize(LABEL_WIDTH,HEIGHT);
        loginHostnameField = new TextField();
        loginHostnameField.setPrefSize(FIELD_WIDTH,HEIGHT);
        loginHostnameField.setPromptText("Server Hostname");
        loginHostnameContainer = new HBox();
        loginHostnameContainer.setAlignment(Pos.CENTER);
        loginHostnameContainer.getChildren().addAll(loginHostnameLabel,loginHostnameField);

        loginServerPasswordLabel = new Label("Server Password: ");
        loginServerPasswordLabel.setPrefSize(LABEL_WIDTH,HEIGHT);
        loginServerPasswordField = new PasswordField();
        loginServerPasswordField.setPrefSize(FIELD_WIDTH,HEIGHT);
        loginServerPasswordField.setPromptText("Server Password (Optional)");
        loginServerPasswordContainer = new HBox();
        loginServerPasswordContainer.setAlignment(Pos.CENTER);
        loginServerPasswordContainer.getChildren().addAll(loginServerPasswordLabel,loginServerPasswordField);

        loginUsernameLabel = new Label("Username: ");
        loginUsernameLabel.setPrefSize(LABEL_WIDTH,HEIGHT);
        loginUsernameField = new TextField();
        loginUsernameField.setPrefSize(FIELD_WIDTH,HEIGHT);
        loginUsernameField.setPromptText("Username");
        loginUsernameContainer = new HBox();
        loginUsernameContainer.setAlignment(Pos.CENTER);
        loginUsernameContainer.getChildren().addAll(loginUsernameLabel,loginUsernameField);

        loginSubmit = new Button("Login");
        loginSubmit.setOnAction(e -> serverLogin());
        loginSubmit.setPrefSize(FIELD_WIDTH,HEIGHT);

        loginStatusLabel = new Label();

        loginInfoContainer = new VBox();
        loginInfoContainer.setAlignment(Pos.CENTER);
        loginInfoContainer.setSpacing(16);
        loginInfoContainer.getChildren().addAll(loginHostnameContainer,loginServerPasswordContainer,loginUsernameContainer,loginSubmit,loginStatusLabel);

        loginMainContainer = new BorderPane();
        loginMainContainer.setCenter(loginInfoContainer);

        loginPage = new Scene(loginMainContainer,350,500);

        chatInfoList = new ListView<String>();
        messageHistory = new ArrayList<String>();
        messages = FXCollections.observableArrayList(messageHistory);
        chatInfoList.setItems(messages);
        chatInfoList.setFocusTraversable(false);

        chatInputField = new TextField();
        chatInputField.setOnAction(e -> sendMessage());
        chatInputField.setPrefSize(275,25);
        chatSubmit = new Button("Send");
        chatSubmit.setOnAction(e -> sendMessage());
        chatSubmit.setPrefSize(75,25);
        chatInputContainer = new HBox();
        chatInputContainer.getChildren().addAll(chatInputField,chatSubmit);
        chatInfoContainer = new VBox();
        chatInfoContainer.setPadding(new Insets(8));
        chatInfoContainer.getChildren().addAll(chatInfoList,chatInputContainer);
        chatMainContainer = new BorderPane();
        chatMainContainer.setCenter(chatInfoContainer);
        chatPage = new Scene(chatMainContainer,350,500);

        primaryStage.setScene(loginPage);
        primaryStage.setResizable(false);
        primaryStage.setTitle("Communication Server");
        primaryStage.show();
    }

    @Override
    public void stop() {
        try {
            ConnectionHandler.continueThread = false;
            ConnectionHandler.serverInput.close();
            ConnectionHandler.serverOutput.close();
            ConnectionHandler.serverConnection.close();
        } catch(Exception e) {}
    }

    private void serverLogin() {
        String hostname = loginHostnameField.getText();
        String serverPassword = loginServerPasswordField.getText();
        String username = loginUsernameField.getText();

        loginStatusLabel.setText("");

        if(hostname.isEmpty()) {
            loginStatusLabel.setText("Server Hostname is Required");
        } else if(username.isEmpty()) {
            loginStatusLabel.setText("Username is Required");
        } else {
            loginStatusLabel.setText("Connecting to Server...");
            new ConnectionHandler(hostname,serverPassword,username).start();
        }
    }

    private void sendMessage() {
        String msg = chatInputField.getText();
        Platform.runLater(new Runnable() {
           @Override
           public void run() {
               ConnectionHandler.serverOutput.println(msg);
               chatInputField.setText("");
           }
        });
    }

    static class ConnectionHandler extends Thread {
        private static final int PORT = 7555;
        private static String cHostname, cPassword, cUsername;
        private static Socket serverConnection;
        private static BufferedReader serverInput;
        private static PrintWriter serverOutput;
        private static Boolean continueThread = true;

        public ConnectionHandler(String hostname, String serverPassword, String username) {
            cHostname = hostname;
            cPassword = serverPassword;
            cUsername = username;

            try {
                serverConnection = new Socket(cHostname, PORT);
                serverInput = new BufferedReader(new InputStreamReader(serverConnection.getInputStream()));
                serverOutput = new PrintWriter(serverConnection.getOutputStream(),true);
            } catch(IOException e) {
                System.out.println("Error connecting to server: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            serverOutput.println(cPassword);
            serverOutput.println(cUsername);
            String serverCode = "0";
            try {
                serverCode = serverInput.readLine();
            } catch(IOException e) {}
            //Code 0 = everything is good.
            //Code 1 = server password is incorrect.
            //Code 2 = username is already taken.
            if(serverCode.equals("0")) {
                Platform.runLater(new Runnable() {
                   @Override
                   public void run() {
                       pStage.setScene(chatPage);
                   }
                });
            } else if(serverCode.equals("1")) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        loginStatusLabel.setText("Server password is incorrect");
                    }
                });
                continueThread = false;
            } else if(serverCode.equals("2")) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        loginStatusLabel.setText("Username is already taken");
                    }
                });
                continueThread = false;
            }

            if(continueThread) {
                try {
                    while(continueThread) {
                        String msg = serverInput.readLine();
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                messages.add(msg);
                                chatInfoList.scrollTo(messages.size());
                            }
                        });
                    }
                } catch(IOException e) {}
            }
        }
    }
}
