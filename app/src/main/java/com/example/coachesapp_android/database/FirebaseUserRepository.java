package com.example.coachesapp_android.database;

import android.util.Log;

import com.example.coachesapp_android.model.Position;
import com.example.coachesapp_android.model.Role;
import com.example.coachesapp_android.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class FirebaseUserRepository implements IUserRepository {
    private static final String TAG = "FirebaseUserRepo";
    private static final String COLLECTION_NAME = "users";
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public FirebaseUserRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    @Override
    public User save(User user) {
        CountDownLatch latch = new CountDownLatch(1);
        final User[] result = new User[1];
        
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", user.getUsername());
        userData.put("email", user.getEmail() != null ? user.getEmail() : "");
        userData.put("role", user.getRole().name());
        userData.put("clubId", user.getClubId() != null ? user.getClubId() : 0);
        userData.put("playerId", user.getPlayerId() != null ? user.getPlayerId() : 0);
        userData.put("managerId", user.getManagerId() != null ? user.getManagerId() : 0);
        userData.put("age", user.getAge() != null ? user.getAge() : 0);
        userData.put("approved", user.isApproved());
        userData.put("preferredPosition", user.getPreferredPosition() != null ? user.getPreferredPosition().name() : null);

        if (user.getId() != null) {

            Log.d(TAG, "Updating user: " + user.getUsername() + ", Approved: " + user.isApproved());
            db.collection(COLLECTION_NAME)
                    .whereEqualTo("username", user.getUsername())
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            String docId = querySnapshot.getDocuments().get(0).getId();
                            db.collection(COLLECTION_NAME)
                                    .document(docId)
                                    .update(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        result[0] = user;
                                        latch.countDown();
                                        Log.d(TAG, "User updated successfully: " + user.getUsername() + ", Approved: " + user.isApproved());
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error updating user document", e);
                                        latch.countDown();
                                    });
                        } else {
                            Log.e(TAG, "User document not found for update: " + user.getUsername());
                            latch.countDown();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error finding user for update", e);
                        latch.countDown();
                    });
        } else {

            Log.d(TAG, "Creating new user in Firestore: " + user.getUsername() + ", Approved: " + user.isApproved());
            db.collection(COLLECTION_NAME)
                    .add(userData)
                    .addOnSuccessListener(documentReference -> {
                        String docId = documentReference.getId();
                        user.setId(docId.hashCode());

                        userData.put("firestoreDocId", docId);
                        userData.put("id", user.getId());
                        documentReference.set(userData);
                        result[0] = user;
                        latch.countDown();
                        Log.d(TAG, "User added successfully: " + user.getUsername() + " with docId: " + docId + ", approved: " + user.isApproved());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error adding user: " + user.getUsername(), e);
                        Log.e(TAG, "Error message: " + e.getMessage());
                        latch.countDown();
                    });
        }

        try {
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            if (!completed) {
                Log.e(TAG, "Timeout waiting for save operation");
            }
            if (result[0] == null) {
                Log.e(TAG, "Save operation completed but result is null");
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while waiting for save", e);
        }
        
        return result[0];
    }

    @Override
    public User findByUsername(String username) {
        CountDownLatch latch = new CountDownLatch(1);
        final User[] result = new User[1];

        db.collection(COLLECTION_NAME)
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        result[0] = documentToUser(document);
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding user by username", e);
                    latch.countDown();
                });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Timeout waiting for findByUsername", e);
        }

        return result[0];
    }

    @Override
    public User findByUsernameAndPassword(String username, String password) {

        return findByUsername(username);
    }

    @Override
    public User findByPlayerId(int playerId) {
        CountDownLatch latch = new CountDownLatch(1);
        final User[] result = new User[1];

        db.collection(COLLECTION_NAME)
                .whereEqualTo("playerId", playerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        result[0] = documentToUser(document);
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding user by playerId", e);
                    latch.countDown();
                });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Timeout waiting for findByPlayerId", e);
        }

        return result[0];
    }

    @Override
    public List<User> findAll() {
        CountDownLatch latch = new CountDownLatch(1);
        final List<User> result = new ArrayList<>();

        db.collection(COLLECTION_NAME)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "findAll - Total documents: " + querySnapshot.size());
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        User user = documentToUser(document);
                        if (user != null) {
                            result.add(user);
                            Log.d(TAG, "findAll - User: " + user.getUsername() + ", Role: " + user.getRole() + ", Approved: " + user.isApproved());
                        }
                    }
                    latch.countDown();
                    Log.d(TAG, "Found " + result.size() + " users");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting users", e);
                    latch.countDown();
                });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Timeout waiting for findAll", e);
        }

        return result;
    }


    public User findByEmail(String email) {
        CountDownLatch latch = new CountDownLatch(1);
        final User[] result = new User[1];

        db.collection(COLLECTION_NAME)
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        result[0] = documentToUser(document);
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding user by email", e);
                    latch.countDown();
                });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Timeout waiting for findByEmail", e);
        }

        return result[0];
    }


    public User getCurrentUser() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser != null) {
            return findByEmail(firebaseUser.getEmail());
        }
        return null;
    }

    private User documentToUser(DocumentSnapshot document) {
        try {
            User user = new User();
            user.setId(document.getId().hashCode());
            user.setUsername(document.getString("username"));
            
            String email = document.getString("email");
            user.setEmail(email != null && !email.isEmpty() ? email : null);
            
            String roleStr = document.getString("role");
            if (roleStr != null) {
                user.setRole(Role.valueOf(roleStr));
            }
            
            Long clubId = document.getLong("clubId");
            if (clubId != null && clubId != 0) {
                user.setClubId(clubId.intValue());
            }
            
            Long playerId = document.getLong("playerId");
            if (playerId != null && playerId > 0) {
                user.setPlayerId(playerId.intValue());
            }
            
            Long managerId = document.getLong("managerId");
            if (managerId != null && managerId > 0) {
                user.setManagerId(managerId.intValue());
            }
            
            Long age = document.getLong("age");
            if (age != null && age > 0) {
                user.setAge(age.intValue());
            }
            
            Boolean approved = document.getBoolean("approved");
            user.setApproved(approved != null ? approved : false);
            
            // Get preferred position for players
            String positionStr = document.getString("preferredPosition");
            if (positionStr != null) {
                try {
                    user.setPreferredPosition(Position.valueOf(positionStr));
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Invalid position value: " + positionStr);
                }
            }
            
            Log.d(TAG, "documentToUser - User: " + user.getUsername() + 
                  ", Email: " + user.getEmail() + 
                  ", Age: " + user.getAge() + 
                  ", ClubId: " + user.getClubId() + 
                  ", Approved: " + user.isApproved());
            
            return user;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to user", e);
            return null;
        }

    }

    
    @Override
    public boolean delete(Integer userId) {

        if (userId == null) {
            return false;

        }
        
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = new boolean[]{false};
        

        db.collection(COLLECTION_NAME)
                .whereEqualTo("id", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String docId = querySnapshot.getDocuments().get(0).getId();
                        db.collection(COLLECTION_NAME)
                                .document(docId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    result[0] = true;
                                    latch.countDown();
                                    Log.d(TAG, "User deleted successfully: " + userId);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error deleting user document", e);
                                    latch.countDown();
                                });
                    } else {
                        Log.e(TAG, "User not found with id: " + userId);
                        latch.countDown();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding user to delete", e);
                    latch.countDown();
                });
        
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Timeout waiting for delete", e);
        }
        
        return result[0];
    }
    

    public boolean deleteByUsername(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = new boolean[]{false};
        
        db.collection(COLLECTION_NAME)
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String docId = querySnapshot.getDocuments().get(0).getId();
                        db.collection(COLLECTION_NAME)
                                .document(docId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    result[0] = true;
                                    latch.countDown();
                                    Log.d(TAG, "User deleted by username: " + username);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error deleting user", e);
                                    latch.countDown();
                                });
                    } else {
                        Log.e(TAG, "User not found with username: " + username);
                        latch.countDown();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding user to delete", e);
                    latch.countDown();
                });

        
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Timeout waiting for delete", e);
        }
        
        return result[0];
    }
}
