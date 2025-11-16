package fr.uga.im2ag.m1info.chatservice.server.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Class representing group information used by the chat server.
 */
public class GroupInfo {
    private final int id;
    private final int adminId;
    private String groupName;
    private final Set<Integer> menbers;
    private long lastLogin;

    /**
     * Constructs a GroupInfo instance with all fields specified.
     *
     * @param id        the group ID
     * @param adminID        the admin group ID
     * @param groupName  the groupName
     * @param menbers  the set of menber IDs
     * @param lastLogin the timestamp of the last login
     */
    public GroupInfo(int id, int adminID, String groupName, Set<Integer> menbers, long lastLogin) {
        this.id = id;
        this.adminId = adminID;
        this.groupName = groupName;
        this.menbers = menbers;
        this.lastLogin = lastLogin;
    }

    /**
     * Constructs a GroupInfo instance with current time as last login.
     *
     * @param id       the group ID
     * @param adminID        the admin group ID
     * @param groupName the groupName
     * @param menbers the set of menber IDs
     */
    public GroupInfo(int id, int adminID, String groupName, Set<Integer> menbers) {
        this(id, adminID, groupName, menbers, System.currentTimeMillis());
    }

    /**
     * Constructs a GroupInfo instance with no menbers and current time as last login.
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
     * Checks if the group has a specific menber ID.
     *
     * @param menberId the menber ID to check
     * @return true if the menber ID exists, false otherwise
     */
    public boolean hasMenber(int menberId) {
        return menbers.contains(menberId);
    }

    /**
     * Gets the set of menber IDs.
     *
     * @return the set of menber IDs
     */
    public Set<Integer> getMenbers() {
        return menbers;
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

    /** Adds a menber ID to the menbers set.
     *
     * @param menberId the menber ID to add
     */
    public void addMenber(int menberId) {
        menbers.add(menberId);
    }

    /** Removes a menber ID from the menbers set.
     *
     * @param menberId the menber ID to remove
     */
    public void removeMenber(int menberId) {
        menbers.remove(menberId);
    }

    /** Updates the last login timestamp to the current time. */
    public void updateLastLogin() {
        this.lastLogin = System.currentTimeMillis();
    }
}
