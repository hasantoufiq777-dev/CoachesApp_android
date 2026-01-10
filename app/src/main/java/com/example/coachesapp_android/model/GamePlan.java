package com.example.coachesapp_android.model;

public class GamePlan {
    private Integer id;
    private Integer clubId;
    private Integer goalkeeperPlayerId;
    private Integer defender1PlayerId;
    private Integer defender2PlayerId;
    private Integer midfielder1PlayerId;
    private Integer midfielder2PlayerId;
    private Integer attackerPlayerId;
    
    // Player names for display
    private String goalkeeperName;
    private String defender1Name;
    private String defender2Name;
    private String midfielder1Name;
    private String midfielder2Name;
    private String attackerName;

    public GamePlan() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getClubId() {
        return clubId;
    }

    public void setClubId(Integer clubId) {
        this.clubId = clubId;
    }

    public Integer getGoalkeeperPlayerId() {
        return goalkeeperPlayerId;
    }

    public void setGoalkeeperPlayerId(Integer goalkeeperPlayerId) {
        this.goalkeeperPlayerId = goalkeeperPlayerId;
    }

    public Integer getDefender1PlayerId() {
        return defender1PlayerId;
    }

    public void setDefender1PlayerId(Integer defender1PlayerId) {
        this.defender1PlayerId = defender1PlayerId;
    }

    public Integer getDefender2PlayerId() {
        return defender2PlayerId;
    }

    public void setDefender2PlayerId(Integer defender2PlayerId) {
        this.defender2PlayerId = defender2PlayerId;
    }

    public Integer getMidfielder1PlayerId() {
        return midfielder1PlayerId;
    }

    public void setMidfielder1PlayerId(Integer midfielder1PlayerId) {
        this.midfielder1PlayerId = midfielder1PlayerId;
    }

    public Integer getMidfielder2PlayerId() {
        return midfielder2PlayerId;
    }

    public void setMidfielder2PlayerId(Integer midfielder2PlayerId) {
        this.midfielder2PlayerId = midfielder2PlayerId;
    }

    public Integer getAttackerPlayerId() {
        return attackerPlayerId;
    }

    public void setAttackerPlayerId(Integer attackerPlayerId) {
        this.attackerPlayerId = attackerPlayerId;
    }

    public String getGoalkeeperName() {
        return goalkeeperName;
    }

    public void setGoalkeeperName(String goalkeeperName) {
        this.goalkeeperName = goalkeeperName;
    }

    public String getDefender1Name() {
        return defender1Name;
    }

    public void setDefender1Name(String defender1Name) {
        this.defender1Name = defender1Name;
    }

    public String getDefender2Name() {
        return defender2Name;
    }

    public void setDefender2Name(String defender2Name) {
        this.defender2Name = defender2Name;
    }

    public String getMidfielder1Name() {
        return midfielder1Name;
    }

    public void setMidfielder1Name(String midfielder1Name) {
        this.midfielder1Name = midfielder1Name;
    }

    public String getMidfielder2Name() {
        return midfielder2Name;
    }

    public void setMidfielder2Name(String midfielder2Name) {
        this.midfielder2Name = midfielder2Name;
    }

    public String getAttackerName() {
        return attackerName;
    }

    public void setAttackerName(String attackerName) {
        this.attackerName = attackerName;
    }
}
