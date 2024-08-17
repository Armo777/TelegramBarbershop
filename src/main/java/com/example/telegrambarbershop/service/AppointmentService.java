package com.example.telegrambarbershop.service;

import com.example.telegrambarbershop.controller.TelegramBotController;
import com.example.telegrambarbershop.entity.Appointment;
import com.example.telegrambarbershop.entity.Barber;
import com.example.telegrambarbershop.repositories.AppointmentRepository;
import com.example.telegrambarbershop.repositories.BarberRepository;
import com.example.telegrambarbershop.repositories.ServiceRepository;
import lombok.extern.log4j.Log4j;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Log4j
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final BarberRepository barberRepository;
    private final ServiceRepository serviceRepository;
    private final TelegramBotController telegramBotController;

    @Autowired
    public AppointmentService(AppointmentRepository appointmentRepository, BarberRepository barberRepository, ServiceRepository serviceRepository, TelegramBotController telegramBotController) {
        this.appointmentRepository = appointmentRepository;
        this.barberRepository = barberRepository;
        this.serviceRepository = serviceRepository;
        this.telegramBotController = telegramBotController;
    }

    @Transactional(readOnly = true)
    public List<Appointment> getAllAppointmentsWithDetails() {
        List<Appointment> appointments = appointmentRepository.findAll();
        // Инициализация ленивых полей
        for (Appointment appointment : appointments) {
            appointment.getBarber().getName(); // Инициализация ленивого поля
            appointment.getService().getServiceName(); // Инициализация ленивого поля
        }
        return appointments;
    }

    public void registrationRecord(long chatId, String name, String appointmentDate, String appointmentTime) {
        if (name != null) {
            sendAppointmentConfirmation(chatId, name, appointmentDate, appointmentTime);
        } else {
            log.error("Получено пустое имя пользователя.");
        }
    }

    public void sendAppointmentConfirmation(Long chatId, String userName, String appointmentDate, String appointmentTime) {
        String confirmationMessage = String.format(
                "Вас приветствует Барбершоп CROP. Я система оповещения R2-D2.\n\n" +
                        "Уважаемый(ая) %s, ждем вас в Барбершоп CROP %s в %s.\n\n" +
                        "Если возникла аварийная ситуация и вы не сможете явиться в назначенное время, то наберите +79515161121.",
                userName, appointmentDate, appointmentTime
        );
        telegramBotController.sendMessage(chatId, confirmationMessage);
    }

    public List<Appointment> getAppointmentsForBarber(Long barberId) {
        return appointmentRepository.findByBarberId(barberId);
    }
}
