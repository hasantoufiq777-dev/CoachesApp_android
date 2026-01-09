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
import com.example.coachesapp_android.database.FirebaseTransferRequestRepository;
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

        ArrayAdapter<String> posAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, positions);
        posAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
                // Get all transfer requests with IN_MARKET status
                allMarketRequests = transferRequestRepository.findInMarket();
                
                // Load all players for reference
                allPlayers = playerRepository.findAll();

                filterMarketPlayers();

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (filteredMarketRequests.isEmpty()) {
                        emptyText.setVisibility(View.VISIBLE);
                        marketPlayersRecyclerView.setVisibility(View.GONE);
                    } else {
                        emptyText.setVisibility(View.GONE);
                        marketPlayersRecyclerView.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading market: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void filterMarketPlayers() {
        filteredMarketRequests.clear();

        if (selectedPosition == null) {
            filteredMarketRequests.addAll(allMarketRequests);
        } else {
            for (TransferRequest tr : allMarketRequests) {
                Player player = getPlayerById(tr.getPlayerId());
                if (player != null && player.getPosition() == selectedPosition) {
                    filteredMarketRequests.add(tr);
                }
            }
        }

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
        for (Player p : allPlayers) {
            if (p.getId().equals(playerId)) {
                return p;
            }
        }
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
            // Update the existing market request with destination club and complete it
            marketRequest.setDestinationClubId(currentUser.getClubId());
            
            // Try to get club name
            try {
                com.example.coachesapp_android.model.Club club = clubRepository.findById(currentUser.getClubId());
                if (club != null) {
                    marketRequest.setDestinationClubName(club.getClubName());
                }
            } catch (Exception e) {
                // Ignore if club name can't be fetched
            }
            
            marketRequest.setTransferFee(releaseFee);
            marketRequest.setStatus(TransferRequest.TransferStatus.COMPLETED);
            marketRequest.setCompletedDate(LocalDateTime.now());
            
            boolean success = transferRequestRepository.update(marketRequest);
            
            if (success && player != null) {
                // Update player's club
                player.setClubId(currentUser.getClubId());
                playerRepository.update(player);
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
            Player player = getPlayerById(request.getPlayerId());

            if (player != null) {
                holder.marketPlayerName.setText(player.getName());
                holder.marketPlayerPosition.setText(player.getPosition() != null ? player.getPosition().name() : "N/A");
                holder.marketPlayerAge.setText("Age: " + player.getAge());
                holder.marketPlayerJersey.setText("Jersey: #" + player.getJersey());
            } else {
                holder.marketPlayerName.setText(request.getPlayerName() != null ? request.getPlayerName() : "Unknown Player");
                holder.marketPlayerPosition.setText("N/A");
                holder.marketPlayerAge.setText("Age: N/A");
                holder.marketPlayerJersey.setText("Jersey: N/A");
            }

            holder.marketCurrentClub.setText("Current Club: " + (request.getSourceClubName() != null ? request.getSourceClubName() : "Unknown"));
            
            // Show release fee if available, otherwise show transfer fee
            Double displayFee = request.getReleaseFee() != null ? request.getReleaseFee() : request.getTransferFee();
            holder.marketTransferFee.setText("Release Fee: $" + (displayFee != null ? String.format("%.2f", displayFee) : "0"));

            // Only show request button for managers (not admin or players)
            if (currentUser.getRole() == Role.CLUB_MANAGER && currentUser.getClubId() != null) {
                // Don't allow purchasing from same club
                if (!currentUser.getClubId().equals(request.getSourceClubId())) {
                    holder.requestTransferButton.setVisibility(View.VISIBLE);
                    holder.requestTransferButton.setOnClickListener(v -> showPurchaseDialog(request, player));
                } else {
                    holder.requestTransferButton.setVisibility(View.GONE);
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
