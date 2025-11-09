package fr.uga.im2ag.m1info.chatservice.client.Model;

import java.util.HashSet;
import java.util.Set;

public class GroupClient {
    private final int groupId;
    private String name;
    private int adminId;
    private Set<Integer> members;

    public GroupClient(int groupId, String name, int adminId) {
        this.groupId = groupId;
        this.name = name;
        this.adminId = adminId;
        this.members = new HashSet<Integer>();
        members.add(adminId);
    }

    public int getGroupId() {
        return groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAdminId() {
        return adminId;
    }

    public Set<Integer> getMembers() {
        return members;
    }

    public void addMember(int memberId) {
        members.add(memberId);
    }

    public void removeMember(int memberId) {
        members.remove(memberId);
    }

    public boolean isAdmin(int userId) {
        return adminId == userId;
    }

    public boolean isMember(int userId) {
        return members.contains(userId);
    }

}
