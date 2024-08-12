package com.example.telegrambarbershop.entity;

public class BarberDTO {
    private Integer id;
    private String name;
    private Double rating;

    public BarberDTO(Integer id, String name, Double rating) {
        this.id = id;
        this.name = name;
        this.rating = rating;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
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
}
