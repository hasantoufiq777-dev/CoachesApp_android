package com.example.coachesapp_android.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.coachesapp_android.model.Role;
import com.example.coachesapp_android.model.User;

public class SessionManager {
    private static final String PREF_NAME = "CoachAppSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROLE = "role";
    private static final String KEY_CLUB_ID = "clubId";
    private static final String KEY_PLAYER_ID = "playerId";
    private static final String KEY_MANAGER_ID = "managerId";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void createLoginSession(User user) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, user.getId());
        editor.putString(KEY_USERNAME, user.getUsername());
        editor.putString(KEY_ROLE, user.getRole().name());
        
        if (user.getClubId() != null) {
            editor.putInt(KEY_CLUB_ID, user.getClubId());
        } else {
            editor.remove(KEY_CLUB_ID);
        }
        
        if (user.getPlayerId() != null) {
            editor.putInt(KEY_PLAYER_ID, user.getPlayerId());
        } else {
            editor.remove(KEY_PLAYER_ID);
        }
        
        if (user.getManagerId() != null) {
            editor.putInt(KEY_MANAGER_ID, user.getManagerId());
        } else {
            editor.remove(KEY_MANAGER_ID);
        }
        
        editor.commit();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public User getCurrentUser() {
        if (!isLoggedIn()) {
            return null;
        }
        
        User user = new User();
        user.setId(prefs.getInt(KEY_USER_ID, 0));
        user.setUsername(prefs.getString(KEY_USERNAME, ""));
        user.setRole(Role.valueOf(prefs.getString(KEY_ROLE, "PLAYER")));
        
        if (prefs.contains(KEY_CLUB_ID)) {
            user.setClubId(prefs.getInt(KEY_CLUB_ID, 0));
        }
        
        if (prefs.contains(KEY_PLAYER_ID)) {
            user.setPlayerId(prefs.getInt(KEY_PLAYER_ID, 0));
        }
        
        if (prefs.contains(KEY_MANAGER_ID)) {
            user.setManagerId(prefs.getInt(KEY_MANAGER_ID, 0));
        }
        
        return user;
    }

    public void logout() {
        editor.clear();
        editor.commit();
    }
}
