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
import java.util.Optional;
import java.util.Set;

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
 * This class manages the server side logic, which includes starting new threads as users connect that receive messages
 * from them, parses the message, and then takes action depending on the type of message.  Users can then connect to and
 * talk with other users through a managed session.  While the server is active all chat history is saved within
 * the session.
 *
 * See MessageTypeEnum for the list of allowable events and what they signify.
 */
public class Server extends Thread {

    // The map of currently active users by their sessionId.
    private Map<Integer, ServerThread> threadIdToUser;
    // The set of user names that are currently active.
    private Set<String> activeUsers;
    // The set of user names that have logged out.
    private Set<String> inactiveUsers;
    // The map of sessions
    private Map<String, Session> idToSession;
    private ServerSocket socket;
    private boolean run;

    public Server(final ServerSocket socket) {
        this.threadIdToUser = new HashMap<>();
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

    public synchronized void processClientMessage(final int sessionId, final Message message) {
        final ServerThread user = threadIdToUser.get(sessionId);
        Optional<ServerThread> match;
        if (null == message) {
            user.send(new Message(BAD_REQUEST, "Invalid Message."));
            return;
        }
        if (LOGIN != message.getType() && null == user.getUserName()) {
            user.send(new Message(ACCESS_DENIED, "Must log in first."));
            return;
        }
        switch(message.getType()) {
            case LOGIN:
                final String loggingInUserName = message.getBody();
                if (activeUsers.contains(loggingInUserName)) {
                    user.send(new Message(LOGIN_DENIED, "Username already exists."));
                } else {
                    activeUsers.add(loggingInUserName);
                    user.setUserName(loggingInUserName);
                    user.send(new Message(LOGIN_ACK));
                }
                break;
            case GET_USERS:
                user.send(new Message(GET_USERS, setToString(activeUsers)));
                break;
            case INVITE:
                final String invitedUserName = message.getBody();
                match = threadIdToUser.values().stream().filter(u -> invitedUserName.equals(u.getUserName())).findFirst();
                if (match.isPresent()) {
                    match.get().send(new Message(INVITE, user.getUserName()));
                } else {
                    user.send(new Message(INVITE_DECLINE, invitedUserName));
                }
                break;
            case INVITE_ACCEPT:
                final String acceptedUserName = message.getBody();
                match = threadIdToUser.values().stream().filter(u -> acceptedUserName.equals(u.getUserName())).findFirst();
                if (match.isPresent()) {
                    match.get().send(new Message(INVITE_ACCEPT, user.getUserName()));
                } else {
                    user.send(new Message(SESSION_EXIT, acceptedUserName));
                }
                ensureSession(acceptedUserName, user.getUserName());
                break;
            case INVITE_DECLINE:
                final String declinedUserName = message.getBody();
                match = threadIdToUser.values().stream().filter(u -> declinedUserName.equals(u.getUserName())).findFirst();
                if (match.isPresent()) {
                    match.get().send(new Message(INVITE_DECLINE, user.getUserName()));
                }
                break;
            case TYPING:

                break;
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

    private void ensureSession(final String userNameA, final String userNameB) {
        // combine the two user names for the session id.  Ensure that they are always in the same order.
        final String sessionId = userNameA.compareTo(userNameB) > 0 ? userNameB + userNameA : userNameA + userNameB;
        if (!idToSession.containsKey(sessionId)) {
            idToSession.put(sessionId, new Session());
        }
    }

    public synchronized void close() {
        for (ServerThread thread : threadIdToUser.values()) {
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
            threadIdToUser.put(serverThread.getThreadId(), serverThread);
        } catch (IOException e) {
            System.out.println("Error in ServerThreadPool.addThread: " + e.getMessage());
        }
    }

    public synchronized void removeThread(final ServerThread thread) {
        thread.close();
        threadIdToUser.remove(thread.getThreadId());
    }
}
