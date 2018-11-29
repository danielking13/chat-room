import java.net.*;
import java.io.*;
import java.util.*;

/*
 * Created by Daniel King
 * on 11/27/2018
 *
 * This is the client class that the user interfaces with.
 * All commands are input through the console and include
 * the following:
 * login <username> <password> -> to connect to the Server
 * send all <message> -> sends a message to all other users
 * send UserID <message> -> sends a message to a specific user
 * who -> receive the list of the users connected
 * logout -> to disconnect from the Server
 * */

public class Client  {

    private ObjectInputStream socketInput;		// to read from the socket
    private ObjectOutputStream socketOutput;    // to write on the socket
    private Socket socket;
    private String server, username, initialCommand;

    Client(String server, String username, String initialCommand) {
        this.server = server;
        this.username = username;
        this.initialCommand = initialCommand;
    }

    public boolean start() {
        final int portNumber = 10064;

        // try to connect to the server
        try {
            socket = new Socket(server, portNumber);
        }
        catch(Exception ec) {
            System.out.println("Error connecting to server:" + ec);
            return false;
        }

        try { //add data streams from socket
            socketInput = new ObjectInputStream(socket.getInputStream());
            socketOutput = new ObjectOutputStream(socket.getOutputStream());
        }
        catch (IOException eIO) {
            System.out.println("Exception creating new Input/output Streams: " + eIO);
            return false;
        }

        try {
            //send username to server
            socketOutput.writeObject(username);

            boolean loginSuccess = false;
            Scanner scan = new Scanner(System.in);
            while(!loginSuccess) {
                String[] words = initialCommand.split("\\s+");
                if (words.length == 3) {
                    username = words[1];
                }
                if(initialCommand.startsWith("login")) {
                    sendMessage((new ChatMessage(ChatMessage.LOGIN, initialCommand)));
                    try {
                        String reply = (String) socketInput.readObject();
                        if(reply.startsWith("login")) {
                            loginSuccess = true;
                        } else {
                            System.out.println(reply);
                        }
                    } catch (ClassNotFoundException ce) {
                        ce.printStackTrace();
                    }
                } else { //new user
                    try {
                        sendMessage((new ChatMessage(ChatMessage.NEWUSER, initialCommand)));
                        String reply = (String) socketInput.readObject();
                        if(reply.startsWith("New")) {
                            loginSuccess = true;
                            System.out.println(reply);
                        } else
                            System.out.println(reply);
                    } catch(ClassNotFoundException ce) {
                        ce.printStackTrace();
                    }
                }
                if(!loginSuccess) {
                    initialCommand = scan.nextLine();
                }
            }
        } catch (IOException eIO) {
            System.out.println("Exception doing login : " + eIO);
            disconnect();
            return false;
        }

        // creates the Thread to listen to the server
        new ListenFromServer().start();

        return true;
    }

    /*
     * To send a message to the server
     */
    private void sendMessage(ChatMessage msg) {
        try {
            socketOutput.writeObject(msg);
        }
        catch(IOException e) {
            System.out.println("Exception writing to server: " + e);
        }
    }

    /*
     * When something goes wrong
     * Close the Input/Output streams and disconnect
     */
    private void disconnect() {
        try {
            if(socketInput != null) socketInput.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        try {
            if(socketOutput != null) socketOutput.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        try{
            if(socket != null) socket.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String serverAddress = "localhost";
        String userName = null;
        String initialInput = null;
        boolean isNotLoggedIn = true;
        Scanner scan = new Scanner(System.in);
        System.out.println("My chatroom client. Version 2.");

        while (isNotLoggedIn) {
            initialInput = scan.nextLine();
            if(initialInput.trim().startsWith("login") || initialInput.trim().startsWith("newuser")) {

                String[] words = initialInput.split("\\s+");
                if(words.length == 3) {
                    isNotLoggedIn = false;
                    userName = words[1];
                } else
                    System.out.println("Incorrect use of function. Please provide the correct parameters");
            } else {
                System.out.println(">Server: Denied. Please login first.");
            }
        }

        Client client = new Client(serverAddress, userName, initialInput);
        if(!client.start())
            return;

        // gather user input and send to server for processing
        while(true) {
            System.out.print("> ");
            String userInput = scan.nextLine();

            if(userInput.equalsIgnoreCase("logout")) {
                client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
                break; //go to disconnect
            } else if(userInput.equalsIgnoreCase("who")) {
                client.sendMessage(new ChatMessage(ChatMessage.WHO, ""));
            } else if(userInput.startsWith("send")) {
                client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, userInput));
            } else {
                System.out.println("Invalid command. Please try another command: ");
            }
        }

        client.disconnect();
    }

    /*
     * Receives messages from the server and outputs to console
     */
    class ListenFromServer extends Thread {

        public void run() {
            while(true) {
                try {
                    String message = (String) socketInput.readObject();
                    System.out.println(message);
                    System.out.print("> ");
                }
                catch(IOException e) {
                    System.out.println("Server has closed the connection: " + e);
                    break;
                }
                catch(ClassNotFoundException e2) {
                    e2.printStackTrace();
                }
            }
        }
    }
}

