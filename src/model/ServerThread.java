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

    private static String SERVER = "server";

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

    public void run() {
        while (run) {
            receive();
        }
    }

    // Synchronize on this in case multiple other server threads are trying to forward messages.
    public synchronized void send(final Message message) {
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
            processMessage(toMessage(streamIn.readUTF()));
        } catch (IOException e) {
            System.out.println("Error in ServerThread.receive: " + e.getMessage());
            server.removeThread(this);
        }
    }

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
                    server.addActiveUser(message.getSourceUser());
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
                forwardMessage(targetUserName, target, message);
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
                server.removeThread(this);
                close();
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
        userName = null;
    }
}
