package com.example.coachesapp_android.repository;

import com.example.coachesapp_android.model.TransferRequest;
import java.util.List;

public interface ITransferRequestRepository {
    /**
     * Save a transfer request to the database
     * @param transferRequest The transfer request to save
     * @return The saved transfer request with generated ID, or null if failed
     */
    TransferRequest save(TransferRequest transferRequest);

    /**
     * Find a transfer request by its ID
     * @param id The ID of the transfer request
     * @return The transfer request if found, null otherwise
     */
    TransferRequest findById(Integer id);

    /**
     * Get all transfer requests
     * @return List of all transfer requests
     */
    List<TransferRequest> findAll();

    /**
     * Get all transfer requests by player ID
     * @param playerId The player ID
     * @return List of transfer requests for the player
     */
    List<TransferRequest> findByPlayerId(Integer playerId);

    /**
     * Get all transfer requests by source club ID
     * @param clubId The source club ID
     * @return List of transfer requests from the club
     */
    List<TransferRequest> findBySourceClubId(Integer clubId);

    /**
     * Get all transfer requests by destination club ID
     * @param clubId The destination club ID
     * @return List of transfer requests to the club
     */
    List<TransferRequest> findByDestinationClubId(Integer clubId);

    /**
     * Get all transfer requests with a specific status
     * @param status The transfer status
     * @return List of transfer requests with that status
     */
    List<TransferRequest> findByStatus(TransferRequest.TransferStatus status);

    /**
     * Get all transfer requests in the market (status = IN_MARKET)
     * @return List of available transfer requests
     */
    List<TransferRequest> findInMarket();

    /**
     * Update a transfer request
     * @param transferRequest The transfer request to update
     * @return true if successful, false otherwise
     */
    boolean update(TransferRequest transferRequest);

    /**
     * Delete a transfer request by ID
     * @param id The ID of the transfer request to delete
     * @return true if successful, false otherwise
     */
    boolean delete(Integer id);
}
