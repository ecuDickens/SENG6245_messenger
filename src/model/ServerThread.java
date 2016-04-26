package model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Represents a connection to an active user.
 * Messages received from this user are parsed and handled by the server which may result in messages being sent back to the user.
 */
public class ServerThread extends Thread {
    private int threadId;
    private String userName;
    private Server server;
    private Socket socket;
    private DataInputStream streamIn;
    private DataOutputStream streamOut;
    private boolean run;

    public ServerThread(final Server server,
                        final Socket socket,
                        final DataInputStream streamIn,
                        final DataOutputStream streamOut) {
        super();
        this.threadId = socket.getPort();
        this.server = server;
        this.socket = socket;
        this.streamIn = streamIn;
        this.streamOut = streamOut;
        this.userName = null;
        this.run = true;
        start();
    }

    public int getThreadId() {
        return threadId;
    }

    public String getUserName() {
        return userName;
    }
    public void setUserName(final String userName) {
        this.userName = userName;
    }

    public void run() {
        while (run) {
            receive();
        }
    }

    public void send(final Message message) {
        try {
            streamOut.writeUTF(message.toString());
            streamOut.flush();
        } catch (IOException e) {
            System.out.println("Error in ServerThread.send: " + e.getMessage());
            server.removeThread(this);
        }
    }

    public void receive() {
        try {
            server.processClientMessage(threadId, Message.toMessage(streamIn.readUTF()));
        } catch (IOException e) {
            System.out.println("Error in ServerThread.receive: " + e.getMessage());
            server.removeThread(this);
        }
    }

    public void close() {
        try {
            if (null != socket) {
                socket.close();
            }
            if (null != streamIn) {
                streamIn.close();
            }
            if (null != streamOut) {
                streamOut.close();
            }
        } catch (IOException e) {
            System.out.println("Error in ServerThread.close: " + e.getMessage());
        }
        run = false;
    }
}
