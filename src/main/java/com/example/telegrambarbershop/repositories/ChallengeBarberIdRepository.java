package com.example.telegrambarbershop.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.telegrambarbershop.entity.ChallengeBarberId;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChallengeBarberIdRepository extends JpaRepository<ChallengeBarberId, Integer> {
    List<ChallengeBarberId> findAll();
}
