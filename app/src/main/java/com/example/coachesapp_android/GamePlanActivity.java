package com.example.coachesapp_android;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.coachesapp_android.database.IGamePlanRepository;
import com.example.coachesapp_android.database.IPlayerRepository;
import com.example.coachesapp_android.model.GamePlan;
import com.example.coachesapp_android.model.Player;
import com.example.coachesapp_android.model.Position;
import com.example.coachesapp_android.util.AppState;
import com.example.coachesapp_android.util.RepositoryFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GamePlanActivity extends AppCompatActivity {
    private static final String TAG = "GamePlanActivity";
    
    private TextView clubNameText;
    private LinearLayout goalkeeperSlot, defender1Slot, defender2Slot;
    private LinearLayout midfielder1Slot, midfielder2Slot, attackerSlot;
    private TextView goalkeeperName, goalkeeperJersey;
    private TextView defender1Name, defender1Jersey, defender2Name, defender2Jersey;
    private TextView midfielder1Name, midfielder1Jersey, midfielder2Name, midfielder2Jersey;
    private TextView attackerName, attackerJersey;
    private Button editLineupButton, saveLineupButton, backButton;
    
    private IPlayerRepository playerRepository;
    private IGamePlanRepository gamePlanRepository;
    private GamePlan currentGamePlan;
    private List<Player> clubPlayers;
    private Integer currentClubId;
    
    private static final int SLOT_GOALKEEPER = 0;
    private static final int SLOT_DEFENDER_1 = 1;
    private static final int SLOT_DEFENDER_2 = 2;
    private static final int SLOT_MIDFIELDER_1 = 3;
    private static final int SLOT_MIDFIELDER_2 = 4;
    private static final int SLOT_ATTACKER = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_plan);
        
        playerRepository = RepositoryFactory.getPlayerRepository();
        gamePlanRepository = RepositoryFactory.getGamePlanRepository();
        currentGamePlan = new GamePlan();
        clubPlayers = new ArrayList<>();
        
        if (AppState.getInstance().currentUser != null) {
            currentClubId = AppState.getInstance().currentUser.getClubId();
        }
        
        initializeViews();
        loadClubPlayers();
        setupListeners();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh player list when returning to this activity
        loadClubPlayers();
    }
    
    private void initializeViews() {
        clubNameText = findViewById(R.id.clubNameText);
        
        goalkeeperSlot = findViewById(R.id.goalkeeperSlot);
        goalkeeperName = findViewById(R.id.goalkeeperName);
        goalkeeperJersey = findViewById(R.id.goalkeeperJersey);
        
        defender1Slot = findViewById(R.id.defender1Slot);
        defender1Name = findViewById(R.id.defender1Name);
        defender1Jersey = findViewById(R.id.defender1Jersey);
        
        defender2Slot = findViewById(R.id.defender2Slot);
        defender2Name = findViewById(R.id.defender2Name);
        defender2Jersey = findViewById(R.id.defender2Jersey);
        
        midfielder1Slot = findViewById(R.id.midfielder1Slot);
        midfielder1Name = findViewById(R.id.midfielder1Name);
        midfielder1Jersey = findViewById(R.id.midfielder1Jersey);
        
        midfielder2Slot = findViewById(R.id.midfielder2Slot);
        midfielder2Name = findViewById(R.id.midfielder2Name);
        midfielder2Jersey = findViewById(R.id.midfielder2Jersey);
        
        attackerSlot = findViewById(R.id.attackerSlot);
        attackerName = findViewById(R.id.attackerName);
        attackerJersey = findViewById(R.id.attackerJersey);
        
        editLineupButton = findViewById(R.id.editLineupButton);
        saveLineupButton = findViewById(R.id.saveLineupButton);
        backButton = findViewById(R.id.backButton);
    }
    
    private void loadClubPlayers() {
        if (currentClubId == null) {
            Toast.makeText(this, "No club assigned", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new Thread(() -> {
            List<Player> allPlayers = playerRepository.findByClubId(currentClubId);
            
            // Deduplicate players by ID (keep only the latest version)
            Map<Integer, Player> playerMap = new HashMap<>();
            for (Player p : allPlayers) {
                if (p.getId() != null) {
                    // If player already exists, keep the one with higher jersey or newer data
                    if (!playerMap.containsKey(p.getId())) {
                        playerMap.put(p.getId(), p);
                    }
                }
            }
            
            clubPlayers = new ArrayList<>(playerMap.values());
            Log.d(TAG, "Loaded " + clubPlayers.size() + " unique players for club " + currentClubId);
            
            runOnUiThread(() -> {
                if (clubPlayers.isEmpty()) {
                    Toast.makeText(this, "No players in club. Add players first.", Toast.LENGTH_LONG).show();
                } else {
                    // Now that players are loaded, load the game plan
                    loadGamePlan();
                }
            });
        }).start();
    }
    
    private void loadGamePlan() {
        if (currentClubId == null) {
            Log.w(TAG, "Cannot load game plan: currentClubId is null");
            return;
        }
        
        Log.d(TAG, "Loading game plan for club " + currentClubId);
        
        new Thread(() -> {
            GamePlan savedGamePlan = gamePlanRepository.findByClubId(currentClubId);
            
            Log.d(TAG, "Game plan query completed. Result: " + (savedGamePlan != null ? "found" : "not found"));
            
            if (savedGamePlan != null) {
                Log.d(TAG, "Found saved game plan - GK: " + savedGamePlan.getGoalkeeperName() + 
                          ", DEF1: " + savedGamePlan.getDefender1Name() + 
                          ", DEF2: " + savedGamePlan.getDefender2Name());
                
                runOnUiThread(() -> {
                    currentGamePlan = savedGamePlan;
                    populateUIFromGamePlan();
                    Toast.makeText(this, "Game plan loaded", Toast.LENGTH_SHORT).show();
                });
            } else {
                Log.d(TAG, "No saved game plan found for club " + currentClubId);
            }
        }).start();
    }
    
    private void populateUIFromGamePlan() {
        Log.d(TAG, "Populating UI from game plan. ClubPlayers size: " + clubPlayers.size());
        
        // Populate goalkeeper
        if (currentGamePlan.getGoalkeeperPlayerId() != null) {
            goalkeeperName.setText(currentGamePlan.getGoalkeeperName());
            Player gk = findPlayerById(currentGamePlan.getGoalkeeperPlayerId());
            if (gk != null) {
                goalkeeperJersey.setText("#" + gk.getJersey());
                Log.d(TAG, "Goalkeeper: " + gk.getName() + " #" + gk.getJersey());
            }
        }
        
        // Populate defenders
        if (currentGamePlan.getDefender1PlayerId() != null) {
            defender1Name.setText(currentGamePlan.getDefender1Name());
            Player def1 = findPlayerById(currentGamePlan.getDefender1PlayerId());
            if (def1 != null) {
                defender1Jersey.setText("#" + def1.getJersey());
                Log.d(TAG, "Defender1: " + def1.getName() + " #" + def1.getJersey());
            }
        }
        
        if (currentGamePlan.getDefender2PlayerId() != null) {
            defender2Name.setText(currentGamePlan.getDefender2Name());
            Player def2 = findPlayerById(currentGamePlan.getDefender2PlayerId());
            if (def2 != null) {
                defender2Jersey.setText("#" + def2.getJersey());
                Log.d(TAG, "Defender2: " + def2.getName() + " #" + def2.getJersey());
            }
        }
        
        // Populate midfielders
        if (currentGamePlan.getMidfielder1PlayerId() != null) {
            midfielder1Name.setText(currentGamePlan.getMidfielder1Name());
            Player mid1 = findPlayerById(currentGamePlan.getMidfielder1PlayerId());
            if (mid1 != null) {
                midfielder1Jersey.setText("#" + mid1.getJersey());
                Log.d(TAG, "Midfielder1: " + mid1.getName() + " #" + mid1.getJersey());
            }
        }
        
        if (currentGamePlan.getMidfielder2PlayerId() != null) {
            midfielder2Name.setText(currentGamePlan.getMidfielder2Name());
            Player mid2 = findPlayerById(currentGamePlan.getMidfielder2PlayerId());
            if (mid2 != null) {
                midfielder2Jersey.setText("#" + mid2.getJersey());
                Log.d(TAG, "Midfielder2: " + mid2.getName() + " #" + mid2.getJersey());
            }
        }
        
        // Populate attacker
        if (currentGamePlan.getAttackerPlayerId() != null) {
            attackerName.setText(currentGamePlan.getAttackerName());
            Player att = findPlayerById(currentGamePlan.getAttackerPlayerId());
            if (att != null) {
                attackerJersey.setText("#" + att.getJersey());
                Log.d(TAG, "Attacker: " + att.getName() + " #" + att.getJersey());
            }
        }
    }
    
    private Player findPlayerById(Integer playerId) {
        if (playerId == null) return null;
        for (Player p : clubPlayers) {
            if (p.getId().equals(playerId)) {
                return p;
            }
        }
        return null;
    }
    
    private void setupListeners() {
        goalkeeperSlot.setOnClickListener(v -> showPlayerSelection(SLOT_GOALKEEPER, Position.GOALKEEPER));
        defender1Slot.setOnClickListener(v -> showPlayerSelection(SLOT_DEFENDER_1, Position.DEFENDER));
        defender2Slot.setOnClickListener(v -> showPlayerSelection(SLOT_DEFENDER_2, Position.DEFENDER));
        midfielder1Slot.setOnClickListener(v -> showPlayerSelection(SLOT_MIDFIELDER_1, Position.MIDFIELDER));
        midfielder2Slot.setOnClickListener(v -> showPlayerSelection(SLOT_MIDFIELDER_2, Position.MIDFIELDER));
        attackerSlot.setOnClickListener(v -> showPlayerSelection(SLOT_ATTACKER, Position.FORWARD));
        
        editLineupButton.setOnClickListener(v -> {
            Toast.makeText(this, "Tap on any position to select a player", Toast.LENGTH_LONG).show();
        });
        
        saveLineupButton.setOnClickListener(v -> saveGamePlan());
        backButton.setOnClickListener(v -> finish());
    }
    
    private boolean isPlayerAlreadyAssigned(Integer playerId, int currentSlot) {
        if (playerId == null) return false;
        
        if (currentSlot != SLOT_GOALKEEPER && playerId.equals(currentGamePlan.getGoalkeeperPlayerId())) return true;
        if (currentSlot != SLOT_DEFENDER_1 && playerId.equals(currentGamePlan.getDefender1PlayerId())) return true;
        if (currentSlot != SLOT_DEFENDER_2 && playerId.equals(currentGamePlan.getDefender2PlayerId())) return true;
        if (currentSlot != SLOT_MIDFIELDER_1 && playerId.equals(currentGamePlan.getMidfielder1PlayerId())) return true;
        if (currentSlot != SLOT_MIDFIELDER_2 && playerId.equals(currentGamePlan.getMidfielder2PlayerId())) return true;
        if (currentSlot != SLOT_ATTACKER && playerId.equals(currentGamePlan.getAttackerPlayerId())) return true;
        
        return false;
    }
    
    private void showPlayerSelection(int slot, Position preferredPosition) {
        if (clubPlayers.isEmpty()) {
            Toast.makeText(this, "No players available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Filter players by preferred position and exclude already assigned
        List<Player> availablePlayers = new ArrayList<>();
        for (Player p : clubPlayers) {
            if (p.getPosition() == preferredPosition && !p.isInjured() && !isPlayerAlreadyAssigned(p.getId(), slot)) {
                availablePlayers.add(p);
            }
        }
        
        // If no players with exact position, show all healthy players not yet assigned
        if (availablePlayers.isEmpty()) {
            for (Player p : clubPlayers) {
                if (!p.isInjured() && !isPlayerAlreadyAssigned(p.getId(), slot)) {
                    availablePlayers.add(p);
                }
            }
        }
        
        if (availablePlayers.isEmpty()) {
            Toast.makeText(this, "No healthy players available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create player names array for dialog
        String[] playerNames = new String[availablePlayers.size() + 1];
        playerNames[0] = "Remove Player";
        for (int i = 0; i < availablePlayers.size(); i++) {
            Player p = availablePlayers.get(i);
            playerNames[i + 1] = p.getName() + " (#" + p.getJersey() + ") - " + p.getPosition().getDisplayName();
        }
        
        new AlertDialog.Builder(this)
            .setTitle("Select Player")
            .setItems(playerNames, (dialog, which) -> {
                if (which == 0) {
                    // Remove player
                    assignPlayerToSlot(slot, null);
                } else {
                    // Assign selected player
                    Player selectedPlayer = availablePlayers.get(which - 1);
                    assignPlayerToSlot(slot, selectedPlayer);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void assignPlayerToSlot(int slot, Player player) {
        switch (slot) {
            case SLOT_GOALKEEPER:
                if (player != null) {
                    currentGamePlan.setGoalkeeperPlayerId(player.getId());
                    currentGamePlan.setGoalkeeperName(player.getName());
                    goalkeeperName.setText(player.getName());
                    goalkeeperJersey.setText("#" + player.getJersey());
                } else {
                    currentGamePlan.setGoalkeeperPlayerId(null);
                    goalkeeperName.setText("GK");
                    goalkeeperJersey.setText("--");
                }
                break;
                
            case SLOT_DEFENDER_1:
                if (player != null) {
                    currentGamePlan.setDefender1PlayerId(player.getId());
                    currentGamePlan.setDefender1Name(player.getName());
                    defender1Name.setText(player.getName());
                    defender1Jersey.setText("#" + player.getJersey());
                } else {
                    currentGamePlan.setDefender1PlayerId(null);
                    defender1Name.setText("DEF");
                    defender1Jersey.setText("--");
                }
                break;
                
            case SLOT_DEFENDER_2:
                if (player != null) {
                    currentGamePlan.setDefender2PlayerId(player.getId());
                    currentGamePlan.setDefender2Name(player.getName());
                    defender2Name.setText(player.getName());
                    defender2Jersey.setText("#" + player.getJersey());
                } else {
                    currentGamePlan.setDefender2PlayerId(null);
                    defender2Name.setText("DEF");
                    defender2Jersey.setText("--");
                }
                break;
                
            case SLOT_MIDFIELDER_1:
                if (player != null) {
                    currentGamePlan.setMidfielder1PlayerId(player.getId());
                    currentGamePlan.setMidfielder1Name(player.getName());
                    midfielder1Name.setText(player.getName());
                    midfielder1Jersey.setText("#" + player.getJersey());
                } else {
                    currentGamePlan.setMidfielder1PlayerId(null);
                    midfielder1Name.setText("MID");
                    midfielder1Jersey.setText("--");
                }
                break;
                
            case SLOT_MIDFIELDER_2:
                if (player != null) {
                    currentGamePlan.setMidfielder2PlayerId(player.getId());
                    currentGamePlan.setMidfielder2Name(player.getName());
                    midfielder2Name.setText(player.getName());
                    midfielder2Jersey.setText("#" + player.getJersey());
                } else {
                    currentGamePlan.setMidfielder2PlayerId(null);
                    midfielder2Name.setText("MID");
                    midfielder2Jersey.setText("--");
                }
                break;
                
            case SLOT_ATTACKER:
                if (player != null) {
                    currentGamePlan.setAttackerPlayerId(player.getId());
                    currentGamePlan.setAttackerName(player.getName());
                    attackerName.setText(player.getName());
                    attackerJersey.setText("#" + player.getJersey());
                } else {
                    currentGamePlan.setAttackerPlayerId(null);
                    attackerName.setText("FW");
                    attackerJersey.setText("--");
                }
                break;
        }
    }
    
    private void saveGamePlan() {
        // Validate all positions are filled
        if (currentGamePlan.getGoalkeeperPlayerId() == null ||
            currentGamePlan.getDefender1PlayerId() == null ||
            currentGamePlan.getDefender2PlayerId() == null ||
            currentGamePlan.getMidfielder1PlayerId() == null ||
            currentGamePlan.getMidfielder2PlayerId() == null ||
            currentGamePlan.getAttackerPlayerId() == null) {
            
            Toast.makeText(this, "Please fill all 6 positions before saving", Toast.LENGTH_LONG).show();
            return;
        }
        
        currentGamePlan.setClubId(currentClubId);
        
        // Save to Firebase in background thread
        new Thread(() -> {
            GamePlan saved = gamePlanRepository.save(currentGamePlan);
            
            runOnUiThread(() -> {
                if (saved != null) {
                    Toast.makeText(this, "Game Plan saved successfully!\n" +
                            "GK: " + currentGamePlan.getGoalkeeperName() + "\n" +
                            "DEF: " + currentGamePlan.getDefender1Name() + ", " + currentGamePlan.getDefender2Name() + "\n" +
                            "MID: " + currentGamePlan.getMidfielder1Name() + ", " + currentGamePlan.getMidfielder2Name() + "\n" +
                            "FW: " + currentGamePlan.getAttackerName(),
                            Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Game Plan saved for club " + currentClubId);
                } else {
                    Toast.makeText(this, "Failed to save game plan. Please try again.", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to save game plan for club " + currentClubId);
                }
            });
        }).start();
    }
}
