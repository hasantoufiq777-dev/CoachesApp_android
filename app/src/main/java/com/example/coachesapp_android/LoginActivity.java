package com.example.coachesapp_android;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.coachesapp_android.database.FirebaseUserRepository;
import com.example.coachesapp_android.model.User;
import com.example.coachesapp_android.model.Role;
import com.example.coachesapp_android.util.AppState;
import com.example.coachesapp_android.util.RepositoryFactory;
import com.example.coachesapp_android.util.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    
    private TextInputEditText emailUsernameInput;
    private TextInputEditText passwordInput;
    private TextView errorText;
    private TextView loginModeHint;
    private Button loginButton;
    private Button registerButton;
    private ProgressBar progressBar;
    private RadioGroup loginModeGroup;
    private RadioButton adminModeRadio;
    private RadioButton playerManagerModeRadio;
    private TextInputLayout usernameLayout;
    
    private SessionManager sessionManager;
    private FirebaseAuth firebaseAuth;
    private FirebaseUserRepository userRepository;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        sessionManager = new SessionManager(this);
        firebaseAuth = FirebaseAuth.getInstance();
        userRepository = RepositoryFactory.getFirebaseUserRepository();
        
        // Check if already logged in
        if (sessionManager.isLoggedIn() && firebaseAuth.getCurrentUser() != null) {
            navigateToMain();
            return;
        }
        
        initializeViews();
        setupListeners();
    }
    
    private void initializeViews() {
        emailUsernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        errorText = findViewById(R.id.errorText);
        loginModeHint = findViewById(R.id.loginModeHint);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        progressBar = findViewById(R.id.progressBar);
        loginModeGroup = findViewById(R.id.loginModeGroup);
        adminModeRadio = findViewById(R.id.adminModeRadio);
        playerManagerModeRadio = findViewById(R.id.playerManagerModeRadio);
        usernameLayout = findViewById(R.id.usernameLayout);
        
        if (progressBar == null) {
            progressBar = new ProgressBar(this);
            progressBar.setVisibility(View.GONE);
        }
    }
    
    private void setupListeners() {
        loginButton.setOnClickListener(v -> handleLogin());
        
        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
            startActivity(intent);
        });
        
        loginModeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.adminModeRadio) {
                loginModeHint.setText("Admin: Enter your Firebase Authentication email and password");
                usernameLayout.setHint("Email");
                registerButton.setVisibility(View.GONE);
            } else {
                loginModeHint.setText("Player/Manager: Login with email or username");
                usernameLayout.setHint("Email or Username");
                registerButton.setVisibility(View.VISIBLE);
            }
        });
    }
    
    private void handleLogin() {
        String emailOrUsername = emailUsernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        
        if (emailOrUsername.isEmpty() || password.isEmpty()) {
            showError("Please enter username and password");
            return;
        }
        
        showLoading(true);
        errorText.setVisibility(View.GONE);
        
        // Check login mode
        if (adminModeRadio.isChecked()) {
            handleAdminLogin(emailOrUsername, password);
        } else {
            if (password.length() < 6) {
                showError("Password must be at least 6 characters");
                showLoading(false);
                return;
            }
            
            if (Patterns.EMAIL_ADDRESS.matcher(emailOrUsername).matches()) {
                loginWithEmail(emailOrUsername, password);
            } else {
                loginWithUsername(emailOrUsername, password);
            }
        }
    }
    
    private void handleAdminLogin(String email, String password) {
        // Authenticate with Firebase using entered email/password
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                FirebaseUser firebaseUser = authResult.getUser();
                if (firebaseUser != null) {
                    // Check if user has admin role in Firestore
                    new Thread(() -> {
                        User adminUser = userRepository.findByEmail(email);
                        
                        if (adminUser == null) {
                            // Create admin profile on first login
                            Log.d(TAG, "Creating new admin profile for: " + email);
                            adminUser = new User();
                            adminUser.setUsername(firebaseUser.getEmail().split("@")[0]);
                            adminUser.setEmail(firebaseUser.getEmail());
                            adminUser.setRole(Role.SYSTEM_ADMIN);
                            adminUser.setAge(0);
                            adminUser.setClubId(null);
                            adminUser.setApproved(true); // Admin is auto-approved
                            
                            Log.d(TAG, "Saving admin user: " + adminUser.getUsername() + ", Role: " + adminUser.getRole());
                            User savedAdmin = userRepository.save(adminUser);
                            Log.d(TAG, "Save result: " + (savedAdmin != null ? "Success" : "Failed"));
                            User finalAdmin = savedAdmin;
                            
                            runOnUiThread(() -> {
                                showLoading(false);
                                if (finalAdmin != null) {
                                    sessionManager.createLoginSession(finalAdmin);
                                    AppState.getInstance().currentUser = finalAdmin;
                                    Toast.makeText(this, "Admin profile created successfully", Toast.LENGTH_SHORT).show();
                                    navigateToMain();
                                } else {
                                    showError("Failed to create admin profile. Check Firestore permissions in Firebase Console.");
                                    Log.e(TAG, "Admin save returned null - likely Firestore permission issue");
                                }
                            });
                        } else {
                            // Check if user has admin role
                            if (adminUser.getRole() == Role.SYSTEM_ADMIN) {
                                User finalAdminUser = adminUser;
                                runOnUiThread(() -> {
                                    showLoading(false);
                                    sessionManager.createLoginSession(finalAdminUser);
                                    AppState.getInstance().currentUser = finalAdminUser;
                                    navigateToMain();
                                });
                            } else {
                                runOnUiThread(() -> {
                                    showLoading(false);
                                    showError("This account does not have admin privileges");
                                });
                            }
                        }
                    }).start();
                }
            })
            .addOnFailureListener(e -> {
                showLoading(false);
                Log.e(TAG, "Admin Firebase login failed", e);
                
                String errorMessage = "Authentication failed";
                if (e.getMessage() != null) {
                    if (e.getMessage().contains("no user record")) {
                        errorMessage = "Account not found. Please create this account in Firebase Authentication.";
                    } else if (e.getMessage().contains("password is invalid")) {
                        errorMessage = "Invalid password";
                    } else if (e.getMessage().contains("network")) {
                        errorMessage = "Network error. Please check your connection.";
                    }
                }
                showError(errorMessage);
            });
    }
    
    private void handleNonAdminLogin(String emailOrUsername, String password) {
        if (TextUtils.isEmpty(emailOrUsername) || TextUtils.isEmpty(password)) {
            showLoading(false);
            showError("Please enter email/username and password");
            return;
        }
        
        // Try to login with email first
        if (Patterns.EMAIL_ADDRESS.matcher(emailOrUsername).matches()) {
            loginWithEmail(emailOrUsername, password);
        } else {
            loginWithUsername(emailOrUsername, password);
        }
    }
    
    private void loginWithEmail(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        // Get user data from Firestore
                        new Thread(() -> {
                            User user = userRepository.findByEmail(email);
                            
                            // Fix missing email/age from Firebase Auth if needed
                            if (user != null) {
                                boolean needsUpdate = false;
                                
                                // If email is missing or empty in Firestore, get it from Firebase Auth
                                if ((user.getEmail() == null || user.getEmail().isEmpty()) && firebaseUser.getEmail() != null) {
                                    Log.d(TAG, "Updating missing email from Firebase Auth: " + firebaseUser.getEmail());
                                    user.setEmail(firebaseUser.getEmail());
                                    needsUpdate = true;
                                }
                                
                                // Update if needed
                                if (needsUpdate) {
                                    User updatedUser = userRepository.save(user);
                                    if (updatedUser != null) {
                                        user = updatedUser;
                                        Log.d(TAG, "User data updated successfully");
                                    }
                                }
                            }
                            
                            User finalUser = user;
                            runOnUiThread(() -> {
                                showLoading(false);
                                
                                if (finalUser != null) {
                                    Log.d(TAG, "User found: " + finalUser.getUsername() + ", Email: " + finalUser.getEmail() + ", Role: " + finalUser.getRole() + ", Approved: " + finalUser.isApproved());
                                    
                                    // Check if user is approved (skip for admins)
                                    if (finalUser.getRole() != Role.SYSTEM_ADMIN && !finalUser.isApproved()) {
                                        Log.d(TAG, "User not approved, blocking login");
                                        showError("Your account is pending admin approval");
                                        firebaseAuth.signOut();
                                        return;
                                    }
                                    
                                    sessionManager.createLoginSession(finalUser);
                                    AppState.getInstance().currentUser = finalUser;
                                    navigateToMain();
                                } else {
                                    showError("User profile not found. Please complete registration.");
                                    firebaseAuth.signOut();
                                }
                            });
                        }).start();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Login failed", e);
                    showError("Invalid email or password");
                });
    }
    
    private void loginWithUsername(String username, String password) {
        // Run in background thread
        new Thread(() -> {
            User user = userRepository.findByUsername(username);
            
            runOnUiThread(() -> {
                if (user != null && user.getEmail() != null) {
                    // Found user, now authenticate with Firebase
                    loginWithEmail(user.getEmail(), password);
                } else {
                    showLoading(false);
                    showError("Username not found. Please use email to login.");
                }
            });
        }).start();
    }
    
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        loginButton.setEnabled(!show);
        registerButton.setEnabled(!show);
    }
    
    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }
    
    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
