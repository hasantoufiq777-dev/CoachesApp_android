package com.example.coachesapp_android.model;

import java.util.ArrayList;
import java.util.List;

public class Club {
    private Integer id;
    private String firestoreDocId; // Actual Firestore document ID
    private String clubName;
    private List<Player> players;

    public Club() {
        this.players = new ArrayList<>();
    }

    public Club(String clubName) {
        this.clubName = clubName;
        this.players = new ArrayList<>();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFirestoreDocId() {
        return firestoreDocId;
    }

    public void setFirestoreDocId(String firestoreDocId) {
        this.firestoreDocId = firestoreDocId;
    }

    public String getClubName() {
        return clubName;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void addPlayer(Player player) {
        if (player != null) {
            players.add(player);
        }
    }

    public boolean removePlayer(Player player) {
        return players.remove(player);
    }

    @Override
    public String toString() {
        return clubName;
    }
}
