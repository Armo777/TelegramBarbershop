package com.example.telegrambarbershop.service;

import com.example.telegrambarbershop.controller.TelegramBotController;
import com.example.telegrambarbershop.entity.Appointment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.generics.TelegramBot;

import java.util.List;

@Service
public class TelegramBotService {
    private final AppointmentService appointmentService;
    private final TelegramBotController botController;

    @Autowired
    public TelegramBotService(AppointmentService appointmentService, TelegramBot telegramBot, TelegramBotController botController) {
        this.appointmentService = appointmentService;
        this.botController = botController;
    }

    // Метод для обработки команды на создание записи через Telegram бота
//    public void handleCreateAppointmentCommand(Long userId, Appointment appointment) {
//        // Логика создания записи
//        Appointment createdAppointment = appointmentService.createAppointment(appointment);
//
//        // Отправка ответа пользователю через Telegram бота
//        botController.sendMessage(userId, "Запись успешно создана!");
//    }
//
//    // Метод для обработки команды на получение записей для определенного барбера через Telegram бота
//    public void handleGetAppointmentsByBarberCommand(Long userId, Long barberId) {
//        // Логика получения записей для барбера
//        List<Appointment> appointments = appointmentService.getAppointmentsByBarber(barberId);
//
//        // Отправка списка записей пользователю через Telegram бота
//        botController.sendMessage(userId, "Список записей: " + appointments.toString());
//    }
//
//    public void handleAppointmentCommand(Long chatId) {
//        // Логика обработки команды "Запись на услуги"
//        // Например, вы можете предложить список барберов и услуг для записи
//
//        // Предлагаем выбрать барбера и услугу
//        String messageText = "Выберите барбера и услугу для записи:";
//        botController.sendMessage(chatId, messageText);
//    }
}
