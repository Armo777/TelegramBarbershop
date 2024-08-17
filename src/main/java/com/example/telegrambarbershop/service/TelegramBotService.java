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
}
