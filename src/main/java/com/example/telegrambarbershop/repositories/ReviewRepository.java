package com.example.telegrambarbershop.repositories;

import com.example.telegrambarbershop.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
}
