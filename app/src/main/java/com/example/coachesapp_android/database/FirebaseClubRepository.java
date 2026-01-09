package com.example.coachesapp_android.database;

import android.util.Log;

import com.example.coachesapp_android.model.Club;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Firebase Firestore implementation of IClubRepository.
 */
public class FirebaseClubRepository implements IClubRepository {
    private static final String TAG = "FirebaseClubRepo";
    private static final String COLLECTION_NAME = "clubs";
    private final FirebaseFirestore db;

    public FirebaseClubRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public Club save(Club club) {
        CountDownLatch latch = new CountDownLatch(1);
        final Club[] result = new Club[1];
        
        Map<String, Object> clubData = new HashMap<>();
        clubData.put("name", club.getClubName());

        db.collection(COLLECTION_NAME)
                .add(clubData)
                .addOnSuccessListener(documentReference -> {
                    String docId = documentReference.getId();
                    int positiveId = Math.abs(docId.hashCode()); // Ensure positive ID
                    club.setId(positiveId); // For backward compatibility
                    club.setFirestoreDocId(docId); // Store actual Firestore doc ID
                    
                    // Update the document to store both IDs
                    clubData.put("firestoreDocId", docId);
                    clubData.put("id", positiveId);
                    documentReference.set(clubData);
                    
                    result[0] = club;
                    latch.countDown();
                    Log.d(TAG, "Club added: " + club.getClubName() + " with docId: " + docId + ", numericId: " + club.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding club", e);
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
    public Club findById(int id) {
        CountDownLatch latch = new CountDownLatch(1);
        final Club[] result = new Club[1];

        Log.d(TAG, "Finding club by id: " + id);
        
        // Search by the numeric id field, not document ID
        db.collection(COLLECTION_NAME)
                .whereEqualTo("id", id)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        result[0] = documentToClub(document);
                        Log.d(TAG, "Found club: " + (result[0] != null ? result[0].getClubName() : "null"));
                    } else {
                        Log.e(TAG, "No club found with id: " + id);
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding club by id: " + id, e);
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
    public List<Club> findAll() {
        CountDownLatch latch = new CountDownLatch(1);
        final List<Club> result = new ArrayList<>();

        db.collection(COLLECTION_NAME)
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        Club club = documentToClub(document);
                        if (club != null) {
                            result.add(club);
                        }
                    }
                    latch.countDown();
                    Log.d(TAG, "Found " + result.size() + " clubs");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting clubs", e);
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
    public Club findByName(String name) {
        CountDownLatch latch = new CountDownLatch(1);
        final Club[] result = new Club[1];

        db.collection(COLLECTION_NAME)
                .whereEqualTo("name", name)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        result[0] = documentToClub(document);
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding club by name", e);
                    latch.countDown();
                });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Timeout waiting for findByName", e);
        }

        return result[0];
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
                    Log.d(TAG, "Club deleted: " + id);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting club", e);
                    latch.countDown();
                });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Timeout waiting for delete", e);
        }

        return result[0];
    }

    private Club documentToClub(DocumentSnapshot document) {
        try {
            Club club = new Club();
            
            // Read the numeric ID from the document field
            Long idLong = document.getLong("id");
            if (idLong != null) {
                club.setId(idLong.intValue());
            } else {
                // Fallback to hashCode if id field doesn't exist (old data)
                club.setId(document.getId().hashCode());
            }
            
            // Store the actual Firestore document ID
            club.setFirestoreDocId(document.getId());
            club.setClubName(document.getString("name"));
            
            Log.d(TAG, "Converted club: " + club.getClubName() + ", ID: " + club.getId() + ", DocId: " + club.getFirestoreDocId());
            return club;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to club", e);
            return null;
        }
    }
}
