package model;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientSendThread extends Thread {
    private Client client;
    private BufferedReader console;
    private DataOutputStream streamOut;
    private boolean run;

    public ClientSendThread(final Client client,
                            final BufferedReader console,
                            final DataOutputStream streamOut) {
        super();
        this.client = client;
        this.console = console;
        this.streamOut = streamOut;
        this.run = true;
        start();
    }

    public void run() {
        while (run) {
            try {
                streamOut.writeUTF(console.readLine());
                streamOut.flush();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                client.stop();
            }
        }
    }

    public void close() {
        try {
            if (null != console) {
                console.close();
            }
            if (null != streamOut) {
                streamOut.close();
            }
        } catch (IOException e) {
            System.out.println("Error in ClientSendThread.close: " + e.getMessage());
        }
        run = false;
    }
}