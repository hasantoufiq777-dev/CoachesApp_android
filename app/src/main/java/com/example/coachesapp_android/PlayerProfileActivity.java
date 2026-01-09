package com.example.coachesapp_android;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.coachesapp_android.database.IClubRepository;
import com.example.coachesapp_android.database.IPlayerRepository;
import com.example.coachesapp_android.model.Club;
import com.example.coachesapp_android.model.Player;
import com.example.coachesapp_android.model.User;
import com.example.coachesapp_android.util.AppState;
import com.example.coachesapp_android.util.RepositoryFactory;

public class PlayerProfileActivity extends AppCompatActivity {
    private static final String TAG = "PlayerProfile";

    private TextView playerNameText;
    private TextView playerEmailText;
    private TextView playerAgeText;
    private TextView playerClubText;
    private TextView playerJerseyText;
    private TextView playerPositionText;
    private TextView playerStatusText;
    private Button toggleInjuryButton;
    private Button backButton;
    
    private IPlayerRepository playerRepository;
    private IClubRepository clubRepository;
    private Player player;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_profile);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Player Profile");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        playerRepository = RepositoryFactory.getPlayerRepository();
        clubRepository = RepositoryFactory.getClubRepository();
        currentUser = AppState.getInstance().currentUser;
        
        if (currentUser == null) {
            Toast.makeText(this, "Error: No user logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initializeViews();
        loadPlayerData();
    }
    
    private void initializeViews() {
        playerNameText = findViewById(R.id.playerNameText);
        playerEmailText = findViewById(R.id.playerEmailText);
        playerAgeText = findViewById(R.id.playerAgeText);
        playerClubText = findViewById(R.id.playerClubText);
        playerJerseyText = findViewById(R.id.playerJerseyText);
        playerPositionText = findViewById(R.id.playerPositionText);
        playerStatusText = findViewById(R.id.playerStatusText);
        toggleInjuryButton = findViewById(R.id.toggleInjuryButton);
        backButton = findViewById(R.id.backButton);
        
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
    }
    
    private void loadPlayerData() {
        // Load player data in background thread
        new Thread(() -> {
            try {
                // First check if viewing a selected player (from admin/manager)
                Player selectedPlayer = AppState.getInstance().getSelectedPlayer();
                if (selectedPlayer != null) {
                    player = selectedPlayer;
                    Log.d(TAG, "Loading selected player: " + player.getName());
                } else {
                    // Loading own profile - get player by playerId from current user
                    if (currentUser.getPlayerId() != null && currentUser.getPlayerId() > 0) {
                        player = playerRepository.findById(currentUser.getPlayerId());
                        Log.d(TAG, "Found player by ID: " + currentUser.getPlayerId());
                    }
                    
                    // If no player found, try finding by user's club
                    if (player == null && currentUser.getClubId() != null) {
                        Log.d(TAG, "Player not found by ID, searching by club and username");
                        java.util.List<Player> clubPlayers = playerRepository.findByClubId(currentUser.getClubId());
                        for (Player p : clubPlayers) {
                            if (p.getName() != null && p.getName().equals(currentUser.getUsername())) {
                                player = p;
                                // Update user with playerId
                                currentUser.setPlayerId(p.getId());
                                Log.d(TAG, "Found player by name match: " + p.getName());
                                break;
                            }
                        }
                    }
                }
                
                runOnUiThread(() -> {
                    if (player != null) {
                        displayPlayerInfo();
                        setupListeners();
                    } else {
                        Toast.makeText(this, "Player profile not found. Please contact admin.", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "No player found for user: " + currentUser.getUsername() + ", playerId: " + currentUser.getPlayerId() + ", clubId: " + currentUser.getClubId());
                        finish();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading player data", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }
    
    private void displayPlayerInfo() {
        try {
            if (playerNameText != null) {
                playerNameText.setText("Name: " + (player.getName() != null ? player.getName() : "Unknown"));
            }
            
            // Load email from the player's user account (not current logged-in user)
            if (playerEmailText != null) {
                playerEmailText.setText("Email: Loading...");
                new Thread(() -> {
                    try {
                        com.example.coachesapp_android.database.IUserRepository userRepository = 
                            com.example.coachesapp_android.util.RepositoryFactory.getFirebaseUserRepository();
                        User playerUser = userRepository.findByPlayerId(player.getId());
                        runOnUiThread(() -> {
                            if (playerUser != null && playerUser.getEmail() != null && !playerUser.getEmail().isEmpty()) {
                                playerEmailText.setText("Email: " + playerUser.getEmail());
                            } else {
                                playerEmailText.setText("Email: Not provided");
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading player email", e);
                        runOnUiThread(() -> playerEmailText.setText("Email: Not available"));
                    }
                }).start();
            }
            
            if (playerAgeText != null) {
                playerAgeText.setText("Age: " + player.getAge());
            }
            
            // Load club info
            if (player.getClubId() != null && player.getClubId() != 0) {
                if (playerClubText != null) {
                    playerClubText.setText("Club: Loading...");
                }
                new Thread(() -> {
                    Club club = clubRepository.findById(player.getClubId());
                    runOnUiThread(() -> {
                        if (playerClubText != null) {
                            if (club != null) {
                                playerClubText.setText("Club: " + club.getClubName());
                            } else {
                                playerClubText.setText("Club: Not found");
                            }
                        }
                    });
                }).start();
            } else {
                if (playerClubText != null) {
                    playerClubText.setText("Club: Free Agent");
                }
            }
            
            if (playerJerseyText != null) {
                playerJerseyText.setText("Jersey: #" + player.getJersey());
            }
            
            if (playerPositionText != null && player.getPosition() != null) {
                playerPositionText.setText("Position: " + player.getPosition().getDisplayName());
            }
            
            if (playerStatusText != null) {
                playerStatusText.setText("Status: " + (player.isInjured() ? "🚑 Injured" : "✓ Healthy"));
            }
            
            if (toggleInjuryButton != null) {
                toggleInjuryButton.setText(player.isInjured() ? "Mark as Healthy" : "Mark as Injured");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error displaying player info", e);
        }
    }
    
    private void setupListeners() {
        if (toggleInjuryButton != null) {
            toggleInjuryButton.setOnClickListener(v -> {
                player.setInjured(!player.isInjured());
                
                // Update in background thread
                new Thread(() -> {
                    boolean success = playerRepository.update(player);
                    runOnUiThread(() -> {
                        if (success) {
                            displayPlayerInfo();
                            Toast.makeText(this, "Player status updated", Toast.LENGTH_SHORT).show();
                        } else {
                            // Revert change if update failed
                            player.setInjured(!player.isInjured());
                            Toast.makeText(this, "Failed to update player status", Toast.LENGTH_SHORT).show();
                        }
                    });
                }).start();
            });
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
