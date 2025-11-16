package fr.uga.im2ag.m1info.chatservice.common;

/** Enum representing different types of messages. */
public enum MessageType {
    TEXT,
    MEDIA,
    CREATE_USER,
    CONNECT_USER,
    ACK_CONNECTION,
    CREATE_GROUP,
    UPDATE_PSEUDO,
    CONTACT_REQUEST,
    CONTACT_REQUEST_RESPONSE,
    REMOVE_CONTACT,
    ADD_GROUP_MEMBER,
    REMOVE_GROUP_MEMBER,
    UPDATE_GROUP_NAME,
    DELETE_GROUP,
    LEAVE_GROUP,
    MESSAGE_REACTION,
    NOTIFICATION,
    EMPTY,
    ERROR,
    KEY_EXCHANGE,
    KEY_EXCHANGE_RESPONSE,
    ENCRYPTED_TEXT,
    ENCRYPTED_FILE_CHUNK,
    FILE_TRANSFER_START,
    FILE_TRANSFER_ACK,
    GROUP_KEY_DISTRIBUTION,
    MESSAGE_ACK,
    NONE;

    /** Convert an integer to a MessageType enum.
     *
     * @param i the integer to convert
     * @return the corresponding MessageType, or NONE if no match is found
     */
    public static MessageType fromInt(int i) {
        for (MessageType type : MessageType.values()) {
            if (type.ordinal() == i) {
                return type;
            }
        }
        return NONE;
    }

    /** Convert a byte to a MessageType enum.
     *
     * @param b the byte to convert
     * @return the corresponding MessageType, or NONE if no match is found
     */
    public static MessageType fromByte(byte b) {
        return fromInt(b);
    }

    /** Convert this MessageType enum to a byte.
     *
     * @return the byte representation of this MessageType
     */
    public byte toByte() {
        return (byte) this.ordinal();
    }

    /** Convert this MessageType enum to a string.
     *
     * @return the string representation of this MessageType
     */
    @Override
    public String toString() {
        return switch (this) {
            case TEXT -> "TEXT";
            case MEDIA -> "MEDIA";
            case CREATE_USER -> "CREATE_USER";
            case CONNECT_USER -> "CONNECT_USER";
            case ACK_CONNECTION -> "ACK_CONNECTION";
            case CREATE_GROUP -> "CREATE_GROUP";
            case UPDATE_PSEUDO -> "UPDATE_PSEUDO";
            case CONTACT_REQUEST -> "CONTACT_REQUEST";
            case CONTACT_REQUEST_RESPONSE -> "CONTACT_REQUEST_RESPONSE";
            case REMOVE_CONTACT -> "REMOVE_CONTACT";
            case ADD_GROUP_MEMBER -> "ADD_GROUP_MEMBER";
            case REMOVE_GROUP_MEMBER -> "REMOVE_GROUP_MEMBER";
            case UPDATE_GROUP_NAME -> "UPDATE_GROUP_NAME";
            case DELETE_GROUP -> "DELETE_GROUP";
            case LEAVE_GROUP -> "LEAVE_GROUP";
            case MESSAGE_REACTION -> "MESSAGE_REACTION";
            case NOTIFICATION -> "NOTIFICATION";
            case EMPTY -> "EMPTY";
            case ERROR -> "ERROR";
            case KEY_EXCHANGE -> "KEY_EXCHANGE";
            case KEY_EXCHANGE_RESPONSE -> "KEY_EXCHANGE_RESPONSE";
            case ENCRYPTED_TEXT -> "ENCRYPTED_TEXT";
            case ENCRYPTED_FILE_CHUNK -> "ENCRYPTED_FILE_CHUNK";
            case FILE_TRANSFER_START -> "FILE_TRANSFER_START";
            case FILE_TRANSFER_ACK -> "FILE_TRANSFER_ACK";
            case GROUP_KEY_DISTRIBUTION -> "GROUP_KEY_DISTRIBUTION";
            case MESSAGE_ACK -> "MESSAGE_ACK";
            case NONE -> "NONE";
        };
    }
}
