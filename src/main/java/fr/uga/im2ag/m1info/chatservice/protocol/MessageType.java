// package fr.uga.im2ag.m1info.chatservice.protocol;
 
// /**
//  * Defines the types of messages that can be exchanged in the protocol.
//  */
// public enum MessageType {
//     KEY_EXCHANGE((byte) 0x01),
//     KEY_EXCHANGE_RESPONSE((byte) 0x02),
//     ENCRYPTED_TEXT((byte) 0x03),
//     ENCRYPTED_FILE_CHUNK((byte) 0x04),
//     FILE_TRANSFER_START((byte) 0x05),
//     FILE_TRANSFER_ACK((byte) 0x06),
//     GROUP_KEY_DISTRIBUTION((byte) 0x07);
 
//     private final byte code;
 
//     MessageType(byte code) {
//         this.code = code;
//     }
 
//     public byte getCode() {
//         return code;
//     }
 
//     /**
//      * Converts a byte code to a MessageType.
//      * @param code The byte code
//      * @return The corresponding MessageType
//      * @throws IllegalArgumentException if code is unknown
//      */
//     public static MessageType fromCode(byte code) {
//         for (MessageType type : values()) {
//             if (type.code == code) {
//                 return type;
//             }
//         }
//         throw new IllegalArgumentException("Unknown message type code: " + code);
//     }
// }