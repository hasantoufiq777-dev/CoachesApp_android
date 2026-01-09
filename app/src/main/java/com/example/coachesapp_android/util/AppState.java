package com.example.coachesapp_android.util;

import com.example.coachesapp_android.model.Club;
import com.example.coachesapp_android.model.Manager;
import com.example.coachesapp_android.model.Player;
import com.example.coachesapp_android.model.User;

import java.util.ArrayList;
import java.util.List;

public class AppState {
    private static AppState instance;
    public List<Player> players = new ArrayList<>();
    public List<Manager> managers = new ArrayList<>();
    public List<Club> clubs = new ArrayList<>();
    public User currentUser;
    public Player selectedPlayer;

    private AppState() {
    }

    public static synchronized AppState getInstance() {
        if (instance == null) {
            instance = new AppState();
        }
        return instance;
    }

    public void setSelectedPlayer(Player player) {
        this.selectedPlayer = player;
    }

    public Player getSelectedPlayer() {
        return selectedPlayer;
    }

    public void clearData() {
        players.clear();
        managers.clear();
        clubs.clear();
        currentUser = null;
        selectedPlayer = null;
    }
}
