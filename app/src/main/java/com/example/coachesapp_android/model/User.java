package com.example.coachesapp_android.model;

public class User {
    private Integer id;
    private String username;
    private String password;
    private String email;
    private Role role;
    private Integer age;
    private Integer clubId;
    private Integer playerId;
    private Integer managerId;
    private Position preferredPosition; // For players registering
    private boolean approved; // Approval status for player/manager accounts

    public User() {
        this.approved = false; // Default to not approved
    }

    public User(Integer id, String username, String password, Role role, Integer clubId, Integer playerId, Integer managerId) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.clubId = clubId;
        this.playerId = playerId;
        this.managerId = managerId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Integer getClubId() {
        return clubId;
    }

    public void setClubId(Integer clubId) {
        this.clubId = clubId;
    }

    public Integer getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Integer playerId) {
        this.playerId = playerId;
    }

    public Integer getManagerId() {
        return managerId;
    }

    public void setManagerId(Integer managerId) {
        this.managerId = managerId;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }
    
    public Position getPreferredPosition() {
        return preferredPosition;
    }
    
    public void setPreferredPosition(Position preferredPosition) {
        this.preferredPosition = preferredPosition;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", role=" + role +
                ", clubId=" + clubId +
                ", playerId=" + playerId +
                ", managerId=" + managerId +
                '}';
    }
}
