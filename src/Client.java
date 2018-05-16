import java.io.*;
import java.net.*;
import java.util.Scanner;

import javafx.application.*;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.*;

public class Client extends Application {
    public static void main(String args) {
        launch(args);
    }

    private static VBox mainContainer;
    private static HBox userInputWindow;
    public static Label chatContent;
    public static TextArea userInput;
    public static Button submit;

    @Override
    public void start(Stage primaryStage) {
        mainContainer = new VBox();
        userInputWindow = new HBox();
        chatContent = new Label();
        chatContent.setMinSize(200,500);
        chatContent.setAlignment(Pos.TOP_LEFT);
        userInput = new TextArea();
        userInput.setMaxSize(10000,50);
        submit = new Button("Submit");
        submit.setOnAction(event -> submitMsg());

        userInputWindow.getChildren().addAll(userInput,submit);
        mainContainer.getChildren().addAll(chatContent,userInputWindow);

        Scene mainScene = new Scene(mainContainer);
        primaryStage.setScene(mainScene);
        primaryStage.show();

        new ConnectionHandler().start();
    }

    public static void submitMsg() {
        String msg = userInput.getText();

        new MessageHandler(msg).start();

        userInput.setText("");
    }
}

class ConnectionHandler extends Thread {
    private static Socket ServerConnection;
    private static final int port = 7555;
    private static String hostname = "localhost";
    private static BufferedReader ServerInput;
    public static PrintWriter ServerOutput;
    private static BufferedReader userInput;
    private static StringBuilder msgContent = new StringBuilder();

    public ConnectionHandler() {
        try{
            ServerConnection = new Socket(hostname,port);
            ServerInput = new BufferedReader(new InputStreamReader(ServerConnection.getInputStream()));
            ServerOutput = new PrintWriter(ServerConnection.getOutputStream(),true);
            userInput = new BufferedReader(new InputStreamReader(System.in));
        } catch(IOException e) {System.out.println(e.getMessage());}
    }

    @Override
    public void run() {
        try{
            while(true) {
                msgContent.append(ServerInput.readLine() + "\n");
                Platform.runLater(new Runnable() {
                   @Override
                   public void run() {
                       Client.chatContent.setText(msgContent.toString());
                   }
                });


                Thread.sleep(100);
            }
        } catch(Exception e) {System.out.println(e.getMessage());}
    }
}

class MessageHandler extends Thread {
    PrintWriter ServerOut;
    String message;
    public MessageHandler(String msg) {
        try {
            ServerOut = ConnectionHandler.ServerOutput;
        } catch(Exception e) {}
        message = msg;
    }

    @Override
    public void run() {
        ServerOut.println(message);
    }
}
