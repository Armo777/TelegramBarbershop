package com.example.telegrambarbershop.repositories;

import com.example.telegrambarbershop.entity.BarberAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BarberAdminRepository extends JpaRepository<BarberAdmin, Long> {
    BarberAdmin findByUsername(String username);
}
