package com.example.telegrambarbershop.repositories;

import com.example.telegrambarbershop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByChatId(Long chatId);
}
