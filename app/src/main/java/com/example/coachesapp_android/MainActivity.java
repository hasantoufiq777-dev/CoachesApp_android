package com.example.coachesapp_android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.coachesapp_android.model.Role;
import com.example.coachesapp_android.model.User;
import com.example.coachesapp_android.util.AppState;
import com.example.coachesapp_android.util.SessionManager;
import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private TextView welcomeText;
    private TextView roleText;
    private LinearLayout menuContainer;
    private Button logoutButton;
    
    private SessionManager sessionManager;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        sessionManager = new SessionManager(this);
        
        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }
        
        currentUser = sessionManager.getCurrentUser();
        AppState.getInstance().currentUser = currentUser;
        
        initializeViews();
        setupUI();
        setupListeners();
    }
    
    private void initializeViews() {
        welcomeText = findViewById(R.id.welcomeText);
        roleText = findViewById(R.id.roleText);
        menuContainer = findViewById(R.id.menuContainer);
        logoutButton = findViewById(R.id.logoutButton);
    }
    
    private void setupUI() {
        welcomeText.setText("Welcome, " + currentUser.getUsername());
        roleText.setText("Role: " + currentUser.getRole().getDisplayName());
        
        // Setup menu based on role
        setupMenuForRole(currentUser.getRole());
    }
    
    private void setupMenuForRole(Role role) {
        menuContainer.removeAllViews();
        
        switch (role) {
            case SYSTEM_ADMIN:
            case CLUB_OWNER:
                addMenuItem("Manage Clubs", ClubListActivity.class);
                addMenuItem("Manage Players", PlayerListActivity.class);
                addMenuItem("Manage Managers", ManagerListActivity.class);
                addMenuItem("Pending Approvals", PendingApprovalsActivity.class);
                addMenuItem("Transfer Requests", TransferRequestActivity.class);
                addMenuItem("Transfer Market", TransferMarketActivity.class);
                break;
                
            case CLUB_MANAGER:
                addMenuItem("Manage Players", PlayerListActivity.class);
                addMenuItem("Game Plan", GamePlanActivity.class);
                addMenuItem("Transfer Requests", TransferRequestActivity.class);
                addMenuItem("Transfer Market", TransferMarketActivity.class);
                addMenuItem("My Profile", ManagerProfileActivity.class);
                break;
                
            case PLAYER:
                addMenuItem("Submit Transfer Request", TransferRequestActivity.class);
                addMenuItem("Transfer Market", TransferMarketActivity.class);
                addMenuItem("My Profile", PlayerProfileActivity.class);
                break;
        }
    }
    
    private void addMenuItem(String title, Class<?> activityClass) {
        MaterialButton button = new MaterialButton(this);
        button.setText(title);
        button.setTextSize(16);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        button.setLayoutParams(params);
        
        button.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, activityClass);
            startActivity(intent);
        });
        
        menuContainer.addView(button);
    }
    
    private void setupListeners() {
        logoutButton.setOnClickListener(v -> {
            sessionManager.logout();
            AppState.getInstance().clearData();
            navigateToLogin();
        });
    }
    
    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}