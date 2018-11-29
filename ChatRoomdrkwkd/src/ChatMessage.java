import java.io.*;

/**
 * Created by Daniel King
 * on 11/27/2018
 *
 * This class defines the different type of messages that will be exchanged between the
 * Clients and the Server.
 * When talking from a Java Client to a Java Server a lot easier to pass Java objects, no
 * need to count bytes or to wait for a line feed at the end of the frame
 *
 * The different types of message sent by the Client
 *   WHO to receive the list of the users connected
 *   MESSAGE an ordinary message
 *   LOGOUT to disconnect from the Server
 *   LOGIN connect to Server
 *   NEWUSER to create a new server and login
 */
public class ChatMessage implements Serializable {
    static final int WHO = 0, MESSAGE = 1, LOGOUT = 2, LOGIN = 3, NEWUSER = 4;
    private int type;
    private String message;

    ChatMessage(int type, String message) {
        this.type = type;
        this.message = message;
    }

    int getType() {
        return type;
    }

    String getMessage() {
        return message;
    }
}

