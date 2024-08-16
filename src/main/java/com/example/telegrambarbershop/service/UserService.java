package com.example.telegrambarbershop.service;

import com.example.telegrambarbershop.entity.User;
import com.example.telegrambarbershop.repositories.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void registerUser(long chatId) {
        if (userRepository.findByChatId(chatId) == null) {
            User user = new User();
            user.setChatId(chatId);
            userRepository.save(user);
        }
    }
}
