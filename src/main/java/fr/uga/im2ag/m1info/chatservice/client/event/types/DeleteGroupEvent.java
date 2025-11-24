package fr.uga.im2ag.m1info.chatservice.client.event.types;

import fr.uga.im2ag.m1info.chatservice.client.event.system.Event;

/**
 * Event representing the reception of a text message in a conversation.
 */
public class DeleteGroupEvent extends Event {

    private final int groupId;
    private final boolean isAdded; // true if member is added, false if removed

    /** Constructor for the UserPseudoUpdatedEvent class.
     *
     * @param source The source object that generated the event.
     * @param groupId The bgroup id  
     * @param isAdded True if the member is added, false if removed.
     */
    public DeleteGroupEvent(Object source, int groupId, boolean isAdded) {
        super(source);
        this.groupId = groupId;
        this.isAdded = isAdded;
    }

    /** Gets the group ID where the member change occurred.
     *
     * @return The group ID.
     */
    public int getGroupId() {
        return groupId;
    }


    /** Indicates whether the group name was changed or not 
     *
     * @return True if the group name was change, false else 
     */
    public boolean isChanged() {
        return isAdded;
    }

    /** Gets the specific type of the event.
     *
     * @return The class type of the event.
     */
    @Override
    public Class<? extends Event> getEventType() {
        return DeleteGroupEvent.class;
    }
}


