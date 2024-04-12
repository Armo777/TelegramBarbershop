package com.example.telegrambarbershop.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
public class Service {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "serviceName_f", nullable = false)
    private String serviceName;

    @Column(name = "bigDecimal_f", nullable = false)
    private BigDecimal price;

    public Service(Integer id, String serviceName, BigDecimal price) {
        this.id = id;
        this.serviceName = serviceName;
        this.price = price;
    }

    public Service() {

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
