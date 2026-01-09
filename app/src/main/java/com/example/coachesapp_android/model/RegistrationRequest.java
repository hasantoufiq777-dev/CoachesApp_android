package com.example.coachesapp_android.model;

import java.time.LocalDateTime;

public class RegistrationRequest {
    private Integer id;
    private String username;
    private String password;
    private String fullName;
    private Role requestedRole;
    private Integer clubId;
    private String clubName;
    private Integer age;
    private String position;
    private RequestStatus status;
    private LocalDateTime requestDate;
    private LocalDateTime approvedDate;
    private String remarks;

    public enum RequestStatus {
        PENDING("Pending Admin Approval"),
        APPROVED("Approved"),
        REJECTED("Rejected");

        private final String displayName;

        RequestStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public RegistrationRequest() {
        this.status = RequestStatus.PENDING;
        this.requestDate = LocalDateTime.now();
    }

    public RegistrationRequest(String username, String password, String fullName, Role requestedRole, Integer clubId) {
        this();
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.requestedRole = requestedRole;
        this.clubId = clubId;
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRequestedRole() {
        return requestedRole;
    }

    public void setRequestedRole(Role requestedRole) {
        this.requestedRole = requestedRole;
    }

    public Integer getClubId() {
        return clubId;
    }

    public void setClubId(Integer clubId) {
        this.clubId = clubId;
    }

    public String getClubName() {
        return clubName;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public LocalDateTime getApprovedDate() {
        return approvedDate;
    }

    public void setApprovedDate(LocalDateTime approvedDate) {
        this.approvedDate = approvedDate;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    @Override
    public String toString() {
        return fullName + " (" + requestedRole.getDisplayName() + ")";
    }
}
