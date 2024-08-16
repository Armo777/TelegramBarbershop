package com.example.telegrambarbershop.repositories;

import com.example.telegrambarbershop.entity.WorkingDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkingDayRepository extends JpaRepository<WorkingDay, Integer> {
    boolean existsByDate(LocalDate date);
    WorkingDay findByDate(LocalDate date);
}
