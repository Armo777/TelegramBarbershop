package com.example.telegrambarbershop.repositories;

import com.example.telegrambarbershop.entity.Appointment;
import com.example.telegrambarbershop.entity.Barber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {
    List<Appointment> findAll();

    Appointment save(Appointment appointment);

    List<Appointment> findByBarberId(Long barberId);

    List<Appointment> findByBarberId(int barberId);

    @Query("SELECT a FROM Appointment a JOIN FETCH a.service WHERE a.barber.id = :barberId AND a.appointmentDateTime BETWEEN :startOfDay AND :endOfDay")
    List<Appointment> findByBarberIdAndAppointmentDateTimeBetween(@Param("barberId") Long barberId, @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT a FROM Appointment a WHERE a.appointmentDateTime <= :notificationTime AND a.reviewRequestSent = false")
    List<Appointment> findCompletedAppointmentsWithNoNotification(@Param("notificationTime") LocalDateTime notificationTime);
}
