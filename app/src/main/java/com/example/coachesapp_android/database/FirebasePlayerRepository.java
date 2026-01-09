package com.example.coachesapp_android.database;

import android.util.Log;

import com.example.coachesapp_android.model.Player;
import com.example.coachesapp_android.model.Position;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class FirebasePlayerRepository implements IPlayerRepository {
    private static final String TAG = "FirebasePlayerRepo";
    private static final String COLLECTION_NAME = "players";
    private final FirebaseFirestore db;

    public FirebasePlayerRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public Player save(Player player) {
        CountDownLatch latch = new CountDownLatch(1);
        final Player[] result = new Player[1];
        
        Map<String, Object> playerData = new HashMap<>();
        playerData.put("name", player.getName());
        playerData.put("age", player.getAge());
        playerData.put("jersey", player.getJersey());
        playerData.put("position", player.getPosition().name());
        playerData.put("injured", player.isInjured());
        playerData.put("clubId", player.getClubId());

        if (player.getId() != null) {

            playerData.put("id", player.getId());
            
            db.collection(COLLECTION_NAME)
                    .whereEqualTo("id", player.getId())
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            // Update the first matching document
                            String docId = querySnapshot.getDocuments().get(0).getId();
                            db.collection(COLLECTION_NAME)
                                    .document(docId)
                                    .set(playerData)
                                    .addOnSuccessListener(aVoid -> {
                                        result[0] = player;
                                        latch.countDown();
                                        Log.d(TAG, "Player updated: " + player.getName() + ", ID: " + player.getId() + ", DocId: " + docId + ", ClubId: " + player.getClubId());
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error updating player document", e);
                                        latch.countDown();
                                    });
                        } else {

                            Log.w(TAG, "No player found with ID " + player.getId() + ", creating new");
                            player.setId(null); // Reset ID to create new
                            result[0] = save(player); // Recursive call to create
                            latch.countDown();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error finding player to update", e);
                        latch.countDown();
                    });
        } else {

            db.collection(COLLECTION_NAME)
                    .add(playerData)
                    .addOnSuccessListener(documentReference -> {
                        String docId = documentReference.getId();
                        int playerId = docId.hashCode(); // Using hashCode for integer ID
                        player.setId(playerId);
                        

                        documentReference.update("id", playerId)
                                .addOnSuccessListener(aVoid -> {
                                    result[0] = player;
                                    latch.countDown();
                                    Log.d(TAG, "Player created: " + player.getName() + ", ID: " + playerId + ", DocId: " + docId + ", ClubId: " + player.getClubId());
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error updating player ID field", e);
                                    result[0] = player; // Still return player even if ID update fails
                                    latch.countDown();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error adding player", e);
                        latch.countDown();
                    });
        }

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Timeout waiting for save", e);
        }
        
        return result[0];
    }

    @Override
    public Player findById(int id) {
        CountDownLatch latch = new CountDownLatch(1);
        final Player[] result = new Player[1];

        db.collection(COLLECTION_NAME)
                .whereEqualTo("id", id)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        result[0] = documentToPlayer(document);
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding player", e);
                    latch.countDown();
                });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Timeout waiting for findById", e);
        }

        return result[0];
    }

    @Override
    public List<Player> findAll() {
        CountDownLatch latch = new CountDownLatch(1);
        final List<Player> result = new ArrayList<>();

        db.collection(COLLECTION_NAME)
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    java.util.Set<Integer> seenIds = new java.util.HashSet<>();
                    
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        Player player = documentToPlayer(document);
                        if (player != null && !seenIds.contains(player.getId())) {
                            result.add(player);
                            seenIds.add(player.getId());
                        } else if (player != null && seenIds.contains(player.getId())) {
                            Log.w(TAG, "Skipping duplicate player: " + player.getName() + ", ID: " + player.getId() + ", DocId: " + document.getId());
                        }
                    }
                    latch.countDown();
                    Log.d(TAG, "Found " + result.size() + " unique players (deduplicated)");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting players", e);
                    latch.countDown();
                });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Timeout waiting for findAll", e);
        }

        return result;
    }

    @Override
    public List<Player> findByClubId(int clubId) {
        CountDownLatch latch = new CountDownLatch(1);
        final List<Player> result = new ArrayList<>();

        Log.d(TAG, "========== FINDING PLAYERS BY CLUB ID ==========");
        Log.d(TAG, "Searching for players with clubId: " + clubId);
        
        db.collection(COLLECTION_NAME)
                .whereEqualTo("clubId", clubId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Query returned " + querySnapshot.size() + " documents");

                    java.util.Set<Integer> seenIds = new java.util.HashSet<>();
                    
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        Log.d(TAG, "Document ID: " + document.getId() + ", clubId field: " + document.getLong("clubId"));
                        Player player = documentToPlayer(document);
                        if (player != null && !seenIds.contains(player.getId())) {
                            result.add(player);
                            seenIds.add(player.getId());
                            Log.d(TAG, "Added player: " + player.getName() + " with clubId: " + player.getClubId());
                        } else if (player != null && seenIds.contains(player.getId())) {
                            Log.w(TAG, "Skipping duplicate player: " + player.getName() + ", ID: " + player.getId() + ", DocId: " + document.getId());
                        }
                    }
                    latch.countDown();
                    Log.d(TAG, "Total found " + result.size() + " unique players for club " + clubId + " (deduplicated)");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting players by club " + clubId, e);
                    latch.countDown();
                });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Timeout waiting for findByClubId", e);
        }

        return result;
    }

    @Override
    public boolean update(Player player) {
        if (player.getId() == null) {
            return false;
        }
        return save(player) != null;
    }

    @Override
    public boolean delete(int id) {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        db.collection(COLLECTION_NAME)
                .document(String.valueOf(id))
                .delete()
                .addOnSuccessListener(aVoid -> {
                    result[0] = true;
                    latch.countDown();
                    Log.d(TAG, "Player deleted: " + id);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting player", e);
                    latch.countDown();
                });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Timeout waiting for delete", e);
        }

        return result[0];
    }

    private Player documentToPlayer(DocumentSnapshot document) {
        try {
            Player player = new Player();


            Long idLong = document.getLong("id");
            if (idLong != null) {
                player.setId(idLong.intValue());
            } else {
                player.setId(document.getId().hashCode());
            }
            
            player.setName(document.getString("name"));
            
            Long age = document.getLong("age");
            player.setAge(age != null ? age.intValue() : 0);
            
            Long jersey = document.getLong("jersey");
            player.setJersey(jersey != null ? jersey.intValue() : 0);
            
            String positionStr = document.getString("position");
            if (positionStr != null) {
                player.setPosition(Position.valueOf(positionStr));
            }
            
            Boolean injured = document.getBoolean("injured");
            player.setInjured(injured != null ? injured : false);
            
            Long clubId = document.getLong("clubId");
            if (clubId != null) {
                player.setClubId(clubId.intValue());
            }
            
            Log.d(TAG, "Loaded player: " + player.getName() + ", ID: " + player.getId() + ", ClubId: " + player.getClubId());
            return player;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to player", e);
            return null;
        }
    }
}
