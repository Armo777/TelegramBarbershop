package com.example.telegrambarbershop.entity;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Entity
@EqualsAndHashCode(exclude = {"barberAdmins", "reviews"})  // Исключаем ассоциации из equals и hashCode
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@ToString(exclude = "barberAdmins")
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
    private Double rating;

    @OneToMany(mappedBy = "barber", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<BarberAdmin> barberAdmins;

    @OneToMany(mappedBy = "barber", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonBackReference
    private Set<Review> reviews;

    public Barber(String name, String phoneNumber) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.rating = 0.0;
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

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
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
        return rating;
    }

    public void setRating(Double rating) {
        rating = rating;
    }

    public void updateRating() {
        this.rating = reviews.stream()
                .mapToDouble(Review::getRating)
                .average()
                .orElse(0.0);
    }
}


