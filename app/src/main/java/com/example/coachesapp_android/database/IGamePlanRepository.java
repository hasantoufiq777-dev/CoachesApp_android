package com.example.coachesapp_android.database;

import com.example.coachesapp_android.model.GamePlan;

public interface IGamePlanRepository {
    GamePlan save(GamePlan gamePlan);
    GamePlan findByClubId(int clubId);
    boolean update(GamePlan gamePlan);
    boolean delete(int clubId);
}
