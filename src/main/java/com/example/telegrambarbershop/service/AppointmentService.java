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

    public List<Appointment> createMonthlyAppointments(Long barberId) {
        Barber barber = barberRepository.findById(barberId.intValue()).orElseThrow(() -> new IllegalArgumentException("Недопустимый ID барбера"));
        com.example.telegrambarbershop.entity.Service defaultService = serviceRepository.findById(1).orElseThrow(() -> new IllegalArgumentException("Invalid service ID"));
        List<Appointment> appointments = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int day = 0; day < 30; day++) {
            LocalDateTime date = now.plusDays(day).with(LocalTime.of(9, 0));
            for (int hour = 9; hour < 23; hour++) {
                LocalDateTime appointmentTime = date.withHour(hour);
                Appointment appointment = new Appointment(null, appointmentTime, barber, defaultService, "Пользователь по умолчанию");
                appointments.add(appointmentRepository.save(appointment));
            }
        }
        return appointments;
    }

    @Transactional
    public List<Appointment> getAppointmentsForDay(Long barberId, LocalDateTime day) {
        LocalDateTime startOfDay = day.with(LocalTime.MIN);
        LocalDateTime endOfDay = day.with(LocalTime.MAX);
        List<Appointment> appointments = appointmentRepository.findByBarberIdAndAppointmentDateTimeBetween(barberId, startOfDay, endOfDay);
        appointments.forEach(appointment -> Hibernate.initialize(appointment.getService()));
        return appointments;
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

    public Appointment createAppointment(Appointment appointment) {
        // Логика создания записи и сохранения в репозитории
        return appointmentRepository.save(appointment);
    }

    public List<Appointment> getAppointmentsByBarber(Long barberId) {
        // Логика получения записей по ID барбера
        // appointmentRepository.findByBarberId(barberId);
        return appointmentRepository.findAll(); // Пример
    }

    public List<Appointment> getAppointmentsForBarber(Long barberId) {
        return appointmentRepository.findByBarberId(barberId);
    }

    public String formatAppointmentsForBarber(List<Appointment> appointments) {
        StringBuilder messageText = new StringBuilder();
        messageText.append("Список записей:\n");

        for (Appointment appointment : appointments) {
            messageText.append("Дата и время: ").append(appointment.getAppointmentDateTime().toString()).append("\n");
            messageText.append("Услуга: ").append(appointment.getService().getServiceName()).append("\n");
            messageText.append("Имя клиента: ").append(appointment.getName()).append("\n\n");
        }
        return messageText.toString();
    }
}
