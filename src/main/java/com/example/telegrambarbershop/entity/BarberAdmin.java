package com.example.telegrambarbershop.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class BarberAdmin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "username_f", nullable = false)
    private String username;
    @Column(name = "password_f", nullable = false)
    private String password;
    @ManyToOne
    @JoinColumn(name = "barber_f", nullable = false)
    private Barber barber;

    public BarberAdmin(Integer id, String username, String password, Barber barber) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.barber = barber;
    }

    public BarberAdmin() {

    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Barber getBarber() {
        return barber;
    }

    public void setBarber(Barber barber) {
        this.barber = barber;
    }
}
