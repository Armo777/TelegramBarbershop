package com.example.telegrambarbershop.repositories;

import com.example.telegrambarbershop.entity.Barber;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BarberRepository extends CrudRepository<Barber, Integer> {
    List<Barber> findBySpecialty(String specialty);
    List<Barber> findAll();

    @Query("SELECT b FROM Barber b JOIN b.barberAdmins ba WHERE ba.id = :adminId")
    Barber findByAdminId(@Param("adminId") int adminId);
}
