package com.example.telegrambarbershop.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
public class Service {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "serviceName_f", nullable = false)
    private String serviceName;

    @Column(name = "bigDecimal_f", nullable = false)
    private BigDecimal price;

    @Column(name = "duration_minutes_f", nullable = false)
    private int durationMinutes;

    public Service(Integer id, String serviceName, BigDecimal price, int durationMinutes) {
        this.id = id;
        this.serviceName = serviceName;
        this.price = price;
        this.durationMinutes = durationMinutes;
    }

    public Service() {

    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
