package model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import static model.Message.toMessage;
import static model.enums.MessageTypeEnum.GET_USERS;
import static model.enums.MessageTypeEnum.INVITE_ACCEPT;
import static model.enums.MessageTypeEnum.INVITE_DECLINE;

/**
 * Represents a connection to the server that can send and receive messages targeted at the server or at another user.
 */
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
            final String send = message.toString();
            System.out.println("Client sent: " + send);
            out.writeUTF(send);
            out.flush();
        } catch (IOException e) {
            System.out.println("Error in ClientReceiveThread.send: " + e.getMessage());
            client.stop();
        }
    }

    public void receive() {
        try {
            final String message = in.readUTF();
            System.out.println("Client received: " + message);
            processMessage(toMessage(message));
        } catch (IOException e) {
            System.out.println("Error in ClientReceiveThread.receive: " + e.getMessage());
            client.stop();
        }
    }

    private void processMessage(final Message message) {
        if (null == message) {
            client.alert(client.getMainFrame(), "Error", "Bad message received.");
            return;
        }
        switch(message.getType()) {
            case LOGIN_ACK:
                send(new Message()
                        .withType(GET_USERS)
                        .withSourceUser(client.getSourceUserName()));
                break;
            case LOGIN_DENIED:
                client.alert(client.getLoginFrame(), "Error", message.getText());
                break;
            case GET_USERS:
                client.displayMain(new HashSet<>(Arrays.asList(message.getText().split(","))));
                break;
            case INVITE:
                if (client.displayInvite(message.getSourceUser()) == 0) {
                    send(new Message()
                            .withType(INVITE_ACCEPT)
                            .withSourceUser(client.getSourceUserName())
                            .withTargetUser(message.getSourceUser()));
                    client.openChatBox(message.getSourceUser());
                } else {
                    send(new Message()
                            .withType(INVITE_DECLINE)
                            .withSourceUser(client.getSourceUserName())
                            .withTargetUser(message.getSourceUser()));
                }
                break;
            case INVITE_ACCEPT:
                client.openChatBox(message.getSourceUser());
                break;
            case INVITE_DECLINE:
                client.alert(client.getMainFrame(), null, message.getSourceUser() + " declined your chat request.");
                break;
            case SESSION_EXIT:
                client.alert(client.getChatFrame(message.getSourceUser()), null, message.getSourceUser() + " closed this chat.");
                break;
            case TYPING:
                client.getChatFrame(message.getSourceUser()).setTitle(client.getSourceUserName() + " -> " + message.getSourceUser() + " is typing");
                break;
            case NOT_TYPING:
                client.getChatFrame(message.getSourceUser()).setTitle(client.getSourceUserName() + " -> " + message.getSourceUser());
                break;
            case MESSAGE:
                client.addMessage(message.getSourceUser(), message.getText());
                break;
            default:
                client.alert(client.getMainFrame(), "Error", message.toString());
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
