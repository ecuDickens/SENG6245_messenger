package model;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static model.Message.END_CHAT_MESSAGE;

public class ServerThreadPool extends Thread {

    private Map<Integer, ServerThread> serverThreads;
    private ServerSocket socket;
    private boolean run;

    public ServerThreadPool(final ServerSocket socket) {
        this.serverThreads = new HashMap<Integer, ServerThread>();
        this.socket = socket;
        run = true;
        start();
    }

    public void run() {
        while (run) {
            try {
                addThread(socket.accept());
            } catch (IOException e) {
                System.out.println("Error in ServerThreadPool.run: " + e.getMessage());
                close();
            }
        }
    }

    public synchronized void distributeMessages(int id, String input) {
        if (input.equals(END_CHAT_MESSAGE)) {
            serverThreads.get(id).send(END_CHAT_MESSAGE);
            removeThread(serverThreads.get(id));
        } else {
            for (ServerThread serverThread : serverThreads.values()) {
                serverThread.send(input);
            }
        }
    }

    public synchronized void close() {
        for (ServerThread thread : serverThreads.values()) {
            thread.close();
        }
        try {
             if (null != socket) {
                 socket.close();
             }
        } catch (IOException e) {
            System.out.println("Error in ServerThreadPool.close: " + e.getMessage());
        }
        run = false;
    }

    public synchronized void addThread(final Socket socket) {
        try {
            final ServerThread serverThread = new ServerThread(this, socket,
                    new DataInputStream(new BufferedInputStream(socket.getInputStream())),
                    new DataOutputStream(new BufferedOutputStream(socket.getOutputStream())));
            serverThreads.put(serverThread.getSessionId(), serverThread);
        } catch (IOException e) {
            System.out.println("Error in ServerThreadPool.addThread: " + e.getMessage());
        }
    }

    public synchronized void removeThread(final ServerThread thread) {
        thread.close();
        serverThreads.remove(thread.getSessionId());
    }
}
