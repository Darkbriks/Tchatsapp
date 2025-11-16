package fr.uga.im2ag.m1info.chatservice.client.command;

import fr.uga.im2ag.m1info.chatservice.common.MessageStatus;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.AckMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for tracking and handling pending commands that await acknowledgment.
 * Implements automatic timeout handling for expired commands.
 */
public class PendingCommandManager {
    private final Map<String, PendingCommand> pendingCommands;

    /**
     * Constructor for PendingCommandManager.
     */
    public PendingCommandManager() {
        this.pendingCommands = new ConcurrentHashMap<>();
    }

    /**
     * Add a command to the pending queue.
     *
     * @param command the command to add
     */
    public void addPendingCommand(PendingCommand command) {
        pendingCommands.put(command.getCommandId(), command);
    }

    /**
     * Handle an acknowledgment message.
     *
     * @param ack the acknowledgment message
     */
    public void handleAck(AckMessage ack) {
        String msgId = ack.getAcknowledgedMessageId();
        PendingCommand command = pendingCommands.get(msgId);

        if (command == null) {
            return;
        }

        if (ack.getAckType() == MessageStatus.FAILED) {
            String reason = ack.getErrorReason() != null ? ack.getErrorReason() : "Unknown error";
            command.onAckFailed(reason);
            pendingCommands.remove(msgId);
        } else {
            command.onAckReceived(ack.getAckType());

            if (ack.getAckType() == MessageStatus.READ) {
                pendingCommands.remove(msgId);
            }
        }
    }

    /**
     * Remove a specific command from the pending queue.
     *
     * @param commandId the ID of the command to remove
     * @return the removed command, or null if not found
     */
    public PendingCommand removePendingCommand(String commandId) {
        return pendingCommands.remove(commandId);
    }

    /**
     * Shutdown the manager and clear all pending commands.
     */
    public void shutdown() {
        pendingCommands.clear();
        // TODO: Save pending commands to persistent storage if needed
    }
}