package com.example.coachesapp_android;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coachesapp_android.database.FirebaseClubRepository;
import com.example.coachesapp_android.database.FirebaseUserRepository;
import com.example.coachesapp_android.database.IClubRepository;
import com.example.coachesapp_android.database.IUserRepository;
import com.example.coachesapp_android.model.Club;
import com.example.coachesapp_android.model.Role;
import com.example.coachesapp_android.model.User;
import com.example.coachesapp_android.util.AppState;

import java.util.ArrayList;
import java.util.List;

public class ManagerListActivity extends AppCompatActivity {
    private RecyclerView managersRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyText;
    private TextView managerCountText;
    private Button backButton;

    private IUserRepository userRepository;
    private IClubRepository clubRepository;

    private List<User> managers = new ArrayList<>();
    private ManagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_list);

        // Initialize repositories
        userRepository = new FirebaseUserRepository();
        clubRepository = new FirebaseClubRepository();

        // Initialize views
        managersRecyclerView = findViewById(R.id.managersRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyText = findViewById(R.id.emptyText);
        managerCountText = findViewById(R.id.managerCountText);
        backButton = findViewById(R.id.backButton);

        // Setup RecyclerView
        adapter = new ManagerAdapter();
        managersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        managersRecyclerView.setAdapter(adapter);

        // Setup buttons
        backButton.setOnClickListener(v -> finish());

        loadManagers();
    }

    private void loadManagers() {
        progressBar.setVisibility(View.VISIBLE);
        managersRecyclerView.setVisibility(View.GONE);
        emptyText.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                List<User> allUsers = userRepository.findAll();
                managers.clear();
                
                // Filter for approved managers and club owners only
                for (User user : allUsers) {
                    if ((user.getRole() == Role.CLUB_MANAGER || user.getRole() == Role.CLUB_OWNER) 
                            && user.isApproved()) {
                        managers.add(user);
                    }
                }

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    managerCountText.setText("Total Managers: " + managers.size());
                    
                    if (managers.isEmpty()) {
                        emptyText.setVisibility(View.VISIBLE);
                        managersRecyclerView.setVisibility(View.GONE);
                    } else {
                        emptyText.setVisibility(View.GONE);
                        managersRecyclerView.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading managers: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void deleteManager(User manager) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Manager")
                .setMessage("Are you sure you want to delete " + manager.getUsername() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    new Thread(() -> {
                        boolean success = userRepository.delete(manager.getId());
                        runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(this, "Manager deleted successfully", Toast.LENGTH_SHORT).show();
                                loadManagers();
                            } else {
                                Toast.makeText(this, "Failed to delete manager", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void viewManagerProfile(User manager) {
        // Set the current manager in AppState
        AppState.getInstance().currentUser = manager;
        Intent intent = new Intent(this, ManagerProfileActivity.class);
        startActivity(intent);
    }

    private void viewManagerPlayers(User manager) {
        if (manager.getClubId() == null) {
            Toast.makeText(this, "Manager not assigned to any club", Toast.LENGTH_SHORT).show();
            return;
        }

        // Open player list filtered by this manager's club
        Intent intent = new Intent(this, PlayerListActivity.class);
        intent.putExtra("clubId", manager.getClubId());
        startActivity(intent);
    }

    private class ManagerAdapter extends RecyclerView.Adapter<ManagerAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_manager, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User manager = managers.get(position);
            
            holder.managerName.setText(manager.getUsername());
            holder.managerAge.setText(manager.getAge() != null ? String.valueOf(manager.getAge()) : "N/A");
            holder.managerRole.setText(manager.getRole() != null ? manager.getRole().getDisplayName() : "Manager");
            holder.managerEmail.setText("Email: " + (manager.getEmail() != null ? manager.getEmail() : "N/A"));
            
            // Get club name
            if (manager.getClubId() != null) {
                new Thread(() -> {
                    Club club = clubRepository.findById(manager.getClubId());
                    runOnUiThread(() -> {
                        if (club != null) {
                            holder.managerClub.setText("Club: " + club.getClubName());
                        } else {
                            holder.managerClub.setText("Club: Not Assigned");
                        }
                    });
                }).start();
            } else {
                holder.managerClub.setText("Club: Not Assigned");
            }

            // Button actions
            holder.viewProfileButton.setOnClickListener(v -> viewManagerProfile(manager));
            holder.viewPlayersButton.setOnClickListener(v -> viewManagerPlayers(manager));
            holder.deleteButton.setOnClickListener(v -> deleteManager(manager));
        }

        @Override
        public int getItemCount() {
            return managers.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView managerName, managerAge, managerRole, managerClub, managerEmail;
            Button viewProfileButton, viewPlayersButton, deleteButton;

            ViewHolder(View itemView) {
                super(itemView);
                managerName = itemView.findViewById(R.id.managerName);
                managerAge = itemView.findViewById(R.id.managerAge);
                managerRole = itemView.findViewById(R.id.managerRole);
                managerClub = itemView.findViewById(R.id.managerClub);
                managerEmail = itemView.findViewById(R.id.managerEmail);
                viewProfileButton = itemView.findViewById(R.id.viewProfileButton);
                viewPlayersButton = itemView.findViewById(R.id.viewPlayersButton);
                deleteButton = itemView.findViewById(R.id.deleteButton);
            }
        }
    }
}
