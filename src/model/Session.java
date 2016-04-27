package model;

import model.enums.SessionStatusEnum;

import java.util.List;

/**
 * Represents a conversation between two users.
 * Stores the chat history between the two for the life of the server.
 */
public class Session {

    // The id is comprised of the sorted concatenation of the two user's names.
    private String id;

    private SessionStatusEnum status;

    private List<String> log;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public SessionStatusEnum getStatus() {
        return status;
    }
    public void setStatus(SessionStatusEnum status) {
        this.status = status;
    }

    public List<String> getLog() {
        return log;
    }
    public void setLog(List<String> log) {
        this.log = log;
    }

    public Session withId(final String id) {
        setId(id);
        return this;
    }
    public Session withStatus(final SessionStatusEnum status) {
        setStatus(status);
        return this;
    }
    public Session withLog(final List<String> log) {
        setLog(log);
        return this;
    }
}
