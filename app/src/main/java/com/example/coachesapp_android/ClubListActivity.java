package com.example.coachesapp_android;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coachesapp_android.database.IClubRepository;
import com.example.coachesapp_android.model.Club;
import com.example.coachesapp_android.util.RepositoryFactory;

import java.util.ArrayList;
import java.util.List;

public class ClubListActivity extends AppCompatActivity {
    
    private RecyclerView recyclerView;
    private Button addClubButton;
    private Button backButton;
    
    private IClubRepository clubRepository;
    private ClubAdapter adapter;
    private List<Club> clubs;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_club_list);
        
        clubRepository = RepositoryFactory.getClubRepository();
        
        initializeViews();
        loadClubs();
        setupListeners();
    }
    
    private void initializeViews() {
        recyclerView = findViewById(R.id.clubsRecyclerView);
        addClubButton = findViewById(R.id.addClubButton);
        backButton = findViewById(R.id.backButton);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
    
    private void loadClubs() {
        new Thread(() -> {
            clubs = clubRepository.findAll();
            
            runOnUiThread(() -> {
                adapter = new ClubAdapter(clubs);
                recyclerView.setAdapter(adapter);
            });
        }).start();
    }
    
    private void setupListeners() {
        addClubButton.setOnClickListener(v -> showAddClubDialog());
        backButton.setOnClickListener(v -> finish());
    }
    
    private void showAddClubDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Club");
        
        final EditText input = new EditText(this);
        input.setHint("Club Name");
        builder.setView(input);
        
        builder.setPositiveButton("Add", (dialog, which) -> {
            String clubName = input.getText().toString().trim();
            if (!clubName.isEmpty()) {
                addClub(clubName);
            } else {
                Toast.makeText(this, "Please enter a club name", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    
    private void addClub(String clubName) {
        Club club = new Club();
        club.setClubName(clubName);
        
        new Thread(() -> {
            Club savedClub = clubRepository.save(club);
            
            runOnUiThread(() -> {
                if (savedClub != null) {
                    clubs.add(savedClub);
                    adapter.notifyItemInserted(clubs.size() - 1);
                    Toast.makeText(this, "Club added!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to add club", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
    
    private class ClubAdapter extends RecyclerView.Adapter<ClubAdapter.ClubViewHolder> {
        private List<Club> clubs;
        
        public ClubAdapter(List<Club> clubs) {
            this.clubs = clubs != null ? clubs : new ArrayList<>();
        }
        
        @NonNull
        @Override
        public ClubViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_club, parent, false);
            return new ClubViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ClubViewHolder holder, int position) {
            Club club = clubs.get(position);
            holder.bind(club);
        }
        
        @Override
        public int getItemCount() {
            return clubs.size();
        }
        
        class ClubViewHolder extends RecyclerView.ViewHolder {
            TextView clubName;
            Button viewButton;
            Button deleteButton;
            
            public ClubViewHolder(@NonNull View itemView) {
                super(itemView);
                clubName = itemView.findViewById(R.id.clubName);
                viewButton = itemView.findViewById(R.id.viewButton);
                deleteButton = itemView.findViewById(R.id.deleteButton);
            }
            
            public void bind(Club club) {
                clubName.setText(club.getClubName());
                
                viewButton.setOnClickListener(v -> {
                    Intent intent = new Intent(ClubListActivity.this, ClubDetailsActivity.class);
                    intent.putExtra("CLUB_ID", club.getId());
                    startActivity(intent);
                });
                
                deleteButton.setOnClickListener(v -> {
                    new AlertDialog.Builder(ClubListActivity.this)
                        .setTitle("Delete Club")
                        .setMessage("Are you sure you want to delete " + club.getClubName() + "?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            new Thread(() -> {
                                boolean success = clubRepository.delete(club.getId());
                                runOnUiThread(() -> {
                                    if (success) {
                                        int position = getAdapterPosition();
                                        if (position != RecyclerView.NO_POSITION) {
                                            clubs.remove(position);
                                            notifyItemRemoved(position);
                                        }
                                        Toast.makeText(ClubListActivity.this, "Club deleted", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(ClubListActivity.this, "Failed to delete club", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }).start();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                });
            }
        }
    }
}