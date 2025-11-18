package fr.uga.im2ag.m1info.chatservice.client.event.types;

import fr.uga.im2ag.m1info.chatservice.client.event.system.Event;

/**
 * Event representing an update to the current user's pseudo.
 */
public class ChangeMenberInGroupEvent extends Event {
    private final int groupId;
    private final int menber;

    /** Constructor for the UserPseudoUpdatedEvent class.
     *
     * @param source The source object that generated the event.
     * @param groupId The new pseudo/username.
     */
    public ChangeMenberInGroupEvent(Object source, int groupID, int menber) {
        super(source);
        this.groupId = groupID;
        this.menber = menber;
    }

    /** Gets the new pseudo.
     *
     * @return The new pseudo/username.
     */
    public int getGroupId() {
        return groupId;
    }
    
    public int getMenber(){
        return menber;
    }

    /** Gets the specific type of the event.
     *
     * @return The class type of the event.
     */
    @Override
    public Class<? extends Event> getEventType() {
        return ChangeMenberInGroupEvent.class;
    }
}


