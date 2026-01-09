package com.example.coachesapp_android;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
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
import com.example.coachesapp_android.model.Manager;
import com.example.coachesapp_android.model.Player;
import com.example.coachesapp_android.model.Position;
import com.example.coachesapp_android.model.Role;
import com.example.coachesapp_android.model.User;
import com.example.coachesapp_android.util.RepositoryFactory;

import java.util.ArrayList;
import java.util.List;

public class PendingApprovalsActivity extends AppCompatActivity {
    private static final String TAG = "PendingApprovals";
    
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyText;
    private PendingUsersAdapter adapter;
    
    private IUserRepository userRepository;
    private IPlayerRepository playerRepository;
    private IClubRepository clubRepository;
    private List<User> pendingUsers;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_approvals);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Pending Approvals");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        userRepository = RepositoryFactory.getFirebaseUserRepository();
        playerRepository = RepositoryFactory.getPlayerRepository();
        clubRepository = RepositoryFactory.getClubRepository();
        pendingUsers = new ArrayList<>();
        
        initializeViews();
        loadPendingUsers();
    }
    
    private void initializeViews() {
        recyclerView = findViewById(R.id.pendingUsersRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyText = findViewById(R.id.emptyText);
        
        adapter = new PendingUsersAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
    
    private void loadPendingUsers() {
        progressBar.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);
        
        new Thread(() -> {
            try {
                List<User> allUsers = userRepository.findAll();
                
                if (allUsers == null) {
                    Log.e(TAG, "findAll() returned null");
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        emptyText.setVisibility(View.VISIBLE);
                        emptyText.setText("Error loading users");
                        Toast.makeText(this, "Error loading users", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                
                Log.d(TAG, "Total users loaded: " + allUsers.size());
                pendingUsers.clear();
                
                // Filter unapproved users
                for (User user : allUsers) {
                    if (user == null) {
                        Log.w(TAG, "Null user in list, skipping");
                        continue;
                    }
                    
                    Log.d(TAG, "User: " + user.getUsername() + ", Approved: " + user.isApproved() + ", Role: " + user.getRole());
                    if (!user.isApproved() && user.getRole() != Role.SYSTEM_ADMIN) {
                        pendingUsers.add(user);
                        Log.d(TAG, "Added to pending: " + user.getUsername());
                    }
                }
                
                Log.d(TAG, "Total pending users: " + pendingUsers.size());
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (pendingUsers.isEmpty()) {
                        emptyText.setVisibility(View.VISIBLE);
                        emptyText.setText("No pending approvals");
                        Log.d(TAG, "No pending users found");
                    } else {
                        emptyText.setVisibility(View.GONE);
                        adapter.notifyDataSetChanged();
                        Log.d(TAG, "Showing " + pendingUsers.size() + " pending users");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading pending users", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    emptyText.setVisibility(View.VISIBLE);
                    emptyText.setText("Error loading users");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    private void approveUser(User user, int position) {
        new AlertDialog.Builder(this)
            .setTitle("Approve User")
            .setMessage("Approve " + user.getUsername() + " as " + user.getRole().getDisplayName() + "?")
            .setPositiveButton("Approve", (dialog, which) -> {
                progressBar.setVisibility(View.VISIBLE);
                
                new Thread(() -> {
                    try {
                        boolean success = false;
                        
                        Log.d(TAG, "Starting approval for user: " + user.getUsername() + ", Role: " + user.getRole() + ", ClubId: " + user.getClubId());
                        
                        // Create Player or Manager profile based on role
                        if (user.getRole() == Role.PLAYER) {
                            // Create Player profile
                            Player player = new Player();
                            player.setName(user.getUsername());
                            player.setAge(user.getAge() != null ? user.getAge() : 18);
                            player.setJersey(0); // Will be assigned later
                            player.setPosition(Position.FORWARD); // Default position
                            player.setClubId(user.getClubId());
                            player.setInjured(false);
                            
                            Log.d(TAG, "Creating player with ClubId: " + player.getClubId());
                            Player savedPlayer = playerRepository.save(player);
                            if (savedPlayer != null) {
                                // Link player to user
                                user.setPlayerId(savedPlayer.getId());
                                user.setApproved(true);
                                // Ensure clubId is preserved
                                Log.d(TAG, "Before saving user - ClubId: " + user.getClubId());
                                User updatedUser = userRepository.save(user);
                                success = (updatedUser != null);
                                Log.d(TAG, "Player profile created: " + savedPlayer.getName() + ", ID: " + savedPlayer.getId() + ", ClubId: " + savedPlayer.getClubId());
                            }
                        } else if (user.getRole() == Role.CLUB_MANAGER || user.getRole() == Role.CLUB_OWNER) {
                            // For managers, just approve the user and preserve club assignment
                            Log.d(TAG, "Approving manager with ClubId: " + user.getClubId());
                            user.setManagerId(user.getId());
                            user.setApproved(true);
                            // Club ID should already be set from registration
                            User updatedUser = userRepository.save(user);
                            success = (updatedUser != null);
                            if (success) {
                                Log.d(TAG, "Manager approved: " + user.getUsername() + ", ClubId: " + updatedUser.getClubId());
                            } else {
                                Log.e(TAG, "Failed to save manager approval");
                            }
                        }
                        
                        final boolean finalSuccess = success;
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            if (finalSuccess) {
                                Toast.makeText(this, "User approved and profile created successfully", Toast.LENGTH_SHORT).show();
                                pendingUsers.remove(position);
                                adapter.notifyItemRemoved(position);
                                
                                if (pendingUsers.isEmpty()) {
                                    emptyText.setVisibility(View.VISIBLE);
                                }
                            } else {
                                Toast.makeText(this, "Failed to approve user", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error approving user", e);
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }).start();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void rejectUser(User user, int position) {
        new AlertDialog.Builder(this)
            .setTitle("Reject User")
            .setMessage("Reject registration for " + user.getUsername() + "? This will delete their account.")
            .setPositiveButton("Reject", (dialog, which) -> {
                progressBar.setVisibility(View.VISIBLE);
                
                new Thread(() -> {
                    try {
                        Log.d(TAG, "Attempting to reject user: " + user.getUsername() + ", ID: " + user.getId());
                        
                        // Use deleteByUsername for more reliable deletion
                        boolean deleted = ((com.example.coachesapp_android.database.FirebaseUserRepository) userRepository)
                                .deleteByUsername(user.getUsername());
                        
                        Log.d(TAG, "Deletion result: " + deleted);
                        
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            if (deleted) {
                                Toast.makeText(this, "User rejected and deleted", Toast.LENGTH_SHORT).show();
                                pendingUsers.remove(position);
                                adapter.notifyItemRemoved(position);
                                
                                if (pendingUsers.isEmpty()) {
                                    emptyText.setVisibility(View.VISIBLE);
                                }
                            } else {
                                Toast.makeText(this, "Failed to reject user. Check logs.", Toast.LENGTH_LONG).show();
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error rejecting user", e);
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                }).start();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
    
    private class PendingUsersAdapter extends RecyclerView.Adapter<PendingUsersAdapter.ViewHolder> {
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pending_user, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User user = pendingUsers.get(position);
            holder.bind(user, position);
        }
        
        @Override
        public int getItemCount() {
            return pendingUsers.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView usernameText;
            TextView emailText;
            TextView roleText;
            TextView ageText;
            TextView clubText;
            Button approveButton;
            Button rejectButton;
            
            ViewHolder(View view) {
                super(view);
                usernameText = view.findViewById(R.id.usernameText);
                emailText = view.findViewById(R.id.emailText);
                roleText = view.findViewById(R.id.roleText);
                ageText = view.findViewById(R.id.ageText);
                clubText = view.findViewById(R.id.clubText);
                approveButton = view.findViewById(R.id.approveButton);
                rejectButton = view.findViewById(R.id.rejectButton);
            }
            
            void bind(User user, int position) {
                if (user == null) {
                    Log.e(TAG, "Attempting to bind null user at position " + position);
                    return;
                }
                
                usernameText.setText(user.getUsername() != null ? user.getUsername() : "Unknown");
                emailText.setText(user.getEmail() != null ? user.getEmail() : "No email");
                
                if (user.getRole() != null) {
                    roleText.setText(user.getRole().getDisplayName());
                } else {
                    roleText.setText("Unknown Role");
                }
                
                if (user.getAge() != null && user.getAge() > 0) {
                    ageText.setText("Age: " + user.getAge());
                    ageText.setVisibility(View.VISIBLE);
                } else {
                    ageText.setVisibility(View.GONE);
                }
                
                // Show club if assigned
                if (user.getClubId() != null && user.getClubId() != 0) {
                    clubText.setText("Loading club...");
                    clubText.setVisibility(View.VISIBLE);
                    
                    new Thread(() -> {
                        try {
                            Club club = clubRepository.findById(user.getClubId());
                            runOnUiThread(() -> {
                                if (club != null) {
                                    clubText.setText("Club: " + club.getClubName());
                                } else {
                                    clubText.setText("Club: Not found (ID: " + user.getClubId() + ")");
                                }
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Error loading club", e);
                            runOnUiThread(() -> {
                                clubText.setText("Club: Error loading");
                            });
                        }
                    }).start();
                } else {
                    clubText.setText("Club: Free Agent");
                    clubText.setVisibility(View.VISIBLE);
                }
                
                approveButton.setOnClickListener(v -> approveUser(user, position));
                rejectButton.setOnClickListener(v -> rejectUser(user, position));
            }
        }
    }
}
