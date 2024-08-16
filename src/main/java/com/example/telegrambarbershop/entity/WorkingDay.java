package com.example.telegrambarbershop.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "working_days")
public class WorkingDay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

//    @Column(name = "barber_id", nullable = false)
//    private Integer barberId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

//    public Integer getBarberId() {
//        return barberId;
//    }
//
//    public void setBarberId(Integer barberId) {
//        this.barberId = barberId;
//    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
