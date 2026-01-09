package com.example.coachesapp_android.database;

import com.example.coachesapp_android.model.Player;
import java.util.List;

/**
 * Repository interface for Player data operations.
 * Implement this interface with Firebase or another data backend.
 * Example: PlayerRepositoryFirebase
 */
public interface IPlayerRepository {
    Player save(Player player);
    Player findById(int id);
    List<Player> findAll();
    List<Player> findByClubId(int clubId);
    boolean update(Player player);
    boolean delete(int id);
}
