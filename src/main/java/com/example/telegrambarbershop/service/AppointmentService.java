package com.example.telegrambarbershop.service;

import com.example.telegrambarbershop.entity.Appointment;
import com.example.telegrambarbershop.repositories.AppointmentRepository;
import org.jvnet.hk2.annotations.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;

    @Autowired
    public AppointmentService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
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
