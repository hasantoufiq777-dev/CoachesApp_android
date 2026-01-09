package com.example.coachesapp_android.model;

public enum Role {
    SYSTEM_ADMIN("System Administrator"),
    CLUB_OWNER("Club Owner"),
    CLUB_MANAGER("Club Manager"),
    PLAYER("Player");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
