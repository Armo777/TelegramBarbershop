package com.example.telegrambarbershop.entity;

public class UserReviewSession {
    private Long chatId;
    private Double rating;
    private Integer appointmentId;
    private String comment;
    private boolean awaitingReview;

    private Long barberId;

    public UserReviewSession(Long chatId, Double rating, Integer appointmentId) {
        this.barberId = barberId;
        this.chatId = chatId;
        this.rating = rating;
        this.appointmentId = appointmentId;
        this.awaitingReview = true;
    }

    public Integer getBarberId() {
        return Math.toIntExact(barberId);
    }

    public void setBarberId(Long barberId) {
        this.barberId = barberId;
    }

    // Метод, который проверяет, ожидается ли отзыв
    public boolean isAwaitingReview() {
        return awaitingReview;
    }

    // Геттеры и сеттеры
    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Integer appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
