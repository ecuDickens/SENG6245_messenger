package model.enums;

import java.util.EnumSet;
import java.util.Optional;

/**
 * The list of allowable message types.
 */
public enum MessageTypeEnum {
    LOGIN("LOGIN"),                     // Always sent first, establishing the user name and setting its status to online (not really a log in as a handshake.
    LOGIN_ACK("LOGIN_ACK"),             // Denotes that the server has accepted the login.
    LOGIN_DENIED("LOGIN_DENIED"),       // Denotes that the server rejected the login.
    ACCESS_DENIED("ACCESS_DENIED"),     // Sent to clients that haven't logged in yet.
    BAD_REQUEST("BAD_REQUEST"),         // Sent to clients that sent unreadable messages.
    GET_USERS("GET_USERS"),             // Returns the list of online user names to the requester.
    INVITE("INVITE"),                   // Invites a user to a session.
    INVITE_DECLINE("INVITE_DECLINE"),   // Indicates that a user has declined an invite.
    INVITE_ACCEPT("INVITE_ACCEPT"),     // Indicates that a user has accepted an invite.
    SESSION_EXIT("SESSION_EXIT"),       // Indicates that a user has left a session.
    TYPING("TYPING"),                   // Indicates that a user is typing.
    NOT_TYPING("NOT_TYPING"),           // Indicates that a user has stopped typing.
    TEXT_CLEARED("TEXT_CLEARED"),       // Indicates that a user no longer has text in their prompt.
    MESSAGE("MESSAGE"),                 // Sends a message to the other user in a session.
    LOGOUT("LOGOUT");                   // Sent before terminating a client connection, setting the user name status to offline.

    private final String value;
    private static EnumSet<MessageTypeEnum> FULL_SET = EnumSet.allOf(MessageTypeEnum.class);

    private MessageTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static MessageTypeEnum from(final String value) {
        final Optional<MessageTypeEnum> optional = FULL_SET.stream().filter(field -> field.getValue().equals(value)).findFirst();
        return optional.isPresent() ? optional.get() : null;
    }

    @Override
    public String toString() {
        return value;
    }
}
