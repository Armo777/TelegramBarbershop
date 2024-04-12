package com.example.telegrambarbershop.entity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Barber {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String specialty;

    @Column(name = "name_f", nullable = false)
    private String name;

    @Column(name = "phoneNumber_f", nullable = false)
    private String phoneNumber;

    @Column(name = "Rating_f", nullable = false)
    private Double Rating;


    public Barber(String name, String phoneNumber, String email, String address) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.Rating = Rating;
        // Инициализация остальных полей при необходимости
    }

    public Barber() {

    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Double getRating() {
        return Rating;
    }

    public void setRating(Double rating) {
        Rating = rating;
    }
}


