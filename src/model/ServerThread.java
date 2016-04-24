package model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ServerThread extends Thread {
    private int sessionId;
    private ServerThreadPool server;
    private Socket socket;
    private DataInputStream streamIn;
    private DataOutputStream streamOut;
    private boolean run;

    public ServerThread(final ServerThreadPool server,
                        final Socket socket,
                        final DataInputStream streamIn,
                        final DataOutputStream streamOut) {
        super();
        this.sessionId = socket.getPort();
        this.server = server;
        this.socket = socket;
        this.streamIn = streamIn;
        this.streamOut = streamOut;
        this.run = true;
        start();
    }

    public int getSessionId() {
        return sessionId;
    }

    public void send(String msg) {
        try {
            streamOut.writeUTF(msg);
            streamOut.flush();
        } catch (IOException ioe) {
            server.removeThread(this);
        }
    }

    public void run() {
        while (run) try {
            server.distributeMessages(sessionId, streamIn.readUTF());
        } catch (IOException e) {
            System.out.println("Error in ServerThread.run: " + e.getMessage());
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
