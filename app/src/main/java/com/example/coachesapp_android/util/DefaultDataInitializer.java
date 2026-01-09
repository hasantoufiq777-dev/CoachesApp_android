package com.example.coachesapp_android.util;

import android.util.Log;

import com.example.coachesapp_android.database.FirebaseUserRepository;
import com.example.coachesapp_android.model.Role;
import com.example.coachesapp_android.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Initializes default data like admin account
 */
public class DefaultDataInitializer {
    private static final String TAG = "DefaultDataInitializer";
    private static final String ADMIN_EMAIL = "admin@gmail.com";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";
    
    private final FirebaseAuth firebaseAuth;
    private final FirebaseUserRepository userRepository;
    
    public DefaultDataInitializer() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.userRepository = RepositoryFactory.getFirebaseUserRepository();
    }
    
    /**
     * Creates default admin account if it doesn't exist
     */
    public void initializeDefaultAdmin(OnInitializationListener listener) {
        new Thread(() -> {
            try {
                // Check if admin already exists
                User existingAdmin = userRepository.findByUsername(ADMIN_USERNAME);
                if (existingAdmin != null) {
                    Log.d(TAG, "Admin account already exists");
                    if (listener != null) {
                        listener.onComplete(true, "Admin already exists");
                    }
                    return;
                }
                
                // Create admin account in Firebase Auth
                firebaseAuth.createUserWithEmailAndPassword(ADMIN_EMAIL, ADMIN_PASSWORD)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser firebaseUser = authResult.getUser();
                        if (firebaseUser != null) {
                            // Create admin user profile in Firestore
                            User adminUser = new User();
                            adminUser.setUsername(ADMIN_USERNAME);
                            adminUser.setEmail(ADMIN_EMAIL);
                            adminUser.setRole(Role.SYSTEM_ADMIN);
                            adminUser.setAge(0); // Not applicable for admin
                            adminUser.setClubId(null);
                            adminUser.setApproved(true); // Admin is always approved
                            
                            new Thread(() -> {
                                User savedUser = userRepository.save(adminUser);
                                if (savedUser != null) {
                                    Log.d(TAG, "Default admin account created successfully");
                                    if (listener != null) {
                                        listener.onComplete(true, "Admin created");
                                    }
                                } else {
                                    Log.e(TAG, "Failed to save admin profile to Firestore");
                                    if (listener != null) {
                                        listener.onComplete(false, "Failed to save admin profile");
                                    }
                                }
                            }).start();
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Check if account already exists in Firebase Auth
                        if (e.getMessage() != null && e.getMessage().contains("already in use")) {
                            Log.d(TAG, "Admin account already exists in Firebase Auth");
                            if (listener != null) {
                                listener.onComplete(true, "Admin exists in Auth");
                            }
                        } else {
                            Log.e(TAG, "Failed to create admin account", e);
                            if (listener != null) {
                                listener.onComplete(false, e.getMessage());
                            }
                        }
                    });
                    
            } catch (Exception e) {
                Log.e(TAG, "Error initializing admin account", e);
                if (listener != null) {
                    listener.onComplete(false, e.getMessage());
                }
            }
        }).start();
    }
    
    public interface OnInitializationListener {
        void onComplete(boolean success, String message);
    }
}
