package com.example.coachesapp_android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coachesapp_android.database.IClubRepository;
import com.example.coachesapp_android.database.IPlayerRepository;
import com.example.coachesapp_android.database.IUserRepository;
import com.example.coachesapp_android.model.Club;
import com.example.coachesapp_android.model.Player;
import com.example.coachesapp_android.model.Role;
import com.example.coachesapp_android.model.User;
import com.example.coachesapp_android.util.RepositoryFactory;

import java.util.ArrayList;
import java.util.List;

public class ClubDetailsActivity extends AppCompatActivity {
    private static final String TAG = "ClubDetails";
    
    private TextView clubNameText;
    private RecyclerView managersRecyclerView;
    private RecyclerView playersRecyclerView;
    private Button backButton;
    
    private IClubRepository clubRepository;
    private IUserRepository userRepository;
    private IPlayerRepository playerRepository;
    
    private Club club;
    private List<User> managers;
    private List<Player> players;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_club_details);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Club Details");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        clubRepository = RepositoryFactory.getClubRepository();
        userRepository = RepositoryFactory.getFirebaseUserRepository();
        playerRepository = RepositoryFactory.getPlayerRepository();
        
        int clubId = getIntent().getIntExtra("CLUB_ID", -1);
        
        initializeViews();
        loadClubDetails(clubId);
    }
    
    private void initializeViews() {
        clubNameText = findViewById(R.id.clubNameText);
        managersRecyclerView = findViewById(R.id.managersRecyclerView);
        playersRecyclerView = findViewById(R.id.playersRecyclerView);
        backButton = findViewById(R.id.backButton);
        
        managersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        playersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        managers = new ArrayList<>();
        players = new ArrayList<>();
        
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
    }
    
    private void loadClubDetails(int clubId) {
        new Thread(() -> {
            club = clubRepository.findById(clubId);
            
            if (club != null) {
                // Load managers
                List<User> allUsers = userRepository.findAll();
                managers = new ArrayList<>();
                for (User user : allUsers) {
                    if ((user.getRole() == Role.CLUB_MANAGER || user.getRole() == Role.CLUB_OWNER) 
                        && user.getClubId() != null && user.getClubId().equals(clubId)) {
                        managers.add(user);
                    }
                }
                
                // Load players
                players = playerRepository.findByClubId(clubId);
                
                // Filter approved players only
                List<Player> approvedPlayers = new ArrayList<>();
                for (Player player : players) {
                    User user = userRepository.findByPlayerId(player.getId());
                    if (user != null && user.isApproved()) {
                        approvedPlayers.add(player);
                    }
                }
                players = approvedPlayers;
                
                runOnUiThread(() -> {
                    displayClubDetails();
                });
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Club not found", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }
    
    private void displayClubDetails() {
        if (club != null) {
            clubNameText.setText(club.getClubName());
        }
        
        // Setup managers adapter
        ManagerAdapter managerAdapter = new ManagerAdapter(managers);
        managersRecyclerView.setAdapter(managerAdapter);
        
        // Setup players adapter
        PlayerAdapter playerAdapter = new PlayerAdapter(players);
        playersRecyclerView.setAdapter(playerAdapter);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
    
    // Manager Adapter
    private class ManagerAdapter extends RecyclerView.Adapter<ManagerAdapter.ViewHolder> {
        private List<User> managers;
        
        public ManagerAdapter(List<User> managers) {
            this.managers = managers != null ? managers : new ArrayList<>();
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_simple, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(managers.get(position));
        }
        
        @Override
        public int getItemCount() {
            return managers.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameText;
            TextView roleText;
            TextView emailText;
            
            ViewHolder(View view) {
                super(view);
                nameText = view.findViewById(R.id.userName);
                roleText = view.findViewById(R.id.userRole);
                emailText = view.findViewById(R.id.userEmail);
            }
            
            void bind(User user) {
                nameText.setText(user.getUsername());
                roleText.setText(user.getRole().getDisplayName());
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    emailText.setText(user.getEmail());
                } else {
                    emailText.setText("No email");
                }
            }
        }
    }
    
    // Player Adapter
    private class PlayerAdapter extends RecyclerView.Adapter<PlayerAdapter.ViewHolder> {
        private List<Player> players;
        
        public PlayerAdapter(List<Player> players) {
            this.players = players != null ? players : new ArrayList<>();
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
            holder.bind(players.get(position));
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
                playerName.setText(player.getName());
                if (player.getPosition() != null) {
                    playerPosition.setText(player.getPosition().getDisplayName());
                }
                playerAge.setText("Age: " + player.getAge());
                playerJersey.setText("#" + player.getJersey());
            }
        }
    }
}
