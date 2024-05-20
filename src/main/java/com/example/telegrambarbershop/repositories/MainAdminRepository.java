package com.example.telegrambarbershop.repositories;

import com.example.telegrambarbershop.entity.MainAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MainAdminRepository extends JpaRepository<MainAdmin, Long> {
    MainAdmin findByUsername(String username);
}
