import java.io.*;
import java.net.*;
import java.util.*;

/*
* Created by Daniel King
* on 11/27/2018
*
* This is the server class that will handle any processing of data
* thrown at it by the client. It will then respond to the client
* with an appropriate message. It grabs users and passwords from a
* text file and adds those to the user hash map. These are used for
* login purposes. It will also add new users to this text file. It
* will continuously accept new clients until the Max client size is
* met.
* */

public class Server {
    // a unique ID for each connection
    private static int uniqueId;
    private ArrayList<ClientThread> clientList;
    private HashMap<String, String> users;
    private final String FILE_PATH = new File("Users.txt").getAbsolutePath();

    public Server() {
        clientList = new ArrayList<>();
        users = addUsersFromFile();
    }

    public void start() {
        final int MAX_CLIENTS = 3;
        final int PORT = 10064;

        /* create socket server and wait for connection requests */
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("My chatroom server. Version 2");
            // accept new clients
            while(true) {
                Socket socket = serverSocket.accept();  	// accept connection

                if(clientList.size() < MAX_CLIENTS) { //if we have not met the max client threshold we add a new client thread
                    ClientThread thread = new ClientThread(socket);
                    clientList.add(thread);
                    thread.start();
                } else {
                    System.out.println("The amount of clients has exceeded max amount of clients. No more can join now.");
                }
            }
        } catch (IOException e) {
            System.out.println(" Exception on new ServerSocket: " + e + "\n");
        }
    }

    private HashMap<String, String> addUsersFromFile() {
        HashMap<String, String> users = new HashMap<>();
        String line;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH));
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");
                if (parts.length == 2) {
                    String key = parts[0];
                    String value = parts[1];
                    users.put(key, value);
                } //else {
//                    System.out.println("ignoring line: " + line); //for debug purposes
               // }
            }
        } catch (FileNotFoundException fnfe) {
            System.out.println("File:" + FILE_PATH + " was not found");
        } catch (IOException ie) {
            System.out.println("Buffered reader failed");
            ie.printStackTrace();
        }
        return users;
    }

    private boolean login(String command) {
        String[] words = command.split("\\s+");

        if(words.length != 3) {
            System.out.println("Denied. Bad input");
            return false;
        }

        if(users.containsKey(words[1])) {
            for (Map.Entry<String, String> entry : users.entrySet()) {
                if(entry.getKey().equals(words[1]) && entry.getValue().equals(words[2]))
                    return true;
            }
        }
        return false;
    }

    private boolean newuser(String command) {
        String[] input = command.split("\\s+");
        if(input.length == 3 && !users.containsKey(input[1])) {
            if ( input[1].length() < 32 && input[2].length() <= 8 && input[2].length() >= 4) {
                users.put(input[1], input[2]);
                printUserToFile(input[1], input[2]);
                return true;
            } else { //doesn't work with the required username and password standards
                return false;
            }
        } else { //bad input or user id is already taken
            return false;
        }
    }

    // This will add the new user to the user file we read from
    private void printUserToFile(String userName, String password) {
        try {
            FileWriter fileWriter = new FileWriter(FILE_PATH, true);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.printf(userName + " " + password + "%n");
            printWriter.close();
        } catch (IOException io) {
            System.out.println("Could not add new user to file");
            io.printStackTrace();
        }
    }

    /*
     *  to broadcast a message to all Clients or a specific client
     */
    private synchronized void broadcast(String message, String username) {
        String[] commands = message.split("\\s+");
        if(commands.length >= 3) {
            if(commands[1].equals("all")) {
                String newMessage = username + ": " + message.substring(9);
                broadcast(newMessage);
            } else { //send it to a specific user
                String newMessage = username + ": " + message.substring(commands[1].length() + 6);
                for (ClientThread client : clientList) {
                    if (client.username.equals(commands[1])) { //checks if we have found the correct user
                        client.writeMessage(newMessage);
                        System.out.println("To: " + client.username + " From " + newMessage);
                    }
                }
            }
        } else {
            // Don't have the right amount of params. Could throw up a message here
        }
    }

    // for events like login or logout
    private synchronized void broadcast(String message) {
        System.out.println(message);

        // we loop in reverse order in case we would have to remove a Client
        // because it has disconnected
        for(int i = clientList.size() -1; i >= 0; --i) {
            ClientThread client = clientList.get(i);
            // try to write to the client if it fails remove it from the list
            if(!client.writeMessage(message)) {
                clientList.remove(i);
                System.out.println("Disconnected Client " + client.username + " removed from list.");
            }
        }
    }

    private synchronized void remove(int id) {
        for(ClientThread client : clientList) {
            if(client.id == id) {
                clientList.remove(client);
                return;
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }

    /**
     *  One instance of this thread will run for each client
     *  */
    class ClientThread extends Thread {
        Socket socket;
        ObjectInputStream socketInput;
        ObjectOutputStream socketOutput;
        // used to easily disconnect client
        int id;
        String username;
        ChatMessage chatMessage;

        ClientThread(Socket socket) {
            id = ++uniqueId;
            this.socket = socket;
            try {
                socketOutput = new ObjectOutputStream(socket.getOutputStream());
                socketInput = new ObjectInputStream(socket.getInputStream());
                //read in username
                username = (String) socketInput.readObject();
            } catch (IOException e) {
                System.out.println("Exception creating new Input/output Streams: " + e);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            boolean loggedIn = true;
            while(loggedIn) {
                try {
                    chatMessage = (ChatMessage) socketInput.readObject();
                }
                catch (IOException e) {
                    System.out.println(username + " Exception reading Streams: " + e);
                    break;
                }
                catch(ClassNotFoundException e2) {
                    break;
                }

                String message = chatMessage.getMessage();

                // Switch on the type of message received
                switch(chatMessage.getType()) {
                    case ChatMessage.LOGIN:
                        if(login(message)) {
                            writeMessage("login confirmed");
                            broadcast(username + " joins.");
                        } else {
                            writeMessage("Denied. Login credentials don't match or don't exist.");
                        }
                        break;
                    case ChatMessage.NEWUSER:
                        if(newuser(message)) {
                            writeMessage("New user created. Login confirmed.");
                            broadcast(username + " joins.");
                        } else
                            writeMessage("Your parameters are bad or the UserId is already being used");
                        break;
                    case ChatMessage.MESSAGE:
                        broadcast(message, username);
                        break;
                    case ChatMessage.LOGOUT:
                        broadcast(username + " left");
                        loggedIn = false;
                        break;
                    case ChatMessage.WHO:
                        for(int i = 0; i < clientList.size(); ++i) {
                            ClientThread client = clientList.get(i);
                            if((i+1) != clientList.size())
                                message += client.username + ", ";
                            else
                                message += client.username;
                        }
                        writeMessage(message);
                        break;
                }
            }
            // remove the client from the list of connected clients
            remove(id);
            close();
        }

        private void close() {
            // try to close the connection
            try {
                if(socketOutput != null) socketOutput.close();
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            try {
                if(socketInput != null) socketInput.close();
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            try {
                if(socket != null) socket.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        /*
         * Write a String to the Client output stream
         */
        private boolean writeMessage(String message) {
            // if Client is still connected send the message to it
            if(!socket.isConnected()) {
                close();
                return false;
            }
            // write the message to the stream
            try {
                socketOutput.writeObject(message);
            }
            // if an error occurs, do not abort just inform the user
            catch(IOException e) {
                System.out.println("Error sending message to " + username);
                System.out.println(e.toString());
            }
            return true;
        }
    }
}
