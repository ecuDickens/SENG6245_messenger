package model;

import model.enums.SessionStatusEnum;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * This class manages the thread pool of user connections, active user names, and sessions between users.  Communication between
 * server and clients is defined by the messaging enums in MessageTypeEnum.
 *
 * The methods provided on this class are mostly to facilitate passing messages between users.  The synchronized keyword
 * is used on several methods to ensure that the accessed information isn't corrupted due to multiple server thread requests.
 */
public class Server extends Thread {

    // The map of currently active users by their sessionId.
    private Map<Integer, ServerThread> threadIdToUser;
    // The set of user names that are currently active (could be different from the map values
    // because a user may not have logged in yet with a user name).
    private Set<String> activeUsers;
    // The map of sessions that have been started while this server has been running.
    private Map<String, Session> idToSession;
    // The socket this server is listening to for requests.
    private ServerSocket socket;
    private boolean run;

    public Server(final ServerSocket socket) {
        this.threadIdToUser = new HashMap<>();
        this.socket = socket;
        this.activeUsers = new HashSet<>();
        this.idToSession = new HashMap<>();
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
    private synchronized void addThread(final Socket socket) {
        try {
            final ServerThread thread = new ServerThread(this, socket,
                    new DataInputStream(new BufferedInputStream(socket.getInputStream())),
                    new DataOutputStream(new BufferedOutputStream(socket.getOutputStream())));
            System.out.println("Connected to new client: " + thread.getThreadId());
            threadIdToUser.put(thread.getThreadId(), thread);
        } catch (IOException e) {
            System.out.println("Error in ServerThreadPool.addThread: " + e.getMessage());
        }
    }

    public synchronized void removeThread(final ServerThread thread) {
        System.out.println("Disconnecting from client: " +thread.getThreadId());
        activeUsers.remove(thread.getUserName());
        threadIdToUser.remove(thread.getThreadId());
    }

    public synchronized Set<String> getActiveUsers() {
        return activeUsers;
    }
    public synchronized ServerThread getUserByUserName(final String userName) {
        final Optional<ServerThread> match = threadIdToUser.values().stream().filter(u -> userName.equals(u.getUserName())).findFirst();
        if (match.isPresent()) {
            return match.get();
        }
        return null;
    }

    // Create a session or reactivates an existing session.
    public synchronized void activateSession(final String userNameA, final String userNameB) {
        final String sessionId = getSessionId(userNameA, userNameB);
        System.out.println("Activating session " + sessionId);
        if (!idToSession.containsKey(sessionId)) {
            idToSession.put(sessionId, new Session().withId(sessionId).withLog(new ArrayList<>()).withStatus(SessionStatusEnum.ACTIVE));
        } else {
            idToSession.get(sessionId).setStatus(SessionStatusEnum.ACTIVE);
        }
    }

    // Deactivates a session if either user leaves it.
    public synchronized void deActivateSession(final String userNameA, final String userNameB) {
        final String sessionId = getSessionId(userNameA, userNameB);
        System.out.println("Deactivating session " + sessionId);
        if (idToSession.containsKey(sessionId)) {
            idToSession.get(sessionId).setStatus(SessionStatusEnum.INACTIVE);
        }
    }

    public synchronized boolean isSessionActive(final String userNameA, final String userNameB) {
        final String sessionId = getSessionId(userNameA, userNameB);
        return idToSession.containsKey(sessionId) && idToSession.get(sessionId).getStatus() == SessionStatusEnum.ACTIVE;
    }

    // Saves a message to the session.
    public synchronized void logSessionText(final String userNameA, final String userNameB, final String text) {
        final String sessionId = getSessionId(userNameA, userNameB);
        if (idToSession.containsKey(sessionId)) {
            idToSession.get(sessionId).getLog().add(text);
        }
    }

    // Combine the two user names for the session id.  Ensure that they are always in the same order.
    private String getSessionId(final String a, final String b) {
        return a.compareTo(b) > 0 ? b + a : a + b;
    }

    public void close() {
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
}
