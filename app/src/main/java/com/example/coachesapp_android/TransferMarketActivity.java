package com.example.coachesapp_android;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coachesapp_android.model.Player;
import com.example.coachesapp_android.model.Position;
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
import java.util.ArrayList;
import java.util.List;

public class TransferMarketActivity extends AppCompatActivity {
    private RecyclerView marketPlayersRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyText;
    private TextView marketInfoText;
    private Button backButton;
    private Spinner positionFilterSpinner;

    private ITransferRequestRepository transferRequestRepository;
    private IPlayerRepository playerRepository;
    private IClubRepository clubRepository;

    private List<TransferRequest> allMarketRequests = new ArrayList<>();
    private List<TransferRequest> filteredMarketRequests = new ArrayList<>();
    private List<Player> allPlayers = new ArrayList<>();
    private MarketPlayerAdapter adapter;

    private User currentUser;
    private Position selectedPosition = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_market);

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
        marketPlayersRecyclerView = findViewById(R.id.marketPlayersRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyText = findViewById(R.id.emptyText);
        marketInfoText = findViewById(R.id.marketInfoText);
        backButton = findViewById(R.id.backButton);
        positionFilterSpinner = findViewById(R.id.positionFilterSpinner);

        // Setup RecyclerView
        adapter = new MarketPlayerAdapter();
        marketPlayersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        marketPlayersRecyclerView.setAdapter(adapter);

        // Setup buttons
        backButton.setOnClickListener(v -> finish());

        // Setup position filter
        setupPositionFilter();

        loadMarketPlayers();
    }

    private void setupPositionFilter() {
        List<String> positions = new ArrayList<>();
        positions.add("All Positions");
        for (Position pos : Position.values()) {
            positions.add(pos.name());
        }

        ArrayAdapter<String> posAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, positions);
        posAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        positionFilterSpinner.setAdapter(posAdapter);

        positionFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedPosition = null;
                } else {
                    selectedPosition = Position.values()[position - 1];
                }
                filterMarketPlayers();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedPosition = null;
                filterMarketPlayers();
            }
        });
    }

    private void loadMarketPlayers() {
        progressBar.setVisibility(View.VISIBLE);
        marketPlayersRecyclerView.setVisibility(View.GONE);
        emptyText.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                android.util.Log.d("TransferMarket", "Loading market players...");
                android.util.Log.d("TransferMarket", "Current user club ID: " + currentUser.getClubId());
                
                // Get all transfer requests with IN_MARKET status
                allMarketRequests = transferRequestRepository.findInMarket();
                android.util.Log.d("TransferMarket", "Found " + allMarketRequests.size() + " players in market");
                
                // Log each market player
                for (TransferRequest req : allMarketRequests) {
                    android.util.Log.d("TransferMarket", "  - Player: " + req.getPlayerName() + 
                            ", Type: " + req.getTransferType() +
                            ", SourceClub: " + req.getSourceClubId() +
                            ", DestClub: " + req.getDestinationClubId() +
                            ", Fee: $" + req.getReleaseFee() + 
                            ", Status: " + req.getStatus());
                }
                
                // Load all players for reference
                allPlayers = playerRepository.findAll();
                android.util.Log.d("TransferMarket", "Loaded " + allPlayers.size() + " total players");

                filterMarketPlayers();

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (filteredMarketRequests.isEmpty()) {
                        android.util.Log.d("TransferMarket", "No players in market after filtering");
                        emptyText.setVisibility(View.VISIBLE);
                        marketPlayersRecyclerView.setVisibility(View.GONE);
                    } else {
                        android.util.Log.d("TransferMarket", "Displaying " + filteredMarketRequests.size() + " players in market");
                        emptyText.setVisibility(View.GONE);
                        marketPlayersRecyclerView.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("TransferMarket", "Error loading market", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading market: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void filterMarketPlayers() {
        filteredMarketRequests.clear();
        
        android.util.Log.d("TransferMarket", "=== FILTERING MARKET PLAYERS ===");
        android.util.Log.d("TransferMarket", "Total requests to filter: " + allMarketRequests.size());
        android.util.Log.d("TransferMarket", "Current manager club ID: " + currentUser.getClubId());

        for (TransferRequest tr : allMarketRequests) {
            android.util.Log.d("TransferMarket", "Checking transfer: " + tr.getPlayerName() + 
                    ", Type: " + tr.getTransferType() + 
                    ", SourceClub: " + tr.getSourceClubId() + 
                    ", DestClub: " + tr.getDestinationClubId());
            
            // Show ALL IN_MARKET players to ALL managers
            // The purchase restriction will be handled in the adapter
            boolean shouldShow = false;
            
            // For DIRECT_CLUB transfers, show to ALL managers (they can see but only dest can purchase)
            if (tr.getTransferType() == TransferRequest.TransferType.DIRECT_CLUB) {
                shouldShow = true;
                android.util.Log.d("TransferMarket", "  -> DIRECT_CLUB transfer - showing to all managers");
            } else {
                // GENERAL_MARKET transfers - show to everyone except source club
                if (currentUser.getClubId() == null || !currentUser.getClubId().equals(tr.getSourceClubId())) {
                    shouldShow = true;
                    android.util.Log.d("TransferMarket", "  -> GENERAL_MARKET - showing (not source club)");
                } else {
                    android.util.Log.d("TransferMarket", "  -> GENERAL_MARKET from own club, skipping");
                }
            }
            
            if (shouldShow) {
                // Apply position filter if needed
                if (selectedPosition == null) {
                    filteredMarketRequests.add(tr);
                    android.util.Log.d("TransferMarket", "  -> Added to filtered list (no position filter)");
                } else {
                    Player player = getPlayerById(tr.getPlayerId());
                    if (player != null && player.getPosition() == selectedPosition) {
                        filteredMarketRequests.add(tr);
                        android.util.Log.d("TransferMarket", "  -> Added to filtered list (position match)");
                    } else {
                        android.util.Log.d("TransferMarket", "  -> Filtered out by position");
                    }
                }
            }
        }
        
        android.util.Log.d("TransferMarket", "Filtered result: " + filteredMarketRequests.size() + " players");

        runOnUiThread(() -> {
            adapter.notifyDataSetChanged();
            if (filteredMarketRequests.isEmpty()) {
                emptyText.setVisibility(View.VISIBLE);
                marketPlayersRecyclerView.setVisibility(View.GONE);
            } else {
                emptyText.setVisibility(View.GONE);
                marketPlayersRecyclerView.setVisibility(View.VISIBLE);
            }
        });
    }

    private Player getPlayerById(Integer playerId) {
        android.util.Log.d("TransferMarket", "Looking for player with ID: " + playerId + " in " + allPlayers.size() + " players");
        
        if (playerId == null) {
            android.util.Log.w("TransferMarket", "PlayerId is NULL!");
            return null;
        }
        
        for (Player p : allPlayers) {
            if (p.getId() != null && p.getId().equals(playerId)) {
                android.util.Log.d("TransferMarket", "Found player: " + p.getName() + " (ID: " + p.getId() + ")");
                return p;
            }
        }
        
        android.util.Log.w("TransferMarket", "Player with ID " + playerId + " NOT FOUND in allPlayers list!");
        // Log all player IDs for debugging
        StringBuilder ids = new StringBuilder("Available player IDs: ");
        for (Player p : allPlayers) {
            ids.append(p.getId()).append(", ");
        }
        android.util.Log.d("TransferMarket", ids.toString());
        
        return null;
    }

    private void showPurchaseDialog(TransferRequest marketRequest, Player player) {
        if (currentUser.getRole() != Role.CLUB_MANAGER) {
            Toast.makeText(this, "Only managers can purchase players", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser.getClubId() == null) {
            Toast.makeText(this, "You must be assigned to a club", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser.getClubId().equals(marketRequest.getSourceClubId())) {
            Toast.makeText(this, "Cannot purchase from your own club", Toast.LENGTH_SHORT).show();
            return;
        }

        Double releaseFee = marketRequest.getReleaseFee() != null ? marketRequest.getReleaseFee() : 0.0;
        String playerName = player != null ? player.getName() : "Unknown Player";
        
        // Show confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Purchase Player")
                .setMessage("Purchase " + playerName + " for $" + String.format("%.2f", releaseFee) + "?")
                .setPositiveButton("Purchase", (dialog, which) -> {
                    completePurchase(marketRequest, player, releaseFee);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void completePurchase(TransferRequest marketRequest, Player player, double releaseFee) {
        new Thread(() -> {
            android.util.Log.d("TransferMarket", "Starting purchase - PlayerId: " + marketRequest.getPlayerId() + 
                    ", From Club: " + marketRequest.getSourceClubId() + 
                    ", To Club: " + currentUser.getClubId());
            
            // Update the existing market request with destination club and complete it
            marketRequest.setDestinationClubId(currentUser.getClubId());
            
            // Try to get club name
            try {
                com.example.coachesapp_android.model.Club club = clubRepository.findById(currentUser.getClubId());
                if (club != null) {
                    marketRequest.setDestinationClubName(club.getClubName());
                    android.util.Log.d("TransferMarket", "Destination club name: " + club.getClubName());
                }
            } catch (Exception e) {
                android.util.Log.e("TransferMarket", "Error fetching club name", e);
            }
            
            marketRequest.setTransferFee(releaseFee);
            marketRequest.setStatus(TransferRequest.TransferStatus.COMPLETED);
            marketRequest.setCompletedDate(LocalDateTime.now());
            
            boolean success = transferRequestRepository.update(marketRequest);
            android.util.Log.d("TransferMarket", "Transfer request update success: " + success);
            
            // Update player's club - fetch fresh player if needed
            if (success) {
                Player playerToUpdate = player;
                if (playerToUpdate == null && marketRequest.getPlayerId() != null) {
                    android.util.Log.d("TransferMarket", "Player was null, fetching from repository...");
                    playerToUpdate = playerRepository.findById(marketRequest.getPlayerId());
                }
                
                if (playerToUpdate != null) {
                    android.util.Log.d("TransferMarket", "Updating player " + playerToUpdate.getName() + 
                            " from club " + playerToUpdate.getClubId() + " to club " + currentUser.getClubId());
                    playerToUpdate.setClubId(currentUser.getClubId());
                    boolean playerUpdateSuccess = playerRepository.update(playerToUpdate);
                    android.util.Log.d("TransferMarket", "Player club update success: " + playerUpdateSuccess);
                } else {
                    android.util.Log.e("TransferMarket", "FAILED: Could not find player with ID: " + marketRequest.getPlayerId());
                }
            }
            
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Player purchased successfully!", Toast.LENGTH_SHORT).show();
                    loadMarketPlayers();
                } else {
                    Toast.makeText(this, "Failed to complete purchase", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private class MarketPlayerAdapter extends RecyclerView.Adapter<MarketPlayerAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_market_player, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TransferRequest request = filteredMarketRequests.get(position);
            
            android.util.Log.d("TransferMarket", "Binding position " + position + ": PlayerId=" + request.getPlayerId() + 
                    ", PlayerName from request=" + request.getPlayerName() + 
                    ", SourceClubId=" + request.getSourceClubId() + 
                    ", SourceClubName=" + request.getSourceClubName());
            
            Player player = getPlayerById(request.getPlayerId());
            
            if (player != null) {
                android.util.Log.d("TransferMarket", "Found player object: " + player.getName() + 
                        ", Position=" + player.getPosition() + ", Age=" + player.getAge());
                holder.marketPlayerName.setText(player.getName());
                holder.marketPlayerPosition.setText(player.getPosition() != null ? player.getPosition().name() : "N/A");
                holder.marketPlayerAge.setText("Age: " + player.getAge());
                holder.marketPlayerJersey.setText("Jersey: #" + player.getJersey());
            } else {
                android.util.Log.w("TransferMarket", "Player object is NULL! Using fallback from TransferRequest. Total players loaded: " + allPlayers.size());
                // Use data from TransferRequest as fallback
                String playerName = request.getPlayerName() != null ? request.getPlayerName() : "Unknown Player";
                holder.marketPlayerName.setText(playerName);
                holder.marketPlayerPosition.setText("Position: N/A");
                holder.marketPlayerAge.setText("Age: N/A");
                holder.marketPlayerJersey.setText("Jersey: N/A");
            }

            holder.marketCurrentClub.setText("Current Club: " + (request.getSourceClubName() != null ? request.getSourceClubName() : "Unknown"));
            
            // Show release fee if available, otherwise show transfer fee
            Double displayFee = request.getReleaseFee() != null ? request.getReleaseFee() : request.getTransferFee();
            String feeText = "$" + (displayFee != null ? String.format("%.2f", displayFee) : "0.00");
            holder.marketTransferFee.setText(feeText);
            
            android.util.Log.d("TransferMarket", "Binding player: " + request.getPlayerName() + ", Fee: " + feeText);

            // Handle purchase button visibility and functionality
            if (currentUser.getRole() == Role.CLUB_MANAGER && currentUser.getClubId() != null) {
                // For DIRECT_CLUB transfers
                if (request.getTransferType() == TransferRequest.TransferType.DIRECT_CLUB) {
                    // Only destination manager can purchase
                    if (currentUser.getClubId().equals(request.getDestinationClubId())) {
                        holder.requestTransferButton.setVisibility(View.VISIBLE);
                        holder.requestTransferButton.setEnabled(true);
                        holder.requestTransferButton.setText("Purchase");
                        holder.requestTransferButton.setOnClickListener(v -> showPurchaseDialog(request, player));
                        android.util.Log.d("TransferMarket", "  -> Purchase button ENABLED (destination manager)");
                    } else {
                        // Other managers can see but not purchase
                        holder.requestTransferButton.setVisibility(View.VISIBLE);
                        holder.requestTransferButton.setEnabled(false);
                        holder.requestTransferButton.setText("Reserved");
                        holder.requestTransferButton.setAlpha(0.5f);
                        android.util.Log.d("TransferMarket", "  -> Purchase button DISABLED (not destination manager)");
                    }
                } else {
                    // GENERAL_MARKET - anyone except source club can purchase
                    if (!currentUser.getClubId().equals(request.getSourceClubId())) {
                        holder.requestTransferButton.setVisibility(View.VISIBLE);
                        holder.requestTransferButton.setEnabled(true);
                        holder.requestTransferButton.setText("Purchase");
                        holder.requestTransferButton.setAlpha(1.0f);
                        holder.requestTransferButton.setOnClickListener(v -> showPurchaseDialog(request, player));
                        android.util.Log.d("TransferMarket", "  -> Purchase button ENABLED (general market)");
                    } else {
                        holder.requestTransferButton.setVisibility(View.GONE);
                    }
                }
            } else {
                holder.requestTransferButton.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return filteredMarketRequests.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView marketPlayerName, marketPlayerPosition, marketCurrentClub, marketTransferFee;
            TextView marketPlayerAge, marketPlayerJersey;
            Button requestTransferButton;

            ViewHolder(View itemView) {
                super(itemView);
                marketPlayerName = itemView.findViewById(R.id.marketPlayerName);
                marketPlayerPosition = itemView.findViewById(R.id.marketPlayerPosition);
                marketCurrentClub = itemView.findViewById(R.id.marketCurrentClub);
                marketTransferFee = itemView.findViewById(R.id.marketTransferFee);
                marketPlayerAge = itemView.findViewById(R.id.marketPlayerAge);
                marketPlayerJersey = itemView.findViewById(R.id.marketPlayerJersey);
                requestTransferButton = itemView.findViewById(R.id.requestTransferButton);
            }
        }
    }
}
