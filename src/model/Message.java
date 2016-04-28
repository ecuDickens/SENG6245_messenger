package model;

import model.enums.MessageTypeEnum;

/**
 * Represents a message being sent from a client to the server or vice versa.
 * The text could represent different things depending on the message type.
 */
public class Message {

    // What type of message it is.
    private MessageTypeEnum type;
    // Who the message is from.
    private String sourceUser = "";
    // Who the message is for (Could be null if a request to the server).
    private String targetUser = "";
    // The relevant messsage.
    private String text = "";

    public MessageTypeEnum getType() {
        return type;
    }
    public void setType(MessageTypeEnum type) {
        this.type = type;
    }

    public String getSourceUser() {
        return sourceUser;
    }
    public void setSourceUser(String sourceUser) {
        this.sourceUser = sourceUser;
    }

    public String getTargetUser() {
        return targetUser;
    }
    public void setTargetUser(String targetUser) {
        this.targetUser = targetUser;
    }

    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }

    public Message withType(final MessageTypeEnum type) {
        setType(type);
        return this;
    }
    public Message withSourceUser(final String sourceUser) {
        setSourceUser(sourceUser);
        return this;
    }
    public Message withTargetUser(final String targetUser) {
        setTargetUser(targetUser);
        return this;
    }
    public Message withText(final String text) {
        setText(text);
        return this;
    }

    @Override
    public String toString() {
        return String.format("{'type':'%s','source_user':'%s','target_user':'%s','text':'%s'}", type, sourceUser, targetUser, text);
    }
    // Poor man's JSON deserializer...
    public static Message toMessage(final String string) {
        if (!string.startsWith("{'type':'")) {
            return null;
        }
        String[] tokens = string.replace("{'type':'", "").split("','source_user':'", 2);
        if (2 != tokens.length) {
            return null; // type is mandatory
        }
        final MessageTypeEnum type = MessageTypeEnum.from(tokens[0]);
        if (null == type) {
            return null;
        }

        final String sourceUserName;
        if (tokens[1].startsWith("','target_user':'")) {
            return null; // source user is mandatory
        } else {
            tokens = tokens[1].split("','target_user':'", 2);
            if (2 != tokens.length) {
                return null;
            }
            sourceUserName = tokens[0];
        }

        // if target user might be empty.
        final String targetUserName;
        if (tokens[1].startsWith("','text':'")) {
            targetUserName = "";
            tokens[1] = tokens[1].replace("','text':'", "");
        } else {
            tokens = tokens[1].split("','text':'", 2);
            if (2 != tokens.length) {
                return null;
            }
            targetUserName = tokens[0];
        }

        if (!tokens[1].endsWith("'}")) {
            return null;
        }
        final String text = tokens[1].substring(0, tokens[1].lastIndexOf("'}"));

        return new Message().withType(type)
                .withSourceUser(sourceUserName)
                .withTargetUser(targetUserName)
                .withText(text);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Message) {
            final Message message = (Message) obj;
            return type == message.getType() &&
                    sourceUser.equals(message.getSourceUser()) &&
                    targetUser.equals(message.getTargetUser()) &&
                    text.equals(message.getText());
        }
        return false;
    }
}
