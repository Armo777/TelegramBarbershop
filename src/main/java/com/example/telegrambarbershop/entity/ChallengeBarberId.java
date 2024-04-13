package com.example.telegrambarbershop.entity;

import jakarta.persistence.*;

@Entity
public class ChallengeBarberId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "barberId_f", nullable = false)
    private Integer barberId;

    public ChallengeBarberId(Integer id, Integer barberId) {
        this.id = id;
        this.barberId = barberId;
    }

    public ChallengeBarberId() {

    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getBarberId() {
        return barberId;
    }

    public void setBarberId(Integer barberId) {
        this.barberId = barberId;
    }
}
