package com.example.coachesapp_android.database;

import com.example.coachesapp_android.model.Club;
import java.util.List;


public interface IClubRepository {
    Club save(Club club);
    Club findById(int id);
    List<Club> findAll();
    Club findByName(String name);
    boolean delete(int id);
}
