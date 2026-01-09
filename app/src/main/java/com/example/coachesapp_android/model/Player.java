package com.example.coachesapp_android.model;

public class Player {
    private Integer id;
    private String name;
    private int age;
    private int jersey;
    private Position position;
    private boolean injured;
    private Integer clubId;
    private String clubView;

    public Player() {
    }

    public Player(String name, int age, int jersey, Position position) {
        this.name = name;
        this.age = age;
        this.jersey = jersey;
        this.position = position;
        this.injured = false;
        this.clubId = null;
    }

    public Player(String name, int age, int jersey, Position position, boolean injured) {
        this.name = name;
        this.age = age;
        this.jersey = jersey;
        this.position = position;
        this.injured = injured;
        this.clubId = null;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getJersey() {
        return jersey;
    }

    public void setJersey(int jersey) {
        this.jersey = jersey;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public boolean isInjured() {
        return injured;
    }

    public void setInjured(boolean injured) {
        this.injured = injured;
    }

    public Integer getClubId() {
        return clubId;
    }

    public void setClubId(Integer clubId) {
        this.clubId = clubId;
    }

    public String getClubView() {
        return clubView;
    }

    public void setClubView(String clubView) {
        this.clubView = clubView;
    }

    @Override
    public String toString() {
        return name + " (#" + jersey + " - " + position + ")";
    }
}
