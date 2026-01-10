package com.example.coachesapp_android.util;

import android.content.Context;

import com.example.coachesapp_android.database.FirebaseClubRepository;
import com.example.coachesapp_android.database.FirebaseGamePlanRepository;
import com.example.coachesapp_android.database.FirebasePlayerRepository;
import com.example.coachesapp_android.database.FirebaseUserRepository;
import com.example.coachesapp_android.database.IClubRepository;
import com.example.coachesapp_android.database.IGamePlanRepository;
import com.example.coachesapp_android.database.IPlayerRepository;
import com.example.coachesapp_android.database.IUserRepository;

/**
 * Factory class to provide Firebase repository instances.
 */
public class RepositoryFactory {
    private static Context applicationContext;
    
    // Singleton instances
    private static IPlayerRepository playerRepository;
    private static IClubRepository clubRepository;
    private static IUserRepository userRepository;
    private static IGamePlanRepository gamePlanRepository;
    
    /**
     * Initialize the factory with application context.
     * Call this in your Application class or main activity.
     */
    public static void init(Context context) {
        applicationContext = context.getApplicationContext();
    }
    
    /**
     * Get PlayerRepository instance (Firebase).
     */
    public static IPlayerRepository getPlayerRepository() {
        if (playerRepository == null) {
            playerRepository = new FirebasePlayerRepository();
        }
        return playerRepository;
    }
    
    /**
     * Get ClubRepository instance (Firebase).
     */
    public static IClubRepository getClubRepository() {
        if (clubRepository == null) {
            clubRepository = new FirebaseClubRepository();
        }
        return clubRepository;
    }
    
    /**
     * Get UserRepository instance (Firebase).
     */
    public static IUserRepository getUserRepository() {
        if (userRepository == null) {
            userRepository = new FirebaseUserRepository();
        }
        return userRepository;
    }
    
    /**
     * Get Firebase UserRepository with additional methods
     */
    public static FirebaseUserRepository getFirebaseUserRepository() {
        return (FirebaseUserRepository) getUserRepository();
    }
    
    /**
     * Get GamePlanRepository instance (Firebase).
     */
    public static IGamePlanRepository getGamePlanRepository() {
        if (gamePlanRepository == null) {
            gamePlanRepository = new FirebaseGamePlanRepository();
        }
        return gamePlanRepository;
    }
}
