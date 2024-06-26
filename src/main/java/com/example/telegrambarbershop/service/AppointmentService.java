package com.example.telegrambarbershop.service;

import com.example.telegrambarbershop.entity.Appointment;
import com.example.telegrambarbershop.entity.Barber;
import com.example.telegrambarbershop.repositories.AppointmentRepository;
import com.example.telegrambarbershop.repositories.BarberRepository;
import com.example.telegrambarbershop.repositories.ServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final BarberRepository barberRepository;
    private final ServiceRepository serviceRepository;
    @Autowired
    public AppointmentService(AppointmentRepository appointmentRepository, BarberRepository barberRepository, ServiceRepository serviceRepository) {
        this.appointmentRepository = appointmentRepository;
        this.barberRepository = barberRepository;
        this.serviceRepository = serviceRepository;
    }

    public List<Appointment> createMonthlyAppointments(Long barberId) {
        Barber barber = barberRepository.findById(barberId.intValue()).orElseThrow(() -> new IllegalArgumentException("Invalid barber ID"));
        com.example.telegrambarbershop.entity.Service defaultService = serviceRepository.findById(1).orElseThrow(() -> new IllegalArgumentException("Invalid service ID")); // Assuming a default service exists with ID 1
        List<Appointment> appointments = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int day = 0; day < 30; day++) {
            LocalDateTime date = now.plusDays(day).with(LocalTime.of(9, 0));
            for (int hour = 9; hour < 17; hour++) {
                LocalDateTime appointmentTime = date.withHour(hour);
                Appointment appointment = new Appointment(null, appointmentTime, barber, defaultService, "Default User");
                appointments.add(appointmentRepository.save(appointment));
            }
        }
        return appointments;
    }

    public List<Appointment> getAppointmentsForDay(Long barberId, LocalDateTime day) {
        LocalDateTime startOfDay = day.with(LocalTime.MIN);
        LocalDateTime endOfDay = day.with(LocalTime.MAX);
        return appointmentRepository.findByBarberIdAndAppointmentDateTimeBetween(barberId, startOfDay, endOfDay);
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
}
