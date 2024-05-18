package com.example.telegrambarbershop.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "appointmentDateTime_f", nullable = false)
    private LocalDateTime appointmentDateTime;

    @ManyToOne
    @JoinColumn(name = "barber_id", nullable = false)
    private Barber barber;

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(name = "name_f", nullable = false)
    private String name;

    public Appointment(Integer id, LocalDateTime appointmentDateTime, Barber barber, Service service, String name) {
        this.id = id;
        this.appointmentDateTime = appointmentDateTime;
        this.barber = barber;
        this.service = service;
        this.name = name;
    }

    public String getNameUser() {
        return name;
    }

    public void setName(String nameUser) {
        this.name = nameUser;
    }

    public Appointment() {

    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDateTime getAppointmentDateTime() {
        return appointmentDateTime;
    }

    public void setAppointmentDateTime(LocalDateTime appointmentDateTime) {
        this.appointmentDateTime = appointmentDateTime;
    }

    public Barber getBarber() {
        return barber;
    }

    public void setBarber(Barber barber) {
        this.barber = barber;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public String getName() {
        return name;
    }
}
