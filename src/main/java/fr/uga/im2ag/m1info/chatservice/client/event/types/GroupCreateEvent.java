package fr.uga.im2ag.m1info.chatservice.client.event.types;

import fr.uga.im2ag.m1info.chatservice.client.event.system.Event;

/**
 * Event representing a creation of a group.
 */
public class GroupCreateEvent extends Event {
    private final int groupId;
    private final String groupName;

    /** Constructor for the GroupCreationEvent class.
     *
     * @param source The source object that generated the event.
     * @param groupId The id of the group
     * @param groupName The name of the group
     */
    public GroupCreateEvent(Object source, int groupID, String groupName) {
        super(source);
        this.groupId = groupID;
        this.groupName = groupName;
    }

    /** Gets the group id.
     *
     * @return The id of the group.
     */
    public int getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    /** Gets the specific type of the event.
     *
     * @return The class type of the event.
     */
    @Override
    public Class<? extends Event> getEventType() {
        return GroupCreateEvent.class;
    }
}

