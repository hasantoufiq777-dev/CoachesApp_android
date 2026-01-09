package com.example.coachesapp_android.database;

import com.example.coachesapp_android.model.User;
import java.util.List;


public interface IUserRepository {
    User save(User user);

    User findByUsername(String username);

    User findByUsernameAndPassword(String username, String password);

    User findByPlayerId(int playerId);

    User findByEmail(String email);

    List<User> findAll();

    boolean delete(Integer userId);

}
