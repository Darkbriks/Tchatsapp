package fr.uga.im2ag.m1info.chatservice.common.messagefactory.providers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;

import java.util.Set;

/**
 * Interface for providing message types and creating ProtocolMessage instances.
 * <p>
 * Multiple MessageTypes can be bind to a single provider,
 * but only the last one will be used if there are conflicts.
 */
public interface MessageProvider {
    Set<MessageType> getType();
    ProtocolMessage createInstance();
}
