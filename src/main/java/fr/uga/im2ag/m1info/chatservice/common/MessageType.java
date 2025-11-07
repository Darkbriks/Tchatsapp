package fr.uga.im2ag.m1info.chatservice.common;

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
    ERROR;

    public static MessageType fromByte(byte b){return MessageType.values()[b];}
    public byte  toByte(){return 0;}
}
