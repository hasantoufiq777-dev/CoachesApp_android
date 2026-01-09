package com.example.coachesapp_android.model;

public enum Position {
    FORWARD("Forward"),
    MIDFIELDER("Midfielder"),
    DEFENDER("Defender"),
    GOALKEEPER("Goalkeeper");

    private final String displayName;

    Position(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
