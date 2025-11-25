package fr.uga.im2ag.m1info.chatservice.client.event.types;

import fr.uga.im2ag.m1info.chatservice.client.event.system.Event;
import fr.uga.im2ag.m1info.chatservice.client.model.Message;

public class ReactionMessageReceivedEvent extends MessageEvent {

    public ReactionMessageReceivedEvent(Object source, String conversationId, Message message) {
        super(source, conversationId, message);
    }

    @Override
    public Class<? extends Event> getEventType() {
        return ReactionMessageReceivedEvent.class;
    }

}
