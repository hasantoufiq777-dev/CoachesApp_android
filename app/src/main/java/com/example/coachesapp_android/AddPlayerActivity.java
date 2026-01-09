package com.example.coachesapp_android;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.coachesapp_android.database.IClubRepository;
import com.example.coachesapp_android.database.IPlayerRepository;
import com.example.coachesapp_android.model.Club;
import com.example.coachesapp_android.model.Player;
import com.example.coachesapp_android.model.Position;
import com.example.coachesapp_android.util.RepositoryFactory;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class AddPlayerActivity extends AppCompatActivity {
    private static final String TAG = "AddPlayerActivity";
    
    private TextInputEditText nameInput;
    private TextInputEditText ageInput;
    private TextInputEditText jerseyInput;
    private Spinner positionSpinner;
    private Spinner clubSpinner;
    private Button saveButton;
    private Button backButton;
    private TextView errorText;
    private ProgressBar progressBar;
    
    private IPlayerRepository playerRepository;
    private IClubRepository clubRepository;
    private List<Club> clubs;
    private Player editingPlayer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_player);
        
        playerRepository = RepositoryFactory.getPlayerRepository();
        clubRepository = RepositoryFactory.getClubRepository();
        
        initializeViews();
        setupPositionSpinner();
        loadClubs();
        setupListeners();
        
        // Check if editing existing player
        int playerId = getIntent().getIntExtra("player_id", -1);
        if (playerId != -1) {
            loadPlayer(playerId);
        }
    }
    
    private void initializeViews() {
        nameInput = findViewById(R.id.nameInput);
        ageInput = findViewById(R.id.ageInput);
        jerseyInput = findViewById(R.id.jerseyInput);
        positionSpinner = findViewById(R.id.positionSpinner);
        clubSpinner = findViewById(R.id.clubSpinner);
        saveButton = findViewById(R.id.saveButton);
        backButton = findViewById(R.id.backButton);
        errorText = findViewById(R.id.errorText);
        progressBar = findViewById(R.id.progressBar);
        
        if (progressBar == null) {
            progressBar = new ProgressBar(this);
            progressBar.setVisibility(View.GONE);
        }
    }
    
    private void setupPositionSpinner() {
        Position[] positions = Position.values();
        String[] positionNames = new String[positions.length];
        
        for (int i = 0; i < positions.length; i++) {
            positionNames[i] = positions[i].getDisplayName();
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            positionNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        positionSpinner.setAdapter(adapter);
    }
    
    private void loadClubs() {
        new Thread(() -> {
            clubs = clubRepository.findAll();
            
            runOnUiThread(() -> {
                String[] clubNames = new String[clubs.size() + 1];
                clubNames[0] = "No Club";
                
                for (int i = 0; i < clubs.size(); i++) {
                    clubNames[i + 1] = clubs.get(i).getClubName();
                }
                
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    clubNames
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                clubSpinner.setAdapter(adapter);
            });
        }).start();
    }
    
    private void loadPlayer(int playerId) {
        showLoading(true);
        
        new Thread(() -> {
            editingPlayer = playerRepository.findById(playerId);
            
            runOnUiThread(() -> {
                showLoading(false);
                
                if (editingPlayer != null) {
                    // Populate form with player data
                    nameInput.setText(editingPlayer.getName());
                    ageInput.setText(String.valueOf(editingPlayer.getAge()));
                    jerseyInput.setText(String.valueOf(editingPlayer.getJersey()));
                    
                    // Set position
                    Position[] positions = Position.values();
                    for (int i = 0; i < positions.length; i++) {
                        if (positions[i] == editingPlayer.getPosition()) {
                            positionSpinner.setSelection(i);
                            break;
                        }
                    }
                    
                    // Set club
                    if (editingPlayer.getClubId() != null && clubs != null) {
                        for (int i = 0; i < clubs.size(); i++) {
                            if (clubs.get(i).getId().equals(editingPlayer.getClubId())) {
                                clubSpinner.setSelection(i + 1);
                                break;
                            }
                        }
                    }
                    
                    saveButton.setText("Update Player");
                }
            });
        }).start();
    }
    
    private void setupListeners() {
        saveButton.setOnClickListener(v -> handleSave());
        backButton.setOnClickListener(v -> finish());
    }
    
    private void handleSave() {
        String name = nameInput.getText().toString().trim();
        String ageStr = ageInput.getText().toString().trim();
        String jerseyStr = jerseyInput.getText().toString().trim();
        
        // Validation
        if (name.isEmpty() || ageStr.isEmpty() || jerseyStr.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }
        
        int age, jersey;
        try {
            age = Integer.parseInt(ageStr);
            jersey = Integer.parseInt(jerseyStr);
        } catch (NumberFormatException e) {
            showError("Age and jersey must be numbers");
            return;
        }
        
        if (age < 1 || age > 100) {
            showError("Age must be between 1 and 100");
            return;
        }
        
        if (jersey < 1 || jersey > 99) {
            showError("Jersey number must be between 1 and 99");
            return;
        }
        
        showLoading(true);
        errorText.setVisibility(View.GONE);
        
        // Create or update player
        Player player = editingPlayer != null ? editingPlayer : new Player();
        player.setName(name);
        player.setAge(age);
        player.setJersey(jersey);
        
        // Get selected position
        String selectedPosition = positionSpinner.getSelectedItem().toString();
        for (Position pos : Position.values()) {
            if (pos.getDisplayName().equals(selectedPosition)) {
                player.setPosition(pos);
                break;
            }
        }
        
        // Get selected club
        int clubSelection = clubSpinner.getSelectedItemPosition();
        if (clubSelection > 0 && clubs != null && clubSelection - 1 < clubs.size()) {
            player.setClubId(clubs.get(clubSelection - 1).getId());
        } else {
            player.setClubId(null);
        }
        
        // Save in background thread
        new Thread(() -> {
            Player savedPlayer = playerRepository.save(player);
            
            runOnUiThread(() -> {
                showLoading(false);
                
                if (savedPlayer != null) {
                    Toast.makeText(this, 
                        editingPlayer != null ? "Player updated!" : "Player added!", 
                        Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    showError("Failed to save player. Please try again.");
                }
            });
        }).start();
    }
    
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        saveButton.setEnabled(!show);
        backButton.setEnabled(!show);
    }
    
    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }
}