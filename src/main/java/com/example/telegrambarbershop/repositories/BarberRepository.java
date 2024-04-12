package com.example.telegrambarbershop.repositories;

import com.example.telegrambarbershop.entity.Barber;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BarberRepository extends CrudRepository<Barber, Integer> {
    List<Barber> findBySpecialty(String specialty);
    List<Barber> findAll();
}
