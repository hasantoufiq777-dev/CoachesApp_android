package com.example.coachesapp_android.database;

import com.example.coachesapp_android.model.Club;
import java.util.List;

/**
 * Repository interface for Club data operations.
 * Implement this interface with Firebase or another data backend.
 */
public interface IClubRepository {
    Club save(Club club);
    Club findById(int id);
    List<Club> findAll();
    Club findByName(String name);
    boolean delete(int id);
}
