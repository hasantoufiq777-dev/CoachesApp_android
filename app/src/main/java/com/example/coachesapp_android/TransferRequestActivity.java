package com.example.coachesapp_android;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coachesapp_android.model.Player;
import com.example.coachesapp_android.model.Role;
import com.example.coachesapp_android.model.TransferRequest;
import com.example.coachesapp_android.model.User;
import com.example.coachesapp_android.database.FirebaseClubRepository;
import com.example.coachesapp_android.database.FirebasePlayerRepository;
import com.example.coachesapp_android.repository.FirebaseTransferRequestRepository;
import com.example.coachesapp_android.database.IClubRepository;
import com.example.coachesapp_android.database.IPlayerRepository;
import com.example.coachesapp_android.repository.ITransferRequestRepository;
import com.example.coachesapp_android.util.AppState;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TransferRequestActivity extends AppCompatActivity {
    private static final String TAG = "TransferRequestActivity";
    private RecyclerView transferRequestsRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyText;
    private TextView userInfoText;
    private Button backButton;
    private Button newRequestButton;
    private Button filterAllButton, filterPendingButton, filterInMarketButton, filterCompletedButton;

    private ITransferRequestRepository transferRequestRepository;
    private IPlayerRepository playerRepository;
    private IClubRepository clubRepository;

    private List<TransferRequest> allTransferRequests = new ArrayList<>();
    private List<TransferRequest> filteredTransferRequests = new ArrayList<>();
    private TransferRequestAdapter adapter;

    private User currentUser;
    private TransferRequest.TransferStatus currentFilter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_request);

        currentUser = AppState.getInstance().currentUser;
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize repositories
        playerRepository = new FirebasePlayerRepository();
        clubRepository = new FirebaseClubRepository();
        transferRequestRepository = new FirebaseTransferRequestRepository(playerRepository, clubRepository);

        // Initialize views
        transferRequestsRecyclerView = findViewById(R.id.transferRequestsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyText = findViewById(R.id.emptyText);
        userInfoText = findViewById(R.id.userInfoText);
        backButton = findViewById(R.id.backButton);
        newRequestButton = findViewById(R.id.newRequestButton);
        
        filterAllButton = findViewById(R.id.filterAllButton);
        filterPendingButton = findViewById(R.id.filterPendingButton);
        filterInMarketButton = findViewById(R.id.filterInMarketButton);
        filterCompletedButton = findViewById(R.id.filterCompletedButton);

        // Setup RecyclerView
        adapter = new TransferRequestAdapter();
        transferRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        transferRequestsRecyclerView.setAdapter(adapter);

        // Setup buttons
        backButton.setOnClickListener(v -> finish());
        
        filterAllButton.setOnClickListener(v -> applyFilter(null));
        filterPendingButton.setOnClickListener(v -> applyFilter(TransferRequest.TransferStatus.PENDING_APPROVAL));
        filterInMarketButton.setOnClickListener(v -> applyFilter(TransferRequest.TransferStatus.IN_MARKET));
        filterCompletedButton.setOnClickListener(v -> applyFilter(TransferRequest.TransferStatus.COMPLETED));

        // Set user info
        String role = currentUser.getRole() != null ? currentUser.getRole().name() : "User";
        userInfoText.setText(currentUser.getUsername() + " (" + role + ")");

        // Show new request button for players only
        if (currentUser.getRole() == Role.PLAYER) {
            newRequestButton.setVisibility(View.VISIBLE);
            newRequestButton.setOnClickListener(v -> showNewRequestDialog());
        }

        loadTransferRequests();
    }

    private void loadTransferRequests() {
        progressBar.setVisibility(View.VISIBLE);
        transferRequestsRecyclerView.setVisibility(View.GONE);
        emptyText.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                // Load requests based on user role
                if (currentUser.getRole() == Role.SYSTEM_ADMIN) {
                    allTransferRequests = transferRequestRepository.findAll();
                } else if (currentUser.getRole() == Role.CLUB_MANAGER) {
                    // Managers see requests from/to their club
                    Log.d(TAG, "Manager loading requests for club ID: " + currentUser.getClubId());
                    
                    if (currentUser.getClubId() == null || currentUser.getClubId() == 0) {
                        Log.w(TAG, "Manager has no club assigned!");
                        allTransferRequests = new ArrayList<>();
                    } else {
                        // First, let's see ALL transfers to debug
                        List<TransferRequest> allTransfers = transferRequestRepository.findAll();
                        Log.d(TAG, "Total transfers in database: " + allTransfers.size());
                        for (TransferRequest tr : allTransfers) {
                            Log.d(TAG, "  - Transfer: ID=" + tr.getId() + ", Player=" + tr.getPlayerName() + 
                                    ", SourceClubId=" + tr.getSourceClubId() + ", DestClubId=" + tr.getDestinationClubId());
                        }
                        
                        List<TransferRequest> sourceRequests = transferRequestRepository.findBySourceClubId(currentUser.getClubId());
                        List<TransferRequest> destRequests = transferRequestRepository.findByDestinationClubId(currentUser.getClubId());
                        Log.d(TAG, "Found " + sourceRequests.size() + " source requests, " + destRequests.size() + " dest requests");
                        allTransferRequests = new ArrayList<>(sourceRequests);
                        for (TransferRequest tr : destRequests) {
                            if (!allTransferRequests.contains(tr)) {
                                allTransferRequests.add(tr);
                            }
                        }
                    }
                } else if (currentUser.getRole() == Role.PLAYER) {
                    // Players see their own requests
                    // Get player ID from user's playerId field
                    if (currentUser.getPlayerId() != null) {
                        allTransferRequests = transferRequestRepository.findByPlayerId(currentUser.getPlayerId());
                    }
                }

                runOnUiThread(() -> {
                    applyFilter(currentFilter);
                    progressBar.setVisibility(View.GONE);
                    if (filteredTransferRequests.isEmpty()) {
                        emptyText.setVisibility(View.VISIBLE);
                        transferRequestsRecyclerView.setVisibility(View.GONE);
                        
                        // Show helpful message if manager has no club
                        if (currentUser.getRole() == Role.CLUB_MANAGER && 
                            (currentUser.getClubId() == null || currentUser.getClubId() == 0)) {
                            emptyText.setText("You are not assigned to any club. Please contact the administrator.");
                        } else if (currentUser.getRole() == Role.CLUB_MANAGER) {
                            emptyText.setText("No transfer requests for your club yet.");
                        } else {
                            emptyText.setText("No transfer requests yet.");
                        }
                    } else {
                        emptyText.setVisibility(View.GONE);
                        transferRequestsRecyclerView.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading transfers: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void applyFilter(TransferRequest.TransferStatus status) {
        currentFilter = status;
        filteredTransferRequests.clear();

        if (status == null) {
            filteredTransferRequests.addAll(allTransferRequests);
        } else {
            for (TransferRequest tr : allTransferRequests) {
                if (tr.getStatus() == status) {
                    filteredTransferRequests.add(tr);
                }
            }
        }

        adapter.notifyDataSetChanged();
        
        if (filteredTransferRequests.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            transferRequestsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            transferRequestsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showNewRequestDialog() {
        // First check if player has a club
        new Thread(() -> {
            try {
                Player currentPlayer = null;
                if (currentUser.getPlayerId() != null) {
                    currentPlayer = playerRepository.findById(currentUser.getPlayerId());
                }
                
                if (currentPlayer == null) {
                    runOnUiThread(() -> {
                        new AlertDialog.Builder(this)
                                .setTitle("No Player Profile")
                                .setMessage("You don't have a player profile yet. Please contact the administrator.")
                                .setPositiveButton("OK", null)
                                .show();
                    });
                    return;
                }
                
                if (currentPlayer.getClubId() == null || currentPlayer.getClubId() == 0) {
                    runOnUiThread(() -> {
                        new AlertDialog.Builder(this)
                                .setTitle("No Club Assigned")
                                .setMessage("You must be assigned to a club before requesting a transfer. Please contact your administrator.")
                                .setPositiveButton("OK", null)
                                .show();
                    });
                    return;
                }
                
                // Player has valid club, show transfer type dialog
                runOnUiThread(() -> {
                    String[] transferTypes = {"Transfer to Specific Club", "Transfer to General Market"};
                    
                    new AlertDialog.Builder(this)
                            .setTitle("Select Transfer Type")
                            .setItems(transferTypes, (dialog, which) -> {
                                if (which == 0) {
                                    // Direct club transfer
                                    showClubSelectionDialog();
                                } else {
                                    // General market transfer
                                    showGeneralMarketRequestDialog();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
    
    private void showClubSelectionDialog() {
        new Thread(() -> {
            try {
                List<com.example.coachesapp_android.model.Club> allClubs = clubRepository.findAll();
                
                // Filter out current club
                List<com.example.coachesapp_android.model.Club> availableClubs = new ArrayList<>();
                Integer currentClubId = null;
                
                // Get current player's club
                Player currentPlayer = null;
                if (currentUser.getPlayerId() != null) {
                    currentPlayer = playerRepository.findById(currentUser.getPlayerId());
                    if (currentPlayer != null) {
                        currentClubId = currentPlayer.getClubId();
                    }
                }
                
                for (com.example.coachesapp_android.model.Club club : allClubs) {
                    if (!club.getId().equals(currentClubId)) {
                        availableClubs.add(club);
                    }
                }
                
                runOnUiThread(() -> {
                    if (availableClubs.isEmpty()) {
                        Toast.makeText(this, "No available clubs", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    String[] clubNames = new String[availableClubs.size()];
                    for (int i = 0; i < availableClubs.size(); i++) {
                        clubNames[i] = availableClubs.get(i).getClubName();
                    }
                    
                    new AlertDialog.Builder(this)
                            .setTitle("Select Destination Club")
                            .setItems(clubNames, (dialog, which) -> {
                                com.example.coachesapp_android.model.Club selectedClub = availableClubs.get(which);
                                submitDirectClubTransfer(selectedClub);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error loading clubs: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
    
    private void showGeneralMarketRequestDialog() {
        new AlertDialog.Builder(this)
                .setTitle("General Market Transfer")
                .setMessage("Submit your transfer request to the general market. Your manager will set the release fee upon approval.")
                .setPositiveButton("Submit", (dialog, which) -> {
                    submitGeneralMarketTransfer();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void submitDirectClubTransfer(com.example.coachesapp_android.model.Club destinationClub) {
        new Thread(() -> {
            try {
                Player currentPlayer = null;
                if (currentUser.getPlayerId() != null) {
                    currentPlayer = playerRepository.findById(currentUser.getPlayerId());
                }
                
                if (currentPlayer == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Player profile not found", Toast.LENGTH_SHORT).show());
                    return;
                }
                
                Log.d(TAG, "Submitting transfer - Player: " + currentPlayer.getName() + ", SourceClubId: " + currentPlayer.getClubId() + ", DestClubId: " + destinationClub.getId());
                
                TransferRequest request = new TransferRequest(
                        currentPlayer.getId(),
                        currentPlayer.getClubId(),
                        destinationClub.getId()
                );
                request.setPlayerName(currentPlayer.getName());
                
                if (currentPlayer.getClubId() != null && currentPlayer.getClubId() != 0) {
                    com.example.coachesapp_android.model.Club sourceClub = clubRepository.findById(currentPlayer.getClubId());
                    if (sourceClub != null) {
                        request.setSourceClubName(sourceClub.getClubName());
                    }
                }
                request.setDestinationClubName(destinationClub.getClubName());
                
                TransferRequest savedRequest = transferRequestRepository.save(request);
                
                runOnUiThread(() -> {
                    if (savedRequest != null) {
                        Toast.makeText(this, "Transfer request submitted successfully!", Toast.LENGTH_SHORT).show();
                        loadTransferRequests();
                    } else {
                        Toast.makeText(this, "Failed to submit request", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
    
    private void submitGeneralMarketTransfer() {
        new Thread(() -> {
            try {
                Player currentPlayer = null;
                if (currentUser.getPlayerId() != null) {
                    currentPlayer = playerRepository.findById(currentUser.getPlayerId());
                }
                
                if (currentPlayer == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Player profile not found", Toast.LENGTH_SHORT).show());
                    return;
                }
                
                Log.d(TAG, "Submitting market transfer - Player: " + currentPlayer.getName() + ", SourceClubId: " + currentPlayer.getClubId());
                
                TransferRequest request = new TransferRequest(
                        currentPlayer.getId(),
                        currentPlayer.getClubId(),
                        null // No destination club for market transfer
                );
                request.setPlayerName(currentPlayer.getName());
                
                if (currentPlayer.getClubId() != null && currentPlayer.getClubId() != 0) {
                    com.example.coachesapp_android.model.Club sourceClub = clubRepository.findById(currentPlayer.getClubId());
                    if (sourceClub != null) {
                        request.setSourceClubName(sourceClub.getClubName());
                    }
                }
                
                TransferRequest savedRequest = transferRequestRepository.save(request);
                
                runOnUiThread(() -> {
                    if (savedRequest != null) {
                        Toast.makeText(this, "Market transfer request submitted successfully!", Toast.LENGTH_SHORT).show();
                        loadTransferRequests();
                    } else {
                        Toast.makeText(this, "Failed to submit request", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void approveTransferRequest(TransferRequest request) {
        // Both transfer types now require setting a fee
        if (request.getTransferType() == TransferRequest.TransferType.GENERAL_MARKET) {
            // Prompt for release fee for general market
            showReleaseFeeDialog(request, true);
        } else {
            // Direct club transfer - also requires setting transfer fee
            showReleaseFeeDialog(request, false);
        }
    }
    
    private void showReleaseFeeDialog(TransferRequest request, boolean isGeneralMarket) {
        // Create a properly formatted dialog view
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(50, 40, 50, 10);
        
        EditText releaseFeeInput = new EditText(this);
        releaseFeeInput.setHint("Enter transfer fee (e.g., 50000)");
        releaseFeeInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        dialogLayout.addView(releaseFeeInput);
        
        String title = isGeneralMarket ? "Approve Market Transfer" : "Approve Direct Transfer";
        String message = isGeneralMarket 
            ? "Set release fee for " + request.getPlayerName() + " to enter the transfer market"
            : "Set transfer fee for " + request.getPlayerName() + " to transfer to " + request.getDestinationClubName();
        
        Log.d(TAG, "Showing release fee dialog for player: " + request.getPlayerName() + ", Type: " + request.getTransferType());
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setView(dialogLayout)
                .setPositiveButton("Approve", null) // Set to null, we'll override below
                .setNegativeButton("Cancel", null)
                .create();
        
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String feeStr = releaseFeeInput.getText().toString().trim();
                
                if (feeStr.isEmpty()) {
                    Toast.makeText(this, "Please enter a transfer fee", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                double fee = 0;
                try {
                    fee = Double.parseDouble(feeStr);
                    if (fee <= 0) {
                        Toast.makeText(this, "Transfer fee must be greater than 0", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid fee amount. Please enter a valid number", Toast.LENGTH_SHORT).show();
                    return;
                }

                Log.d(TAG, "Approving transfer with fee: $" + fee);
                if (isGeneralMarket) {
                    approveMarketTransfer(request, fee);
                } else {
                    approveDirectClubTransfer(request, fee);
                }
                dialog.dismiss();
            });
        });
        
        dialog.show();
    }
    
    private void approveMarketTransfer(TransferRequest request, double releaseFee) {
        Log.d(TAG, "Approving market transfer - Player: " + request.getPlayerName() + 
                ", PlayerId: " + request.getPlayerId() +
                ", SourceClubId: " + request.getSourceClubId() + 
                ", SourceClubName: " + request.getSourceClubName() +
                ", ReleaseFee: $" + releaseFee);
        
        new Thread(() -> {
            request.setStatus(TransferRequest.TransferStatus.IN_MARKET);
            request.setApprovedBySourceDate(LocalDateTime.now());
            request.setReleaseFee(releaseFee);
            
            Log.d(TAG, "About to update transfer request - ID: " + request.getId() + 
                    ", PlayerId: " + request.getPlayerId() + 
                    ", PlayerName: " + request.getPlayerName());
            
            Log.d(TAG, "Updating transfer request with IN_MARKET status");
            boolean success = transferRequestRepository.update(request);
            
            if (success) {
                Log.d(TAG, "Transfer approved successfully. Player now in market with fee: $" + releaseFee);
            } else {
                Log.e(TAG, "Failed to update transfer request to IN_MARKET status");
            }
            
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Player approved for transfer market!\nRelease Fee: $" + String.format("%.2f", releaseFee), Toast.LENGTH_LONG).show();
                    loadTransferRequests();
                } else {
                    Toast.makeText(this, "Failed to approve transfer", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
    
    private void approveDirectClubTransfer(TransferRequest request, double transferFee) {
        Log.d(TAG, "Approving direct club transfer - Player: " + request.getPlayerName() + 
                ", From: " + request.getSourceClubName() + 
                ", To: " + request.getDestinationClubName() +
                ", Fee: $" + transferFee);
        
        new Thread(() -> {
            Log.d(TAG, "BEFORE UPDATE - Request status: " + request.getStatus() + ", ID: " + request.getId());
            
            // Set status to IN_MARKET so destination manager can see and accept it
            request.setStatus(TransferRequest.TransferStatus.IN_MARKET);
            request.setApprovedBySourceDate(LocalDateTime.now());
            request.setReleaseFee(transferFee);
            
            Log.d(TAG, "AFTER SETTING - Request status: " + request.getStatus() + 
                    ", ReleaseFee: " + request.getReleaseFee() + 
                    ", TransferType: " + request.getTransferType());
            
            boolean success = transferRequestRepository.update(request);
            
            if (success) {
                Log.d(TAG, "✓ Direct transfer approved and saved. Status=IN_MARKET, Fee=$" + transferFee);
                Log.d(TAG, "  Player: " + request.getPlayerName() + 
                        ", SourceClub: " + request.getSourceClubId() + 
                        ", DestClub: " + request.getDestinationClubId());
            } else {
                Log.e(TAG, "✗ FAILED to approve direct transfer - update returned false");
            }
            
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Transfer approved! Waiting for " + request.getDestinationClubName() + " manager to accept.\nFee: $" + String.format("%.2f", transferFee), Toast.LENGTH_LONG).show();
                    loadTransferRequests();
                } else {
                    Toast.makeText(this, "Failed to approve transfer", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void cancelTransferRequest(TransferRequest request) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Transfer")
                .setMessage("Cancel transfer request for " + request.getPlayerName() + "?")
                .setPositiveButton("Cancel Transfer", (dialog, which) -> {
                    new Thread(() -> {
                        request.setStatus(TransferRequest.TransferStatus.CANCELLED);
                        boolean success = transferRequestRepository.update(request);
                        
                        runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(this, "Transfer cancelled", Toast.LENGTH_SHORT).show();
                                loadTransferRequests();
                            } else {
                                Toast.makeText(this, "Failed to cancel transfer", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }).start();
                })
                .setNegativeButton("Back", null)
                .show();
    }

    private class TransferRequestAdapter extends RecyclerView.Adapter<TransferRequestAdapter.ViewHolder> {
        private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_transfer_request, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TransferRequest request = filteredTransferRequests.get(position);
            
            holder.playerNameText.setText(request.getPlayerName() != null ? request.getPlayerName() : "Player ID: " + request.getPlayerId());
            holder.sourceClubText.setText("From: " + (request.getSourceClubName() != null ? request.getSourceClubName() : "Unknown"));
            holder.destinationClubText.setText("To: " + (request.getDestinationClubName() != null ? request.getDestinationClubName() : "Transfer Market"));
            holder.transferFeeText.setText("Fee: $" + (request.getTransferFee() != null ? String.format("%.2f", request.getTransferFee()) : "0"));
            
            String statusText = request.getStatus() != null ? request.getStatus().getDisplayName() : "UNKNOWN";
            holder.statusBadge.setText(statusText);
            
            // Color code status
            int statusColor;
            switch (request.getStatus()) {
                case PENDING_APPROVAL:
                    statusColor = 0xFFFF9800; // Orange
                    break;
                case IN_MARKET:
                    statusColor = 0xFF2196F3; // Blue
                    break;
                case COMPLETED:
                    statusColor = 0xFF4CAF50; // Green
                    break;
                case CANCELLED:
                    statusColor = 0xFFF44336; // Red
                    break;
                default:
                    statusColor = 0xFF757575; // Gray
            }
            holder.statusBadge.setBackgroundColor(statusColor);
            
            if (request.getRequestDate() != null) {
                holder.requestDateText.setText("Requested: " + request.getRequestDate().format(dateFormatter));
            }

            // Show action buttons for managers on pending requests from their club
            // Admin can only VIEW transfers, not approve/reject them
            boolean canApprove = currentUser.getRole() == Role.CLUB_MANAGER 
                    && request.getSourceClubId() != null
                    && request.getSourceClubId().equals(currentUser.getClubId())
                    && request.getStatus() == TransferRequest.TransferStatus.PENDING_APPROVAL;

            if (canApprove) {
                holder.actionButtonsLayout.setVisibility(View.VISIBLE);
                holder.approveButton.setOnClickListener(v -> approveTransferRequest(request));
                holder.rejectButton.setOnClickListener(v -> cancelTransferRequest(request));
            } else {
                holder.actionButtonsLayout.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return filteredTransferRequests.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView playerNameText, sourceClubText, destinationClubText, transferFeeText, requestDateText, statusBadge;
            View actionButtonsLayout;
            Button approveButton, rejectButton;

            ViewHolder(View itemView) {
                super(itemView);
                playerNameText = itemView.findViewById(R.id.playerNameText);
                sourceClubText = itemView.findViewById(R.id.sourceClubText);
                destinationClubText = itemView.findViewById(R.id.destinationClubText);
                transferFeeText = itemView.findViewById(R.id.transferFeeText);
                requestDateText = itemView.findViewById(R.id.requestDateText);
                statusBadge = itemView.findViewById(R.id.statusBadge);
                actionButtonsLayout = itemView.findViewById(R.id.actionButtonsLayout);
                approveButton = itemView.findViewById(R.id.approveButton);
                rejectButton = itemView.findViewById(R.id.rejectButton);
            }
        }
    }
}
