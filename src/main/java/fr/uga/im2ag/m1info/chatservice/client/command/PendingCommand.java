package fr.uga.im2ag.m1info.chatservice.client.command;

import fr.uga.im2ag.m1info.chatservice.common.MessageStatus;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.AckMessage;

/**
 * Interface for commands that await acknowledgment from the server.
 * Implements the Command pattern for tracking pending operations.
 */
public interface PendingCommand {
    /**
     * Get the unique identifier for this command.
     *
     * @return the command ID
     */
    String getCommandId();

    /**
     * Handle an acknowledgment message for this command.
     *
     * @param message the acknowledgment message
     * @return true if the command can be considered complete, and removed from pending list, false otherwise
     */
    default boolean handleAck(AckMessage message) {
        if (message.getAckType() == MessageStatus.FAILED) {
            onAckFailed(message.getErrorReason());
            return true;
        } else {
            return onAckReceived(message.getAckType());
        }
    }

    /**
     * Called when an acknowledgment is received for this command.
     *
     * @param ackType the type of acknowledgment received
     * @return true if the command can be considered complete, and removed from pending list, false otherwise
     */
    boolean onAckReceived(MessageStatus ackType);

    /**
     * Called when the command fails.
     *
     * @param reason the reason for failure
     */
    void onAckFailed(String reason);
}