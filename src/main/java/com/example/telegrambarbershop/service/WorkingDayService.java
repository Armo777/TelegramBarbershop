package com.example.telegrambarbershop.service;

import com.example.telegrambarbershop.entity.WorkingDay;
import com.example.telegrambarbershop.repositories.WorkingDayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WorkingDayService {
    @Autowired
    private WorkingDayRepository workingDayRepository;

    public void saveWorkingDay(LocalDate date) {
        // Проверяем, существует ли уже запись на этот день
        if (!workingDayRepository.existsByDate(date)) {
            WorkingDay workingDay = new WorkingDay();
            workingDay.setDate(date);
            workingDayRepository.save(workingDay);
        }
    }

    public List<LocalDate> getAllWorkingDays() {
        return workingDayRepository.findAll().stream()
                .map(WorkingDay::getDate)
                .collect(Collectors.toList());
    }
}
