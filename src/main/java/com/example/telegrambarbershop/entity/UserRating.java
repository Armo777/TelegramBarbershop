package com.example.telegrambarbershop.entity;

public class UserRating {
    private final double rating;
    private final Integer appointmentId;

//    public Integer getBarberId() {
//        return barberId;
//    }

    //private final Integer barberId;

    public UserRating(double rating, Integer appointmentId) {
        this.rating = rating;
        this.appointmentId = appointmentId;
        //this.barberId = barberId;
    }

    public double getRating() {
        return rating;
    }

    public Integer getAppointmentId() {
        return appointmentId;
    }
}
