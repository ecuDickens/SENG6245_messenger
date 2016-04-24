package model;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static model.Message.END_CHAT_MESSAGE;

public class Client {

    private Socket socket;
    private ClientReceiveThread receive;
    private ClientSendThread send;

    public Client(final Socket socket,
                  final DataInputStream in,
                  final DataOutputStream out,
                  final BufferedReader console) {
        this.socket = socket;
        this.receive = new ClientReceiveThread(this, in);
        this.send = new ClientSendThread(this, console, out);
    }

    public void handle(final String msg) {
        if (msg.equals(END_CHAT_MESSAGE)) {
            System.out.println("Good bye. Press RETURN to exit ...");
            stop();
        } else {
            System.out.println(msg);
        }
    }

    public void stop() {
        receive.close();
        send.close();
        try {
            if (null != socket) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error in Client.stop: " + e.getMessage());
        }
    }
}
