package com.example.coachesapp_android.model;

public class Manager {
    private Integer id;
    private String name;
    private Integer age;
    private Club club;

    public Manager() {
    }

    public Manager(String managerName, String clubName) {
        this.name = managerName;
        this.club = new Club(clubName);
    }

    public Manager(String name, Club club) {
        this.name = name;
        this.club = club;
    }

    public Manager(String name, Integer age, Club club) {
        this.name = name;
        this.age = age;
        this.club = club;
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

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Club getClub() {
        return club;
    }

    public void setClub(Club club) {
        this.club = club;
    }

    @Override
    public String toString() {
        String clubInfo = (club != null) ? club.getClubName() : "No Club";
        String ageInfo = (age != null) ? age + " years old, " : "";
        return name + " (" + ageInfo + clubInfo + ")";
    }
}
