package com.example.telegrambarbershop.service;

import com.example.telegrambarbershop.controller.TelegramBotController;
import com.example.telegrambarbershop.entity.Barber;
import com.example.telegrambarbershop.entity.BarberAdmin;
import com.example.telegrambarbershop.entity.MainAdmin;
import com.example.telegrambarbershop.entity.User;
import com.example.telegrambarbershop.repositories.BarberAdminRepository;
import com.example.telegrambarbershop.repositories.BarberRepository;
import com.example.telegrambarbershop.repositories.MainAdminRepository;
import com.example.telegrambarbershop.repositories.UserRepository;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
@Log4j
public class AdminService {

    @Autowired
    private MainAdminRepository mainAdminRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TelegramBotController telegramBotController;

    @Autowired
    private BarberAdminRepository barberAdminRepository;

    @Autowired
    private BarberRepository barberRepository;

    public boolean isMainAdminCredentials(String username, String password) {
        MainAdmin mainAdmin = mainAdminRepository.findByUsername(username);
        return mainAdmin != null && mainAdmin.getPassword().equals(password);
    }

    public boolean isMainAdmin(long chatId) {
        User user = userRepository.findByChatId(chatId);
        return user != null && user.isAdmin();
    }

    public void adminLogin(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Введите логин и пароль в формате: логин_пароль");

        try {
            telegramBotController.execute(message);
        } catch (TelegramApiException e) {
            log.error("Произошла ошибка: " + e.getMessage());
        }
    }

    public void mainAdminLogin(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));

        if (!isMainAdmin(chatId)) {
            message.setText("Пожалуйста, введите логин и пароль главного администратора в формате: логин_пароль");
            try {
                telegramBotController.execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }

        if (isMainAdmin(chatId)) {
            message.setText("Добро пожаловать, главный администратор!\n\nВыберите команду администратора:");
            message.setReplyMarkup(telegramBotController.getMainAdminCommandsKeyboard());
            try {
                telegramBotController.execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    public Barber validateAdminCredentials(String username, String password) {
        BarberAdmin admin = barberAdminRepository.findByUsername(username);
        if (admin != null && admin.getPassword().equals(password)) {
            return barberRepository.findByAdminId(admin.getId());
        }
        return null;
    }
}
