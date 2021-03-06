package model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Set;

import static model.Message.toMessage;
import static model.enums.MessageTypeEnum.ACCESS_DENIED;
import static model.enums.MessageTypeEnum.BAD_REQUEST;
import static model.enums.MessageTypeEnum.GET_USERS;
import static model.enums.MessageTypeEnum.INVITE;
import static model.enums.MessageTypeEnum.INVITE_ACCEPT;
import static model.enums.MessageTypeEnum.INVITE_DECLINE;
import static model.enums.MessageTypeEnum.LOGIN;
import static model.enums.MessageTypeEnum.LOGIN_ACK;
import static model.enums.MessageTypeEnum.LOGIN_DENIED;
import static model.enums.MessageTypeEnum.SESSION_EXIT;

/**
 * Represents a connection to an active user that can send and receive messages to that user and to other active users
 * via the server.
 */
public class ServerThread extends Thread {
    private int threadId;
    private String userName;
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean run;

    private static String SERVER = "server";

    public ServerThread(final Server server,
                        final Socket socket,
                        final DataInputStream in,
                        final DataOutputStream out) {
        super();
        this.threadId = socket.getPort();
        this.server = server;
        this.socket = socket;
        this.in = in;
        this.out = out;
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

    public void run() {
        while (run) {
            receive();
        }
    }
    private void receive() {
        try {
            final String message = in.readUTF();
            System.out.println("Server received: " + message);
            processMessage(toMessage(message));
        } catch (IOException e) {
            System.out.println("Error in ServerThread.receive: " + e.getMessage());
            close();
        }
    }

    // Synchronize on this in case multiple other server threads are trying to forward messages.
    public synchronized void send(final Message message) {
        try {
            final String send = message.toString();
            System.out.println("Server sent: " + send);
            out.writeUTF(send);
            out.flush();
        } catch (IOException e) {
            System.out.println("Error in ServerThread.send: " + e.getMessage());
            close();
        }
    }

    // Depending on the message type and information, will send a message back to the current user or forward it to the target user.
    private void processMessage(final Message message) {
        if (null == message) {
            send(new Message()
                    .withType(BAD_REQUEST)
                    .withSourceUser(SERVER)
                    .withText("Invalid Message."));
            return;
        }
        if (LOGIN != message.getType() && null == userName) {
            send(new Message()
                    .withType(ACCESS_DENIED)
                    .withSourceUser(SERVER)
                    .withText("Must log in first."));
            return;
        }
        final String targetUserName = message.getTargetUser();
        final ServerThread target = server.getUserByUserName(targetUserName);
        switch(message.getType()) {
            case LOGIN:
                if (server.getActiveUsers().contains(message.getSourceUser())) {
                    send(new Message()
                            .withType(LOGIN_DENIED)
                            .withSourceUser(SERVER)
                            .withTargetUser(message.getSourceUser())
                            .withText("Username already exists."));
                } else {
                    server.getActiveUsers().add(message.getSourceUser());
                    this.userName = message.getSourceUser();
                    send(new Message()
                            .withType(LOGIN_ACK)
                            .withSourceUser(SERVER)
                            .withTargetUser(message.getSourceUser()));
                }
                break;
            case GET_USERS:
                send(new Message()
                        .withType(GET_USERS)
                        .withSourceUser(SERVER)
                        .withText(setToString(server.getActiveUsers())));
                break;
            case INVITE:
                if (null != target) {
                    target.send(new Message()
                            .withType(INVITE)
                            .withSourceUser(userName));
                } else {
                    send(new Message()
                            .withType(INVITE_DECLINE)
                            .withSourceUser(targetUserName)
                            .withText("User is no longer online."));
                }
                break;
            case INVITE_ACCEPT:
                if (null != target) {
                    target.send(new Message()
                            .withType(INVITE_ACCEPT)
                            .withSourceUser(userName));
                    server.activateSession(userName, targetUserName);
                } else {
                    send(new Message()
                            .withType(SESSION_EXIT)
                            .withSourceUser(targetUserName)
                            .withText("User no longer online."));
                }
                break;
            case INVITE_DECLINE:
                forwardMessage(targetUserName, target, message);
                break;
            case SESSION_EXIT:
                server.deActivateSession(userName, message.getText());
                forwardMessageIfInSession(targetUserName, target, message);
                break;
            case TYPING:
                forwardMessageIfInSession(targetUserName, target, message);
                break;
            case NOT_TYPING:
                forwardMessageIfInSession(targetUserName, target, message);
                break;
            case TEXT_CLEARED:
                forwardMessageIfInSession(targetUserName, target, message);
                break;
            case MESSAGE:
                server.logSessionText(userName, targetUserName, message.getText());
                forwardMessageIfInSession(targetUserName, target, message);
                break;
            case LOGOUT:
                server.getActiveUsers().remove(userName);
                userName = null;
                break;
            default:
                send(new Message()
                        .withType(BAD_REQUEST)
                        .withSourceUser(SERVER)
                        .withText("Invalid Message type: " +message.getType()));
                break;
        }
    }

    // Only send these messages if the related session is active.
    private void forwardMessageIfInSession(final String targetUserName, final ServerThread target, final Message message) {
        if (null == targetUserName || "".equals(targetUserName)) {
            send(new Message()
                    .withType(BAD_REQUEST)
                    .withSourceUser(SERVER)
                    .withText("Target user name is required."));
        }
        if (server.isSessionActive(userName, targetUserName)) {
            if (null != target) {
                target.send(new Message()
                        .withType(message.getType())
                        .withSourceUser(userName)
                        .withText(message.getText()));
            } else {
                send(new Message()
                        .withType(SESSION_EXIT)
                        .withSourceUser(SERVER)
                        .withText("User no longer online."));
            }
        } else {
            send(new Message()
                    .withType(BAD_REQUEST)
                    .withSourceUser(SERVER)
                    .withText("Session is no longer active."));
        }
    }

    private void forwardMessage(final String targetUserName, final ServerThread target, final Message message) {
        if (null == targetUserName || "".equals(targetUserName)) {
            send(new Message()
                    .withType(BAD_REQUEST)
                    .withSourceUser(SERVER)
                    .withText("Target user name is required."));
        }
        if (null != target) {
            target.send(new Message()
                    .withType(message.getType())
                    .withSourceUser(userName)
                    .withText(message.getText()));
        } else {
            send(new Message()
                    .withType(SESSION_EXIT)
                    .withSourceUser(SERVER)
                    .withText("User no longer online."));
        }
    }

    // Returns a comma delimited string.
    private String setToString(final Set<String> strings) {
        StringBuilder builder = new StringBuilder();
        boolean isFirst = true;
        for (String string : strings) {
            if (isFirst) {
                builder.append(string);
                isFirst = false;
            } else {
                builder.append(",").append(string);
            }
        }
        return builder.toString();
    }

    public void close() {
        try {
            if (null != socket) {
                socket.close();
            }
            if (null != in) {
                in.close();
            }
            if (null != out) {
                out.close();
            }
        } catch (IOException e) {
            System.out.println("Error in ServerThread.close: " + e.getMessage());
        }
        server.removeThread(this);
        run = false;
        userName = null;
    }
}
