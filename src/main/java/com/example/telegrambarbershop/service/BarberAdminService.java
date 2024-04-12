package com.example.telegrambarbershop.service;

import com.example.telegrambarbershop.entity.Appointment;
import com.example.telegrambarbershop.entity.BarberAdmin;
import com.example.telegrambarbershop.repositories.AppointmentRepository;
import com.example.telegrambarbershop.repositories.BarberAdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BarberAdminService {
    @Autowired
    private BarberAdminRepository adminRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;

    public boolean authenticate(String username, String password) {
        BarberAdmin admin = adminRepository.findByUsername(username);
        return admin != null && admin.getPassword().equals(password);
    }

    public List<Appointment> getAppointmentsForBarber(long barberId) {
        // Получить записи для данного барбера из репозитория
        return appointmentRepository.findAll();
    }
}
