package com.example.telegrambarbershop.repositories;

import com.example.telegrambarbershop.entity.Appointment;
import com.example.telegrambarbershop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByChatId(Long chatId);
    //User findByTelegramUserId(String telegramUserId);
    //List<User> findByAppointmentDateTimeBefore(LocalDateTime dateTime);
    Optional<User> findById(Long chatId);
}
