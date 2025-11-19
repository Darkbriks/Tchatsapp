package fr.uga.im2ag.m1info.chatservice.server.model;

import java.util.Set;

import fr.uga.im2ag.m1info.chatservice.server.repository.UserRepository;

public class GroupInfo {

    private final int id;
    private String groupName;
    private Set<UserInfo> members;
    private int adminId;

    public int getAdminId() {
        return adminId;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public int getId() {
        return id;
    }

    public String getGroupName() {
        return groupName;
    }

    public Set<UserInfo> getMembers() {
        return members;
    }

    public void setMembers(Set<UserInfo> members){
        this.members = members;
    }

    public void addMember(UserRepository repo, int user) {
        UserInfo memberToAdd = repo.findById(user);
        members.add(memberToAdd);
    }

    public void addMember(UserInfo user) {
        members.add(user);
    }

    public GroupInfo(int id, String groupName, Set<UserInfo> members, int adminId) {
        this.id = id;
        this.groupName = groupName;
        this.members = members;
        this.adminId = adminId;
    }

}
