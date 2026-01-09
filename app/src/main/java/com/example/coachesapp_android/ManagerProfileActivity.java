package com.example.coachesapp_android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coachesapp_android.database.IClubRepository;
import com.example.coachesapp_android.database.IPlayerRepository;
import com.example.coachesapp_android.model.Club;
import com.example.coachesapp_android.model.Player;
import com.example.coachesapp_android.model.Position;
import com.example.coachesapp_android.model.User;
import com.example.coachesapp_android.util.AppState;
import com.example.coachesapp_android.util.RepositoryFactory;

import java.util.ArrayList;
import java.util.List;

public class ManagerProfileActivity extends AppCompatActivity {
    private TextView managerNameText;
    private TextView managerEmailText;
    private TextView managerClubText;
    private TextView managerAgeText;
    private Spinner positionFilterSpinner;
    private RecyclerView playersRecyclerView;
    private Button backButton;
    
    private IPlayerRepository playerRepository;
    private IClubRepository clubRepository;
    private com.example.coachesapp_android.database.IUserRepository userRepository;
    private User currentManager;
    private List<Player> clubPlayers;
    private List<Player> allClubPlayers;
    private PlayerAdapter playerAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_profile);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manager Profile");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        playerRepository = RepositoryFactory.getPlayerRepository();
        clubRepository = RepositoryFactory.getClubRepository();
        userRepository = RepositoryFactory.getFirebaseUserRepository();
        currentManager = AppState.getInstance().currentUser;
        
        if (currentManager == null) {
            android.widget.Toast.makeText(this, "Error: No manager logged in", android.widget.Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Reload user data from Firestore to ensure we have latest data
        reloadUserData();
        
        initializeViews();
        setupPositionFilter();
        loadClubPlayers();
    }
    
    private void reloadUserData() {
        // Reload user from Firestore to get latest data (email, age, etc.)
        new Thread(() -> {
            try {
                com.example.coachesapp_android.model.User updatedUser = null;
                
                // Try to find by username
                if (currentManager.getUsername() != null) {
                    updatedUser = userRepository.findByUsername(currentManager.getUsername());
                }
                
                // If not found or username is null, try by email
                if (updatedUser == null && currentManager.getEmail() != null) {
                    updatedUser = userRepository.findByEmail(currentManager.getEmail());
                }
                
                if (updatedUser != null) {
                    currentManager = updatedUser;
                    AppState.getInstance().currentUser = updatedUser;
                    android.util.Log.d("ManagerProfile", "User data reloaded: " + updatedUser.getUsername() + 
                        ", Email: " + updatedUser.getEmail() + 
                        ", Age: " + updatedUser.getAge());
                }
                
                runOnUiThread(() -> {
                    loadManagerInfo();
                });
            } catch (Exception e) {
                android.util.Log.e("ManagerProfile", "Error reloading user data", e);
                runOnUiThread(() -> {
                    loadManagerInfo();
                });
            }
        }).start();
    }
    
    private void initializeViews() {
        try {
            managerNameText = findViewById(R.id.managerNameText);
            managerEmailText = findViewById(R.id.managerEmailText);
            managerClubText = findViewById(R.id.managerClubText);
            managerAgeText = findViewById(R.id.managerAgeText);
            positionFilterSpinner = findViewById(R.id.positionFilterSpinner);
            playersRecyclerView = findViewById(R.id.clubPlayersRecyclerView);  // Fixed: match XML id
            backButton = findViewById(R.id.backButton);
            
            if (playersRecyclerView != null) {
                playersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            }
            clubPlayers = new ArrayList<>();
            allClubPlayers = new ArrayList<>();
            
            if (backButton != null) {
                backButton.setOnClickListener(v -> finish());
            }
        } catch (Exception e) {
            android.util.Log.e("ManagerProfile", "Error initializing views", e);
            android.widget.Toast.makeText(this, "Error loading profile", android.widget.Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void loadManagerInfo() {
        try {
            if (currentManager == null) {
                android.util.Log.e("ManagerProfile", "Current manager is null");
                return;
            }
            
            android.util.Log.d("ManagerProfile", "Loading info for: " + currentManager.getUsername() + 
                ", Email: " + currentManager.getEmail() + 
                ", Age: " + currentManager.getAge() + 
                ", ClubId: " + currentManager.getClubId());
            
            if (managerNameText != null) {
                managerNameText.setText("Name: " + (currentManager.getUsername() != null ? currentManager.getUsername() : "Unknown"));
            }
            
            if (managerEmailText != null) {
                String email = currentManager.getEmail();
                if (email != null && !email.isEmpty()) {
                    managerEmailText.setText("Email: " + email);
                } else {
                    managerEmailText.setText("Email: Not provided");
                }
            }
            
            if (managerAgeText != null) {
                if (currentManager.getAge() != null && currentManager.getAge() > 0) {
                    managerAgeText.setText("Age: " + currentManager.getAge());
                } else {
                    managerAgeText.setText("Age: Not specified");
                }
            }
            
            // Load club info
            android.util.Log.d("ManagerProfile", "Manager: " + currentManager.getUsername() + ", clubId: " + currentManager.getClubId());
            if (currentManager.getClubId() != null && currentManager.getClubId() != 0) {
                if (managerClubText != null) {
                    managerClubText.setText("Loading club...");
                }
                new Thread(() -> {
                    try {
                        Club club = clubRepository.findById(currentManager.getClubId());
                        runOnUiThread(() -> {
                            if (managerClubText != null) {
                                if (club != null) {
                                    managerClubText.setText("Club: " + club.getClubName());
                                    android.util.Log.d("ManagerProfile", "Club loaded: " + club.getClubName());
                                } else {
                                    managerClubText.setText("Club: Not found (ID: " + currentManager.getClubId() + ")");
                                    android.util.Log.e("ManagerProfile", "Club not found for ID: " + currentManager.getClubId());
                                }
                            }
                        });
                    } catch (Exception e) {
                        android.util.Log.e("ManagerProfile", "Error loading club", e);
                        runOnUiThread(() -> {
                            if (managerClubText != null) {
                                managerClubText.setText("Error loading club: " + e.getMessage());
                            }
                        });
                    }
                }).start();
            } else {
                if (managerClubText != null) {
                    managerClubText.setText("Club: Free Agent");
                }
                android.util.Log.d("ManagerProfile", "No club assigned to manager");
            }
        } catch (Exception e) {
            android.util.Log.e("ManagerProfile", "Error in loadManagerInfo", e);
            android.widget.Toast.makeText(this, "Error loading manager info: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
        }
    }
    
    private void setupPositionFilter() {
        List<String> positions = new ArrayList<>();
        positions.add("All Positions");
        for (Position pos : Position.values()) {
            positions.add(pos.getDisplayName());
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, positions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        positionFilterSpinner.setAdapter(adapter);
        
        positionFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterPlayersByPosition();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    
    private void loadClubPlayers() {
        if (currentManager == null || currentManager.getClubId() == null || currentManager.getClubId() == 0) {
            android.util.Log.d("ManagerProfile", "No club to load players for");
            return;
        }
        
        new Thread(() -> {
            try {
                android.util.Log.d("ManagerProfile", "Loading players for club ID: " + currentManager.getClubId());
                List<Player> players = playerRepository.findByClubId(currentManager.getClubId());
                android.util.Log.d("ManagerProfile", "Found " + players.size() + " players from repository");
                
                // Filter to show only approved players
                List<Player> approvedPlayers = new ArrayList<>();
                for (Player player : players) {
                    android.util.Log.d("ManagerProfile", "Checking player: " + player.getName() + ", ID: " + player.getId());
                    User user = userRepository.findByPlayerId(player.getId());
                    if (user != null) {
                        android.util.Log.d("ManagerProfile", "Found user: " + user.getUsername() + ", Approved: " + user.isApproved());
                        if (user.isApproved()) {
                            approvedPlayers.add(player);
                        }
                    } else {
                        android.util.Log.w("ManagerProfile", "No user found for player ID: " + player.getId());
                    }
                }
                android.util.Log.d("ManagerProfile", "After filtering: " + approvedPlayers.size() + " approved players");
                players = approvedPlayers;
                
                // Load club names for each player
                for (Player player : players) {
                    if (player.getClubId() != null) {
                        Club club = clubRepository.findById(player.getClubId());
                        if (club != null) {
                            player.setClubView(club.getClubName());
                        }
                    }
                }
                
                List<Player> finalPlayers = players;
                runOnUiThread(() -> {
                    try {
                        clubPlayers.clear();
                        clubPlayers.addAll(finalPlayers);
                        allClubPlayers.clear();
                        allClubPlayers.addAll(finalPlayers);
                        
                        if (playersRecyclerView != null) {
                            playerAdapter = new PlayerAdapter(clubPlayers);
                            playersRecyclerView.setAdapter(playerAdapter);
                        }
                        android.util.Log.d("ManagerProfile", "Displayed " + finalPlayers.size() + " players in RecyclerView");
                    } catch (Exception e) {
                        android.util.Log.e("ManagerProfile", "Error updating player list UI", e);
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("ManagerProfile", "Error loading club players", e);
                runOnUiThread(() -> {
                    android.widget.Toast.makeText(this, "Error loading players: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    private void filterPlayersByPosition() {
        if (allClubPlayers.isEmpty()) return;
        
        String selectedPosition = positionFilterSpinner.getSelectedItem().toString();
        
        List<Player> filtered = new ArrayList<>();
        for (Player player : allClubPlayers) {
            if (selectedPosition.equals("All Positions") || 
                player.getPosition().getDisplayName().equals(selectedPosition)) {
                filtered.add(player);
            }
        }
        
        clubPlayers.clear();
        clubPlayers.addAll(filtered);
        if (playerAdapter != null) {
            playerAdapter.notifyDataSetChanged();
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
    
    // Simple adapter to display players
    private class PlayerAdapter extends RecyclerView.Adapter<PlayerAdapter.ViewHolder> {
        private List<Player> players;
        
        public PlayerAdapter(List<Player> players) {
            this.players = players;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_player_simple, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Player player = players.get(position);
            holder.bind(player);
        }
        
        @Override
        public int getItemCount() {
            return players.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView playerName;
            TextView playerPosition;
            TextView playerAge;
            TextView playerJersey;
            
            ViewHolder(View view) {
                super(view);
                playerName = view.findViewById(R.id.playerName);
                playerPosition = view.findViewById(R.id.playerPosition);
                playerAge = view.findViewById(R.id.playerAge);
                playerJersey = view.findViewById(R.id.playerJersey);
            }
            
            void bind(Player player) {
                try {
                    if (player == null) {
                        android.util.Log.e("ManagerProfile", "Attempting to bind null player");
                        return;
                    }
                    
                    if (playerName != null) {
                        playerName.setText(player.getName() != null ? player.getName() : "Unknown");
                    }
                    if (playerPosition != null && player.getPosition() != null) {
                        playerPosition.setText(player.getPosition().getDisplayName());
                    }
                    if (playerAge != null) {
                        playerAge.setText("Age: " + player.getAge());
                    }
                    if (playerJersey != null) {
                        playerJersey.setText("#" + player.getJersey());
                    }
                } catch (Exception e) {
                    android.util.Log.e("ManagerProfile", "Error binding player", e);
                }
            }
        }
    }
}
