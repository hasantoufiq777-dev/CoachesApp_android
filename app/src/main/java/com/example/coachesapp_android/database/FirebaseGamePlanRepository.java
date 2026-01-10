package com.example.coachesapp_android.database;

import android.util.Log;

import com.example.coachesapp_android.model.GamePlan;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FirebaseGamePlanRepository implements IGamePlanRepository {
    private static final String TAG = "FirebaseGamePlanRepo";
    private static final String COLLECTION_NAME = "gamePlans";
    private final FirebaseFirestore db;

    public FirebaseGamePlanRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public GamePlan save(GamePlan gamePlan) {
        CountDownLatch latch = new CountDownLatch(1);
        final GamePlan[] result = new GamePlan[1];
        
        Log.d(TAG, "Saving game plan for club " + gamePlan.getClubId());
        Log.d(TAG, "Data - GK: " + gamePlan.getGoalkeeperName() + " (ID:" + gamePlan.getGoalkeeperPlayerId() + ")");
        Log.d(TAG, "Data - DEF1: " + gamePlan.getDefender1Name() + " (ID:" + gamePlan.getDefender1PlayerId() + ")");
        Log.d(TAG, "Data - DEF2: " + gamePlan.getDefender2Name() + " (ID:" + gamePlan.getDefender2PlayerId() + ")");
        Log.d(TAG, "Data - MID1: " + gamePlan.getMidfielder1Name() + " (ID:" + gamePlan.getMidfielder1PlayerId() + ")");
        Log.d(TAG, "Data - MID2: " + gamePlan.getMidfielder2Name() + " (ID:" + gamePlan.getMidfielder2PlayerId() + ")");
        Log.d(TAG, "Data - ATT: " + gamePlan.getAttackerName() + " (ID:" + gamePlan.getAttackerPlayerId() + ")");
        
        Map<String, Object> data = new HashMap<>();
        data.put("clubId", gamePlan.getClubId());
        data.put("goalkeeperPlayerId", gamePlan.getGoalkeeperPlayerId());
        data.put("goalkeeperName", gamePlan.getGoalkeeperName());
        data.put("defender1PlayerId", gamePlan.getDefender1PlayerId());
        data.put("defender1Name", gamePlan.getDefender1Name());
        data.put("defender2PlayerId", gamePlan.getDefender2PlayerId());
        data.put("defender2Name", gamePlan.getDefender2Name());
        data.put("midfielder1PlayerId", gamePlan.getMidfielder1PlayerId());
        data.put("midfielder1Name", gamePlan.getMidfielder1Name());
        data.put("midfielder2PlayerId", gamePlan.getMidfielder2PlayerId());
        data.put("midfielder2Name", gamePlan.getMidfielder2Name());
        data.put("attackerPlayerId", gamePlan.getAttackerPlayerId());
        data.put("attackerName", gamePlan.getAttackerName());

        // Check if game plan exists for this club
        db.collection(COLLECTION_NAME)
            .whereEqualTo("clubId", gamePlan.getClubId())
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (!querySnapshot.isEmpty()) {
                    // Update existing game plan
                    String docId = querySnapshot.getDocuments().get(0).getId();
                    db.collection(COLLECTION_NAME)
                        .document(docId)
                        .update(data)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Game plan updated for club " + gamePlan.getClubId());
                            result[0] = gamePlan;
                            latch.countDown();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to update game plan", e);
                            latch.countDown();
                        });
                } else {
                    // Create new game plan
                    db.collection(COLLECTION_NAME)
                        .add(data)
                        .addOnSuccessListener(documentReference -> {
                            Log.d(TAG, "Game plan created for club " + gamePlan.getClubId());
                            result[0] = gamePlan;
                            latch.countDown();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to create game plan", e);
                            latch.countDown();
                        });
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to query game plan", e);
                latch.countDown();
            });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Timeout waiting for save", e);
        }

        return result[0];
    }

    @Override
    public GamePlan findByClubId(int clubId) {
        CountDownLatch latch = new CountDownLatch(1);
        final GamePlan[] result = new GamePlan[1];

        db.collection(COLLECTION_NAME)
            .whereEqualTo("clubId", clubId)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                Log.d(TAG, "Query result - Documents found: " + querySnapshot.size());
                if (!querySnapshot.isEmpty()) {
                    DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                    result[0] = documentToGamePlan(doc);
                    Log.d(TAG, "Found game plan for club " + clubId);
                    if (result[0] != null) {
                        Log.d(TAG, "Loaded - GK: " + result[0].getGoalkeeperName());
                        Log.d(TAG, "Loaded - DEF1: " + result[0].getDefender1Name());
                        Log.d(TAG, "Loaded - DEF2: " + result[0].getDefender2Name());
                    }
                } else {
                    Log.d(TAG, "No game plan found for club " + clubId);
                }
                latch.countDown();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to find game plan", e);
                latch.countDown();
            });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Timeout waiting for find", e);
        }

        return result[0];
    }

    @Override
    public boolean update(GamePlan gamePlan) {
        GamePlan updated = save(gamePlan);
        return updated != null;
    }

    @Override
    public boolean delete(int clubId) {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        db.collection(COLLECTION_NAME)
            .whereEqualTo("clubId", clubId)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (!querySnapshot.isEmpty()) {
                    String docId = querySnapshot.getDocuments().get(0).getId();
                    db.collection(COLLECTION_NAME)
                        .document(docId)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Game plan deleted for club " + clubId);
                            result[0] = true;
                            latch.countDown();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to delete game plan", e);
                            latch.countDown();
                        });
                } else {
                    latch.countDown();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to query game plan for deletion", e);
                latch.countDown();
            });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Timeout waiting for delete", e);
        }

        return result[0];
    }

    private GamePlan documentToGamePlan(DocumentSnapshot doc) {
        GamePlan gamePlan = new GamePlan();
        
        gamePlan.setClubId(doc.getLong("clubId").intValue());
        
        if (doc.getLong("goalkeeperPlayerId") != null) {
            gamePlan.setGoalkeeperPlayerId(doc.getLong("goalkeeperPlayerId").intValue());
        }
        gamePlan.setGoalkeeperName(doc.getString("goalkeeperName"));
        
        if (doc.getLong("defender1PlayerId") != null) {
            gamePlan.setDefender1PlayerId(doc.getLong("defender1PlayerId").intValue());
        }
        gamePlan.setDefender1Name(doc.getString("defender1Name"));
        
        if (doc.getLong("defender2PlayerId") != null) {
            gamePlan.setDefender2PlayerId(doc.getLong("defender2PlayerId").intValue());
        }
        gamePlan.setDefender2Name(doc.getString("defender2Name"));
        
        if (doc.getLong("midfielder1PlayerId") != null) {
            gamePlan.setMidfielder1PlayerId(doc.getLong("midfielder1PlayerId").intValue());
        }
        gamePlan.setMidfielder1Name(doc.getString("midfielder1Name"));
        
        if (doc.getLong("midfielder2PlayerId") != null) {
            gamePlan.setMidfielder2PlayerId(doc.getLong("midfielder2PlayerId").intValue());
        }
        gamePlan.setMidfielder2Name(doc.getString("midfielder2Name"));
        
        if (doc.getLong("attackerPlayerId") != null) {
            gamePlan.setAttackerPlayerId(doc.getLong("attackerPlayerId").intValue());
        }
        gamePlan.setAttackerName(doc.getString("attackerName"));
        
        return gamePlan;
    }
}
