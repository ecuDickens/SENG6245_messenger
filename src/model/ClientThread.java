package model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import static model.Message.toMessage;
import static model.enums.MessageTypeEnum.GET_USERS;
import static model.enums.MessageTypeEnum.INVITE_ACCEPT;

public class ClientThread extends Thread {
    private Client client;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean run;

    public ClientThread(final Client client,
                        final DataInputStream in,
                        final DataOutputStream out) {
        super();
        this.client = client;
        this.in = in;
        this.out = out;
        this.run = true;
        start();
    }

    public void run() {
        while (run) {
            receive();
        }
    }

    public void send(final Message message) {
        try {
            out.writeUTF(message.toString());
            out.flush();
        } catch (IOException e) {
            System.out.println("Error in ClientReceiveThread.send: " + e.getMessage());
            client.stop();
        }
    }

    public void receive() {
        try {
            processMessage(toMessage(in.readUTF()));
        } catch (IOException e) {
            System.out.println("Error in ClientReceiveThread.receive: " + e.getMessage());
            client.stop();
        }
    }

    private void processMessage(final Message message) {
        if (null == message) {
            System.out.println("Bad message received.");
            return;
        }
        switch(message.getType()) {
            case LOGIN_ACK:
                send(new Message()
                        .withType(GET_USERS)
                        .withSourceUser(client.getSourceUserName()));
                break;
            case LOGIN_DENIED:
                client.setLoginMessage(message.getText());
            case GET_USERS:
                client.displayUsers(new HashSet<>(Arrays.asList(message.getText().split(","))));
                break;
            case INVITE:
                send(new Message()
                        .withType(INVITE_ACCEPT)
                        .withSourceUser(message.getTargetUser())
                        .withTargetUser(message.getSourceUser()));
                client.openChatBox(message.getSourceUser());
                break;
            case INVITE_ACCEPT:
                client.openChatBox(message.getSourceUser());
                break;
            case INVITE_DECLINE:
                break;
            case SESSION_EXIT:
                client.addMessage(message.getSourceUser(), "Left chat");
                break;
            case TYPING:
                client.addMessage(message.getSourceUser(), "Is typing");
                break;
            case NOT_TYPING:
                client.addMessage(message.getSourceUser(), "Stopped typing");
                break;
            case TEXT_CLEARED:
                client.addMessage(message.getSourceUser(), "Cleared text");
                break;
            case MESSAGE:
                client.addMessage(message.getSourceUser(), message.getText());
                break;
            default:
                client.addMessage(message.getSourceUser(), "Error: " + message.getText());
                break;
        }
    }

    public void close() {
        try {
            if (null != in) {
                in.close();
            }
            if (null != out) {
                out.close();
            }
        } catch (IOException e) {
            System.out.println("Error in ClientReceiveThread.close: " + e.getMessage());
        }
        run = false;
    }
}
