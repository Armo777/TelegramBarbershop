package com.example.telegrambarbershop.entity;

import com.example.telegrambarbershop.repositories.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ReviewDTO {

    private Integer id;
    private Double rating;
    private String comment;
    private LocalDateTime createdAt;
    private BarberDTO barber;
    private String userName;

    public ReviewDTO(Integer id, Double rating, String comment, LocalDateTime createdAt, BarberDTO barber, String userName) {
        this.id = id;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
        this.barber = barber;
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public BarberDTO getBarber() {
        return barber;
    }

    public void setBarber(BarberDTO barber) {
        this.barber = barber;
    }
}
