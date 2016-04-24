package model;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientReceiveThread extends Thread {
    private Client client;
    private DataInputStream streamIn;
    private boolean run;

    public ClientReceiveThread(final Client client,
                               final DataInputStream streamIn) {
        super();
        this.client = client;
        this.streamIn = streamIn;
        this.run = true;
        start();
    }

    public void run() {
        while (run) {
            try {
                client.handle(streamIn.readUTF());
            } catch (IOException e) {
                System.out.println("Error in ClientReceiveThread.run: " + e.getMessage());
                client.stop();
            }
        }
    }

    public void close() {
        try {
            if (null != streamIn) {
                streamIn.close();
            }
        } catch (IOException e) {
            System.out.println("Error in ClientReceiveThread.close: " + e.getMessage());
        }
        run = false;
    }
}
