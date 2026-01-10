package com.example.coachesapp_android;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coachesapp_android.database.IClubRepository;
import com.example.coachesapp_android.database.IPlayerRepository;
import com.example.coachesapp_android.database.IUserRepository;
import com.example.coachesapp_android.model.Club;
import com.example.coachesapp_android.model.Player;
import com.example.coachesapp_android.model.Position;
import com.example.coachesapp_android.model.Role;
import com.example.coachesapp_android.model.User;
import com.example.coachesapp_android.util.AppState;
import com.example.coachesapp_android.util.RepositoryFactory;

import java.util.ArrayList;
import java.util.List;

public class PlayerListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private Button addPlayerButton;
    private Button backButton;
    private Spinner sortPositionSpinner;
    private Spinner sortClubSpinner;
    IPlayerRepository playerRepository;
    private IClubRepository clubRepository;
    private IUserRepository userRepository;
    private PlayerAdapter adapter;
    private List<Player> players;
    private List<Player> allPlayers;
    private List<Club> clubs;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_list);
        
        playerRepository = RepositoryFactory.getPlayerRepository();
        clubRepository = RepositoryFactory.getClubRepository();
        userRepository = RepositoryFactory.getFirebaseUserRepository();
        
        initializeViews();
        loadPlayers();
        setupListeners();
    }
    
    private void initializeViews() {
        recyclerView = findViewById(R.id.playersRecyclerView);
        addPlayerButton = findViewById(R.id.addPlayerButton);
        backButton = findViewById(R.id.backButton);
        sortPositionSpinner = findViewById(R.id.sortPositionSpinner);
        sortClubSpinner = findViewById(R.id.sortClubSpinner);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        players = new ArrayList<>();
        allPlayers = new ArrayList<>();
        clubs = new ArrayList<>();
        
        setupSpinners();
        
        // Hide add button for players
        if (AppState.getInstance().currentUser != null && 
            AppState.getInstance().currentUser.getRole() == Role.PLAYER) {
            addPlayerButton.setVisibility(View.GONE);
        }
    }
    
    private void loadPlayers() {
        // Load players in background thread
        new Thread(() -> {
            List<Player> loadedPlayers = new ArrayList<>();
            
            if (AppState.getInstance().currentUser != null) {
                Role role = AppState.getInstance().currentUser.getRole();
                Integer currentUserClubId = AppState.getInstance().currentUser.getClubId();
                
                android.util.Log.d("PlayerListActivity", "========== LOADING PLAYERS ==========");
                android.util.Log.d("PlayerListActivity", "User: " + AppState.getInstance().currentUser.getUsername());
                android.util.Log.d("PlayerListActivity", "Role: " + role);
                android.util.Log.d("PlayerListActivity", "User ClubId: " + currentUserClubId);
                
                if (role == Role.SYSTEM_ADMIN || role == Role.CLUB_OWNER) {
                    loadedPlayers = playerRepository.findAll();
                    android.util.Log.d("PlayerListActivity", "Admin/Owner loaded " + loadedPlayers.size() + " total players");
                } else if (role == Role.CLUB_MANAGER) {
                    if (currentUserClubId != null && currentUserClubId != 0) {
                        android.util.Log.d("PlayerListActivity", "Searching for players with clubId: " + currentUserClubId);
                        loadedPlayers = playerRepository.findByClubId(currentUserClubId);
                        android.util.Log.d("PlayerListActivity", "Manager loaded " + loadedPlayers.size() + " players for club " + currentUserClubId);
                        
                        // Log each player found
                        for (Player p : loadedPlayers) {
                            android.util.Log.d("PlayerListActivity", "  - Found player: " + p.getName() + ", PlayerClubId: " + p.getClubId());
                        }
                    } else {
                        android.util.Log.e("PlayerListActivity", "Manager has no clubId assigned! ClubId is: " + currentUserClubId);
                    }
                }
                
                // Filter to show only approved players
                List<Player> approvedPlayers = new ArrayList<>();
                for (Player player : loadedPlayers) {
                    android.util.Log.d("PlayerListActivity", "Checking player: " + player.getName() + ", ID: " + player.getId() + ", ClubId: " + player.getClubId());
                    User user = userRepository.findByPlayerId(player.getId());
                    if (user != null) {
                        android.util.Log.d("PlayerListActivity", "Found user for player: " + user.getUsername() + ", Approved: " + user.isApproved());
                        if (user.isApproved()) {
                            approvedPlayers.add(player);
                        }
                    } else {
                        android.util.Log.w("PlayerListActivity", "No user found for player ID: " + player.getId());
                    }
                }
                android.util.Log.d("PlayerListActivity", "After filtering: " + approvedPlayers.size() + " approved players");
                loadedPlayers = approvedPlayers;
            }
            
            // Add club names to players
            for (Player player : loadedPlayers) {
                if (player.getClubId() != null) {
                    Club club = clubRepository.findById(player.getClubId());
                    if (club != null) {
                        player.setClubView(club.getClubName());
                        android.util.Log.d("PlayerListActivity", "Set club name: " + club.getClubName() + " for player: " + player.getName());
                    } else {
                        android.util.Log.w("PlayerListActivity", "Club not found for ID: " + player.getClubId());
                    }
                }
            }
            
            List<Player> finalPlayers = loadedPlayers;
            runOnUiThread(() -> {
                players.clear();
                players.addAll(finalPlayers);
                allPlayers.clear();
                allPlayers.addAll(finalPlayers);
                adapter = new PlayerAdapter(players);
                recyclerView.setAdapter(adapter);
                android.util.Log.d("PlayerListActivity", "Displayed " + finalPlayers.size() + " players in RecyclerView");
            });
        }).start();
        
        // Load clubs for filtering
        new Thread(() -> {
            clubs = clubRepository.findAll();
            runOnUiThread(() -> updateClubSpinner());
        }).start();
    }
    
    private void setupSpinners() {
        // Position spinner
        List<String> positions = new ArrayList<>();
        positions.add("All Positions");
        for (Position pos : Position.values()) {
            positions.add(pos.getDisplayName());
        }
        
        ArrayAdapter<String> positionAdapter = new ArrayAdapter<>(
            this, R.layout.spinner_item, positions);
        positionAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        sortPositionSpinner.setAdapter(positionAdapter);
        
        sortPositionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterPlayers();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        // Club spinner
        sortClubSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterPlayers();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    
    private void updateClubSpinner() {
        List<String> clubNames = new ArrayList<>();
        clubNames.add("All Clubs");
        for (Club club : clubs) {
            clubNames.add(club.getClubName());
        }
        
        ArrayAdapter<String> clubAdapter = new ArrayAdapter<>(
            this, R.layout.spinner_item, clubNames);
        clubAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        sortClubSpinner.setAdapter(clubAdapter);
    }
    
    private void filterPlayers() {
        if (allPlayers.isEmpty()) return;
        
        String selectedPosition = sortPositionSpinner.getSelectedItem().toString();
        String selectedClub = sortClubSpinner.getSelectedItem() != null ? 
                             sortClubSpinner.getSelectedItem().toString() : "All Clubs";
        
        List<Player> filtered = new ArrayList<>();
        
        for (Player player : allPlayers) {
            boolean matchesPosition = selectedPosition.equals("All Positions") || 
                                    player.getPosition().getDisplayName().equals(selectedPosition);
            boolean matchesClub = selectedClub.equals("All Clubs") || 
                                (player.getClubView() != null && player.getClubView().equals(selectedClub));
            
            if (matchesPosition && matchesClub) {
                filtered.add(player);
            }
        }
        
        players.clear();
        players.addAll(filtered);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
    
    private void setupListeners() {
        addPlayerButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddPlayerActivity.class);
            startActivityForResult(intent, 100);
        });
        
        backButton.setOnClickListener(v -> finish());
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            loadPlayers();
        }
    }
    
    private class PlayerAdapter extends RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder> {
        private List<Player> players;
        
        public PlayerAdapter(List<Player> players) {
            this.players = players;
        }
        
        @NonNull
        @Override
        public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_player, parent, false);
            return new PlayerViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
            Player player = players.get(position);
            holder.bind(player);
        }
        
        @Override
        public int getItemCount() {
            return players.size();
        }
        
        class PlayerViewHolder extends RecyclerView.ViewHolder {
            TextView playerName;
            TextView playerAge;
            TextView playerPosition;
            TextView playerClub;
            TextView playerEmail;
            Button viewButton;
            Button editButton;
            Button deleteButton;
            
            public PlayerViewHolder(@NonNull View itemView) {
                super(itemView);
                playerName = itemView.findViewById(R.id.playerName);
                playerAge = itemView.findViewById(R.id.playerAge);
                playerPosition = itemView.findViewById(R.id.playerPosition);
                playerClub = itemView.findViewById(R.id.playerClub);
                playerEmail = itemView.findViewById(R.id.playerEmail);
                viewButton = itemView.findViewById(R.id.viewButton);
                editButton = itemView.findViewById(R.id.editButton);
                deleteButton = itemView.findViewById(R.id.deleteButton);
            }
            
            public void bind(Player player) {
                playerName.setText(player.getName());
                playerAge.setText("Age: " + player.getAge());
                playerPosition.setText(player.getPosition().getDisplayName());
                
                String clubText = player.getClubView() != null ? player.getClubView() : "Free Agent";
                if (player.isInjured()) {
                    clubText += " • 🚑 Injured";
                }
                playerClub.setText(clubText);
                
                // Show jersey number
                playerEmail.setText("Jersey #" + player.getJersey());
                
                viewButton.setOnClickListener(v -> {
                    AppState.getInstance().setSelectedPlayer(player);
                    Intent intent = new Intent(PlayerListActivity.this, PlayerProfileActivity.class);
                    startActivity(intent);
                });
                
                editButton.setOnClickListener(v -> {
                    Intent intent = new Intent(PlayerListActivity.this, AddPlayerActivity.class);
                    intent.putExtra("player_id", player.getId());
                    startActivityForResult(intent, 100);
                });
                
                deleteButton.setOnClickListener(v -> {
                    new AlertDialog.Builder(PlayerListActivity.this)
                        .setTitle("Delete Player")
                        .setMessage("Are you sure you want to delete " + player.getName() + "?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            // Delete in background thread
                            new Thread(() -> {
                                boolean success = playerRepository.delete(player.getId());
                                runOnUiThread(() -> {
                                    if (success) {
                                        int position = getAdapterPosition();
                                        if (position != RecyclerView.NO_POSITION) {
                                            players.remove(position);
                                            notifyItemRemoved(position);
                                        }
                                        Toast.makeText(PlayerListActivity.this, "Player deleted", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(PlayerListActivity.this, "Failed to delete player", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }).start();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                });
                
                // Hide edit/delete for players viewing their own profile
                if (AppState.getInstance().currentUser != null && 
                    AppState.getInstance().currentUser.getRole() == Role.PLAYER) {
                    editButton.setVisibility(View.GONE);
                    deleteButton.setVisibility(View.GONE);
                }
            }
        }
    }
}
