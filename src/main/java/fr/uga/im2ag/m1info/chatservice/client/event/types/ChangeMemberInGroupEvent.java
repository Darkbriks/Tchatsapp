package fr.uga.im2ag.m1info.chatservice.client.event.types;

import fr.uga.im2ag.m1info.chatservice.client.event.system.Event;

/**
 * Event representing an update to the current user's pseudo.
 */
public class ChangeMemberInGroupEvent extends Event {
    private final int groupId;
    private final int member;

    /** Constructor for the UserPseudoUpdatedEvent class.
     *
     * @param source The source object that generated the event.
     * @param groupId The new pseudo/username.
     */
    public ChangeMemberInGroupEvent(Object source, int groupID, int member) {
        super(source);
        this.groupId = groupID;
        this.member = member;
    }

    /** Gets the new pseudo.
     *
     * @return The new pseudo/username.
     */
    public int getGroupId() {
        return groupId;
    }
    
    public int getMember(){
        return member;
    }

    /** Gets the specific type of the event.
     *
     * @return The class type of the event.
     */
    @Override
    public Class<? extends Event> getEventType() {
        return ChangeMemberInGroupEvent.class;
    }
}


