package fr.uga.im2ag.m1info.chatservice.server.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Class representing group information used by the chat server.
 */
public class GroupInfo implements Serializable {
    private final int id;
    private final int adminId;
    private String groupName;
    private final Set<Integer> members;
    private long lastLogin;

    /**
     * Constructs a GroupInfo instance with all fields specified.
     *
     * @param id        the group ID
     * @param adminID        the admin group ID
     * @param groupName  the groupName
     * @param members  the set of member IDs
     * @param lastLogin the timestamp of the last login
     */
    public GroupInfo(int id, int adminID, String groupName, Set<Integer> members, long lastLogin) {
        this.id = id;
        this.adminId = adminID;
        this.groupName = groupName;
        this.members = members;
        this.lastLogin = lastLogin;
    }

    /**
     * Constructs a GroupInfo instance with current time as last login.
     *
     * @param id       the group ID
     * @param adminID        the admin group ID
     * @param groupName the groupName
     * @param members the set of member IDs
     */
    public GroupInfo(int id, int adminID, String groupName, Set<Integer> members) {
        this(id, adminID, groupName, members, System.currentTimeMillis());
    }

    /**
     * Constructs a GroupInfo instance with no members and current time as last login.
     *
     * @param id       the group ID
     * @param adminID        the admin group ID
     * @param groupName the groupName
     */
    public GroupInfo(int id, int adminID, String groupName) {
        this(id, adminID, groupName, new HashSet<>(), System.currentTimeMillis());
    }

    /**
     * Gets the group ID.
     *
     * @return the group ID
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the group admin ID.
     *
     * @return the group admin ID
     */
    public int getAdminId() {
        return adminId;
    }
    /**
     * Gets the groupName.
     *
     * @return the groupName
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Checks if the group has a specific member ID.
     *
     * @param memberId the member ID to check
     * @return true if the member ID exists, false otherwise
     */
    public boolean hasMember(int memberId) {
        return members.contains(memberId);
    }

    /**
     * Gets the set of member IDs.
     *
     * @return the set of member IDs
     */
    public Set<Integer> getMembers() {
        return members;
    }

    /**
     * Gets the timestamp of the last login.
     *
     * @return the last login timestamp
     */
    public long getLastLogin() {
        return lastLogin;
    }

    /** Sets the groupName.
     *
     * @param groupName the new groupName
     */
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    /** Adds a member ID to the members set.
     *
     * @param memberId the member ID to add
     */
    public void addMember(int memberId) {
        members.add(memberId);
    }

    /** Removes a member ID from the members set.
     *
     * @param memberId the member ID to remove
     */
    public void removeMember(int memberId) {
        members.remove(memberId);
    }

    /** Updates the last login timestamp to the current time. */
    public void updateLastLogin() {
        this.lastLogin = System.currentTimeMillis();
    }
}
