package com.example.telegrambarbershop.repositories;

import com.example.telegrambarbershop.entity.Appointment;
import com.example.telegrambarbershop.entity.Barber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {
    List<Appointment> findAll();

    //List<Appointment> findByBarberAndAppointmentTimeAfter(Barber barber, LocalDateTime appointmentDateTime);

    List<Appointment> findByBarberIdOrderByAppointmentDateTime(Integer barberId);

    Appointment save(Appointment appointment);

    List<Appointment> findByBarberAndAppointmentDateTimeAfter(Barber barber, LocalDateTime appointmentDateTime);

    List<Appointment> findByBarberId(Long barberId);

    //List<Appointment> findByAppointmentTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
}
