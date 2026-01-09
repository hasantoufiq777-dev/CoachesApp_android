package com.example.coachesapp_android;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.coachesapp_android.database.FirebaseUserRepository;
import com.example.coachesapp_android.database.IClubRepository;
import com.example.coachesapp_android.model.Club;
import com.example.coachesapp_android.model.Position;
import com.example.coachesapp_android.model.Role;
import com.example.coachesapp_android.model.User;
import com.example.coachesapp_android.util.RepositoryFactory;

import java.util.ArrayList;
import java.util.List;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegistrationActivity extends AppCompatActivity {
    private static final String TAG = "RegistrationActivity";
    
    private TextInputEditText usernameInput;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private TextInputEditText ageInput;
    private Spinner roleSpinner;
    private Spinner positionSpinner;
    private Spinner clubSpinner;
    private Button registerButton;
    private Button backButton;
    private TextView errorText;
    private TextView positionLabel;
    private TextView clubLabel;
    private View ageInputLayout;
    private ProgressBar progressBar;
    private TextView passwordStrengthText;
    
    private FirebaseAuth firebaseAuth;
    private FirebaseUserRepository userRepository;
    private IClubRepository clubRepository;
    private List<Club> clubs = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        
        firebaseAuth = FirebaseAuth.getInstance();
        userRepository = RepositoryFactory.getFirebaseUserRepository();
        clubRepository = RepositoryFactory.getClubRepository();
        
        initializeViews();
        setupRoleSpinner();
        setupPositionSpinner();
        loadClubs();
        setupListeners();
    }
    
    private void initializeViews() {
        usernameInput = findViewById(R.id.usernameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        ageInput = findViewById(R.id.ageInput);
        ageInputLayout = findViewById(R.id.ageInputLayout);
        roleSpinner = findViewById(R.id.roleSpinner);
        positionSpinner = findViewById(R.id.positionSpinner);
        clubSpinner = findViewById(R.id.clubSpinner);
        positionLabel = findViewById(R.id.positionLabel);
        clubLabel = findViewById(R.id.clubLabel);
        registerButton = findViewById(R.id.registerButton);
        backButton = findViewById(R.id.backButton);
        errorText = findViewById(R.id.errorText);
        progressBar = findViewById(R.id.progressBar);
        passwordStrengthText = findViewById(R.id.passwordStrengthText);
        
        if (progressBar == null) {
            progressBar = new ProgressBar(this);
            progressBar.setVisibility(View.GONE);
        }
    }
    
    private void setupRoleSpinner() {
        // Create array of display names for roles (excluding SYSTEM_ADMIN)
        String[] roleNames = new String[]{
            Role.CLUB_MANAGER.getDisplayName(),
            Role.PLAYER.getDisplayName()
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            roleNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);
        
        // Initially show fields for first role (Club Manager)
        updateFieldsVisibility(true, false);
        
        // Show/hide position field based on role selection
        roleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean isPlayer = roleNames[position].equals(Role.PLAYER.getDisplayName());
                boolean isManager = roleNames[position].equals(Role.CLUB_MANAGER.getDisplayName());
                
                updateFieldsVisibility(isManager, isPlayer);
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    
    private void updateFieldsVisibility(boolean isManager, boolean isPlayer) {
        // Show position only for players
        positionSpinner.setVisibility(isPlayer ? View.VISIBLE : View.GONE);
        positionLabel.setVisibility(isPlayer ? View.VISIBLE : View.GONE);
        
        // Show club for both players and managers
        clubSpinner.setVisibility((isPlayer || isManager) ? View.VISIBLE : View.GONE);
        clubLabel.setVisibility((isPlayer || isManager) ? View.VISIBLE : View.GONE);
        
        // Show age for both players and managers
        ageInputLayout.setVisibility(View.VISIBLE);
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
                List<String> clubNames = new ArrayList<>();
                clubNames.add("None (Free Agent)");
                for (Club club : clubs) {
                    clubNames.add(club.getClubName());
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
    
    private void setupListeners() {
        registerButton.setOnClickListener(v -> handleRegistration());
        backButton.setOnClickListener(v -> finish());
        
        // Add password strength listener
        passwordInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordStrength(s.toString());
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }
    
    private void updatePasswordStrength(String password) {
        if (password.isEmpty()) {
            passwordStrengthText.setVisibility(View.GONE);
            return;
        }
        
        int strength = calculatePasswordStrength(password);
        passwordStrengthText.setVisibility(View.VISIBLE);
        
        if (strength < 2) {
            passwordStrengthText.setText("Weak");
            passwordStrengthText.setTextColor(0xFFE74C3C); // Red
        } else if (strength < 4) {
            passwordStrengthText.setText("Medium");
            passwordStrengthText.setTextColor(0xFFF39C12); // Orange
        } else {
            passwordStrengthText.setText("Strong");
            passwordStrengthText.setTextColor(0xFF27AE60); // Green
        }
    }
    
    private int calculatePasswordStrength(String password) {
        int strength = 0;
        
        // Check length
        if (password.length() >= 8) strength++;
        if (password.length() >= 12) strength++;
        
        // Check for uppercase
        if (password.matches(".*[A-Z].*")) strength++;
        
        // Check for lowercase
        if (password.matches(".*[a-z].*")) strength++;
        
        // Check for digits
        if (password.matches(".*[0-9].*")) strength++;
        
        // Check for special characters
        if (password.matches(".*[@#$%^&+=!].*")) strength++;
        
        return strength;
    }
    
    private boolean validatePasswordRequirements(String password) {
        // Must contain at least one letter, one number, and one special character
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        boolean hasSpecial = password.matches(".*[@#$%^&+=!].*");
        
        return hasLetter && hasDigit && hasSpecial && password.length() >= 8;
    }
    
    private void handleRegistration() {
        String username = usernameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();
        
        // Validation
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address");
            return;
        }
        
        if (!validatePasswordRequirements(password)) {
            showError("Password must be at least 8 characters with letters, numbers, and special characters (@#$%^&+=!)");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }
        
        showLoading(true);
        errorText.setVisibility(View.GONE);
        
        // Create Firebase Auth account
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        // Create user profile in Firestore
                        createUserProfile(username, email, firebaseUser.getUid());
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Registration failed", e);
                    
                    String errorMessage = "Registration failed";
                    if (e.getMessage() != null) {
                        if (e.getMessage().contains("already in use")) {
                            errorMessage = "Email is already registered";
                        } else if (e.getMessage().contains("network")) {
                            errorMessage = "Network error. Please check your connection";
                        }
                    }
                    showError(errorMessage);
                });
    }
    
    private void createUserProfile(String username, String email, String uid) {
        // Get selected role
        String selectedRoleDisplay = roleSpinner.getSelectedItem().toString();
        Role role = selectedRoleDisplay.equals(Role.CLUB_MANAGER.getDisplayName()) 
                ? Role.CLUB_MANAGER : Role.PLAYER;
        
        // Create user object
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(role);
        user.setApproved(false); // Requires admin approval
        
        Log.d(TAG, "Creating user: " + username + ", Role: " + role + ", Approved: " + user.isApproved());
        
        // Add age for both players and managers
        String ageStr = ageInput.getText().toString().trim();
        if (!ageStr.isEmpty()) {
            user.setAge(Integer.parseInt(ageStr));
        }
        
        // Add club for both players and managers
        int clubPosition = clubSpinner.getSelectedItemPosition();
        Log.d(TAG, "Club spinner position: " + clubPosition + ", clubs size: " + clubs.size());
        if (clubPosition > 0 && !clubs.isEmpty()) {
            Club selectedClub = clubs.get(clubPosition - 1);
            user.setClubId(selectedClub.getId());
            Log.d(TAG, "Assigned club: " + selectedClub.getClubName() + " (ID: " + selectedClub.getId() + ")");
        } else {
            Log.d(TAG, "No club assigned (Free Agent)");
        }
        
        // Save to Firestore in background thread
        new Thread(() -> {
            Log.d(TAG, "Saving user to Firestore: " + user.getUsername() + ", ClubId: " + user.getClubId() + ", Approved: " + user.isApproved());
            User savedUser = userRepository.save(user);
            Log.d(TAG, "Save result: " + (savedUser != null ? "Success" : "Failed"));
            
            runOnUiThread(() -> {
                showLoading(false);
                
                if (savedUser != null) {
                    Toast.makeText(this, 
                        "Registration submitted! Waiting for admin approval.", 
                        Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    showError("Failed to create user profile. Please try again.");
                    // Clean up Firebase Auth user if Firestore save failed
                    firebaseAuth.getCurrentUser().delete();
                }
            });
        }).start();
    }
    
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        registerButton.setEnabled(!show);
        backButton.setEnabled(!show);
    }
    
    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }
}