package com.example.telegrambarbershop.service;

import com.example.telegrambarbershop.entity.Barber;
import com.example.telegrambarbershop.repositories.BarberRepository;
import org.jvnet.hk2.annotations.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Service
public class BarberService {
    @Autowired
    private BarberRepository barberRepository;


    public BarberService(BarberRepository barberRepository) {
        this.barberRepository = barberRepository;
    }

    public List<Barber> getAllBarbers() {
        Iterable<Barber> iterable = barberRepository.findAll();
        List<Barber> barbers = new ArrayList<>();
        iterable.forEach(barbers::add); // Преобразование Iterable в List
        return barbers;
    }

    public Barber saveAppointment(Barber appointment) {

        return barberRepository.save(appointment);
    }

    public void deleteBarber(Integer id) {
        barberRepository.deleteById(id);
    }
}
