package com.example.telegrambarbershop.repositories;

import com.example.telegrambarbershop.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Integer> {
    Service findByServiceName(String serviceName);
}
