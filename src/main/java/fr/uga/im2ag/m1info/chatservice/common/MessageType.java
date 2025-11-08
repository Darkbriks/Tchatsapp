package fr.uga.im2ag.m1info.chatservice.common;

/** Enum representing different types of messages. */
public enum MessageType {
    TEXT,
    MEDIA,
    CREATE_USER,
    CREATE_GROUP,
    UPDATE_PSEUDO,
    ADD_CONTACT,
    ADD_GROUP_MEMBER,
    REMOVE_GROUP_MEMBER,
    UPDATE_GROUP_NAME,
    DELETE_GROUP,
    LEAVE_GROUP,
    MESSAGE_REACTION,
    NOTIFICATION,
    EMPTY,
    ERROR,
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
            case CREATE_GROUP -> "CREATE_GROUP";
            case UPDATE_PSEUDO -> "UPDATE_PSEUDO";
            case ADD_CONTACT -> "ADD_CONTACT";
            case ADD_GROUP_MEMBER -> "ADD_GROUP_MEMBER";
            case REMOVE_GROUP_MEMBER -> "REMOVE_GROUP_MEMBER";
            case UPDATE_GROUP_NAME -> "UPDATE_GROUP_NAME";
            case DELETE_GROUP -> "DELETE_GROUP";
            case LEAVE_GROUP -> "LEAVE_GROUP";
            case MESSAGE_REACTION -> "MESSAGE_REACTION";
            case NOTIFICATION -> "NOTIFICATION";
            case EMPTY -> "EMPTY";
            case ERROR -> "ERROR";
            case NONE -> "NONE";
        };
    }
}
