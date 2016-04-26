package model.enums;

/**
 * The list of allowable message types.
 */
public enum MessageTypeEnum {
    LOGIN,              // Always sent first, establishing the user name and setting its status to online (not really a log in as a handshake.
    LOGIN_ACK,          // Denotes that the server has accepted the login.
    LOGIN_DENIED,       // Denotes that the server rejected the login.
    ACCESS_DENIED,      // Sent to clients that haven't logged in yet.
    BAD_REQUEST,        // Sent to clients that sent unreadable messages.
    GET_USERS,          // Returns the list of online user names to the requester.
    INVITE,             // Invites a user to a session.
    INVITE_DECLINE,     // Indicates that a user has declined an invite.
    INVITE_ACCEPT,      // Indicates that a user has accepted an invite.
    SESSION_EXIT,       // Indicates that a user has left a session.
    TYPING,             // Indicates that a user is typing.
    NOT_TYPING,         // Indicates that a user has stopped typing.
    TEXT_CLEARED,       // Indicates that a user no longer has text in their prompt.
    MESSAGE,            // Sends a message to the other user in a session.
    LOGOUT              // Sent before terminating a client connection, setting the user name status to offline.
}
