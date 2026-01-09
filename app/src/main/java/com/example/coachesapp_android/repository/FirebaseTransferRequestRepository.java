package com.example.coachesapp_android.repository;

import android.util.Log;
import com.example.coachesapp_android.database.IClubRepository;
import com.example.coachesapp_android.database.IPlayerRepository;
import com.example.coachesapp_android.model.Club;
import com.example.coachesapp_android.model.Player;
import com.example.coachesapp_android.model.TransferRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class FirebaseTransferRequestRepository implements ITransferRequestRepository {
    private static final String TAG = "TransferRequestRepo";
    private static final String COLLECTION_NAME = "transferRequests";
    
    private final FirebaseFirestore db;
    private final IPlayerRepository playerRepository;
    private final IClubRepository clubRepository;
    
    private Integer nextId = 1;

    public FirebaseTransferRequestRepository(IPlayerRepository playerRepository, IClubRepository clubRepository) {
        this.db = FirebaseFirestore.getInstance();
        this.playerRepository = playerRepository;
        this.clubRepository = clubRepository;
        initializeNextId();
    }

    private void initializeNextId() {
        db.collection(COLLECTION_NAME)
                .orderBy("id", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        Long maxId = doc.getLong("id");
                        if (maxId != null) {
                            nextId = maxId.intValue() + 1;
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error initializing next ID", e));
    }

    private synchronized Integer getNextId() {
        return nextId++;
    }

    @Override
    public TransferRequest save(TransferRequest transferRequest) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TransferRequest> result = new AtomicReference<>(null);

        try {
            if (transferRequest.getId() == null) {
                transferRequest.setId(getNextId());
            }

            // Enrich with player and club names
            enrichTransferRequest(transferRequest);

            Map<String, Object> data = transferRequestToMap(transferRequest);
            String docId = "transfer_" + transferRequest.getId();

            Log.d(TAG, "Saving transfer request: ID=" + transferRequest.getId() + 
                    ", PlayerId=" + transferRequest.getPlayerId() + 
                    ", SourceClubId=" + transferRequest.getSourceClubId() + 
                    ", DestClubId=" + transferRequest.getDestinationClubId() + 
                    ", Status=" + transferRequest.getStatus());

            db.collection(COLLECTION_NAME)
                    .document(docId)
                    .set(data)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Transfer request saved successfully: " + transferRequest.getId());
                        result.set(transferRequest);
                        latch.countDown();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error saving transfer request", e);
                        latch.countDown();
                    });

            latch.await();
        } catch (Exception e) {
            Log.e(TAG, "Exception in save", e);
        }

        return result.get();
    }

    @Override
    public TransferRequest findById(Integer id) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TransferRequest> result = new AtomicReference<>(null);

        String docId = "transfer_" + id;
        db.collection(COLLECTION_NAME)
                .document(docId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        result.set(documentToTransferRequest(documentSnapshot));
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding transfer request", e);
                    latch.countDown();
                });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while waiting", e);
        }

        return result.get();
    }

    @Override
    public List<TransferRequest> findAll() {
        CountDownLatch latch = new CountDownLatch(1);
        List<TransferRequest> result = new ArrayList<>();

        db.collection(COLLECTION_NAME)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot) {
                        TransferRequest tr = documentToTransferRequest(doc);
                        if (tr != null) {
                            result.add(tr);
                        }
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding all transfer requests", e);
                    latch.countDown();
                });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while waiting", e);
        }

        return result;
    }

    @Override
    public List<TransferRequest> findByPlayerId(Integer playerId) {
        return findByField("playerId", playerId);
    }

    @Override
    public List<TransferRequest> findBySourceClubId(Integer clubId) {
        return findByField("sourceClubId", clubId);
    }

    @Override
    public List<TransferRequest> findByDestinationClubId(Integer clubId) {
        return findByField("destinationClubId", clubId);
    }

    @Override
    public List<TransferRequest> findByStatus(TransferRequest.TransferStatus status) {
        return findByField("status", status.name());
    }

    @Override
    public List<TransferRequest> findInMarket() {
        Log.d(TAG, "🔍 QUERY START - Finding transfers with status IN_MARKET");
        List<TransferRequest> results = findByStatus(TransferRequest.TransferStatus.IN_MARKET);
        Log.d(TAG, "🔍 QUERY RESULT - Found " + results.size() + " transfers in market");
        
        // Log details of each found transfer
        for (int i = 0; i < results.size(); i++) {
            TransferRequest tr = results.get(i);
            Log.d(TAG, "  " + (i+1) + ". Player=" + tr.getPlayerName() + 
                    ", Type=" + tr.getTransferType() + 
                    ", SourceClub=" + tr.getSourceClubId() + 
                    ", DestClub=" + tr.getDestinationClubId() +
                    ", Fee=$" + tr.getReleaseFee());
        }
        
        return results;
    }

    private List<TransferRequest> findByField(String fieldName, Object value) {
        CountDownLatch latch = new CountDownLatch(1);
        List<TransferRequest> result = new ArrayList<>();

        Log.d(TAG, "Querying " + fieldName + " = " + value + " (type: " + (value != null ? value.getClass().getSimpleName() : "null") + ")");

        db.collection(COLLECTION_NAME)
                .whereEqualTo(fieldName, value)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Found " + querySnapshot.size() + " documents for " + fieldName + " = " + value);
                    for (DocumentSnapshot doc : querySnapshot) {
                        // Log raw data for debugging
                        Object rawValue = doc.get(fieldName);
                        Log.d(TAG, "  - Document " + doc.getId() + ": " + fieldName + " = " + rawValue + 
                                " (type: " + (rawValue != null ? rawValue.getClass().getSimpleName() : "null") + ")");
                        
                        TransferRequest tr = documentToTransferRequest(doc);
                        if (tr != null) {
                            Log.d(TAG, "  - Transfer: Player=" + tr.getPlayerName() + ", SourceClub=" + tr.getSourceClubId() + ", Status=" + tr.getStatus());
                            result.add(tr);
                        }
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding by " + fieldName, e);
                    latch.countDown();
                });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while waiting", e);
        }

        return result;
    }

    @Override
    public boolean update(TransferRequest transferRequest) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> result = new AtomicReference<>(false);

        try {
            Log.d(TAG, "⚙ UPDATE START - ID=" + transferRequest.getId() + 
                    ", Status=" + transferRequest.getStatus() + 
                    ", ReleaseFee=" + transferRequest.getReleaseFee());
            
            enrichTransferRequest(transferRequest);
            Map<String, Object> data = transferRequestToMap(transferRequest);
            String docId = "transfer_" + transferRequest.getId();
            
            // Log the exact data being saved to Firebase
            Log.d(TAG, "⚙ Firebase document: " + docId);
            Log.d(TAG, "⚙ Data map contains: status=" + data.get("status") + 
                    ", releaseFee=" + data.get("releaseFee") + 
                    ", transferType=" + data.get("transferType"));

            db.collection(COLLECTION_NAME)
                    .document(docId)
                    .set(data)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✓ UPDATE SUCCESS - Transfer " + transferRequest.getId() + 
                                " saved with status=" + data.get("status"));
                        result.set(true);
                        latch.countDown();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "✗ UPDATE FAILED - Transfer " + transferRequest.getId(), e);
                        latch.countDown();
                    });

            latch.await();
        } catch (Exception e) {
            Log.e(TAG, "✗ EXCEPTION in update", e);
        }

        boolean success = result.get();
        Log.d(TAG, "⚙ UPDATE RESULT: " + (success ? "SUCCESS" : "FAILED"));
        return success;
    }

    @Override
    public boolean delete(Integer id) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> result = new AtomicReference<>(false);

        String docId = "transfer_" + id;
        db.collection(COLLECTION_NAME)
                .document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Transfer request deleted: " + id);
                    result.set(true);
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting transfer request", e);
                    latch.countDown();
                });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while waiting", e);
        }

        return result.get();
    }

    private void enrichTransferRequest(TransferRequest transferRequest) {
        Log.d(TAG, "Enriching transfer request - ID: " + transferRequest.getId() + ", PlayerId: " + transferRequest.getPlayerId());
        
        // Get player name
        if (transferRequest.getPlayerId() != null) {
            Player player = playerRepository.findById(transferRequest.getPlayerId());
            if (player != null) {
                transferRequest.setPlayerName(player.getName());
                Log.d(TAG, "Enriched with player name: " + player.getName());
            } else {
                Log.w(TAG, "Player not found for ID: " + transferRequest.getPlayerId());
            }
        } else {
            Log.w(TAG, "PlayerId is NULL in transfer request!");
        }

        // Get source club name
        if (transferRequest.getSourceClubId() != null) {
            Club sourceClub = clubRepository.findById(transferRequest.getSourceClubId());
            if (sourceClub != null) {
                transferRequest.setSourceClubName(sourceClub.getClubName());
                Log.d(TAG, "Enriched with source club name: " + sourceClub.getClubName());
            } else {
                Log.w(TAG, "Source club not found for ID: " + transferRequest.getSourceClubId());
            }
        }

        // Get destination club name
        if (transferRequest.getDestinationClubId() != null) {
            Club destClub = clubRepository.findById(transferRequest.getDestinationClubId());
            if (destClub != null) {
                transferRequest.setDestinationClubName(destClub.getClubName());
                Log.d(TAG, "Enriched with destination club name: " + destClub.getClubName());
            }
        }
    }

    private Map<String, Object> transferRequestToMap(TransferRequest tr) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", tr.getId());
        map.put("playerId", tr.getPlayerId());
        map.put("sourceClubId", tr.getSourceClubId());
        map.put("destinationClubId", tr.getDestinationClubId());
        map.put("status", tr.getStatus() != null ? tr.getStatus().name() : null);
        map.put("transferType", tr.getTransferType() != null ? tr.getTransferType().name() : null);
        map.put("transferFee", tr.getTransferFee());
        map.put("releaseFee", tr.getReleaseFee());
        map.put("remarks", tr.getRemarks());
        
        // Store player and club names for easier querying
        map.put("playerName", tr.getPlayerName());
        map.put("sourceClubName", tr.getSourceClubName());
        map.put("destinationClubName", tr.getDestinationClubName());

        // Convert LocalDateTime to Firestore Timestamp
        if (tr.getRequestDate() != null) {
            map.put("requestDate", dateToTimestamp(tr.getRequestDate()));
        }
        if (tr.getApprovedBySourceDate() != null) {
            map.put("approvedBySourceDate", dateToTimestamp(tr.getApprovedBySourceDate()));
        }
        if (tr.getCompletedDate() != null) {
            map.put("completedDate", dateToTimestamp(tr.getCompletedDate()));
        }

        return map;
    }

    private TransferRequest documentToTransferRequest(DocumentSnapshot doc) {
        try {
            TransferRequest tr = new TransferRequest();
            tr.setId(doc.getLong("id") != null ? doc.getLong("id").intValue() : null);
            tr.setPlayerId(doc.getLong("playerId") != null ? doc.getLong("playerId").intValue() : null);
            tr.setSourceClubId(doc.getLong("sourceClubId") != null ? doc.getLong("sourceClubId").intValue() : null);
            tr.setDestinationClubId(doc.getLong("destinationClubId") != null ? doc.getLong("destinationClubId").intValue() : null);
            tr.setTransferFee(doc.getDouble("transferFee"));
            tr.setReleaseFee(doc.getDouble("releaseFee"));
            tr.setRemarks(doc.getString("remarks"));
            
            tr.setPlayerName(doc.getString("playerName"));
            tr.setSourceClubName(doc.getString("sourceClubName"));
            tr.setDestinationClubName(doc.getString("destinationClubName"));

            String statusStr = doc.getString("status");
            if (statusStr != null) {
                tr.setStatus(TransferRequest.TransferStatus.valueOf(statusStr));
            }

            String transferTypeStr = doc.getString("transferType");
            if (transferTypeStr != null) {
                try {
                    tr.setTransferType(TransferRequest.TransferType.valueOf(transferTypeStr));
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Invalid transferType: " + transferTypeStr);
                }
            }

            tr.setRequestDate(timestampToDate(doc.getDate("requestDate")));
            tr.setApprovedBySourceDate(timestampToDate(doc.getDate("approvedBySourceDate")));
            tr.setCompletedDate(timestampToDate(doc.getDate("completedDate")));

            return tr;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to TransferRequest", e);
            return null;
        }
    }

    private Date dateToTimestamp(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private LocalDateTime timestampToDate(Date date) {
        if (date == null) return null;
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
