package model;

import model.enums.MessageTypeEnum;

/**
 * Represents a message being sent from a client to the server or vice versa.
 * The body could represent different things depending on the message type.
 */
public class Message {

    private MessageTypeEnum type;
    private String body;

    public Message(final MessageTypeEnum type) {
        this.type = type;
        this.body = "";
    }

    public Message(final MessageTypeEnum type, final String body) {
        this.type = type;
        this.body = body;
    }

    public MessageTypeEnum getType() {
        return type;
    }
    public void setType(MessageTypeEnum type) {
        this.type = type;
    }

    public String getBody() {
        return body;
    }
    public void setBody(String body) {
        this.body = body;
    }

    public Message withType(final MessageTypeEnum type) {
        setType(type);
        return this;
    }
    public Message withBody(final String body) {
        setBody(body);
        return this;
    }

    @Override
    public String toString() {
        return String.format("{'type':'%s','body':'%s'}", type, body);
    }
    public static Message toMessage(final String string) {
        return new Message(MessageTypeEnum.LOGIN, string);
    }
}
