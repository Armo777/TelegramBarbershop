package com.example.telegrambarbershop.controller;

import com.example.telegrambarbershop.entity.Appointment;
import com.example.telegrambarbershop.entity.Barber;
import com.example.telegrambarbershop.entity.Service;
import com.example.telegrambarbershop.repositories.AppointmentRepository;
import com.example.telegrambarbershop.repositories.BarberRepository;
import com.example.telegrambarbershop.repositories.ServiceRepository;
import com.example.telegrambarbershop.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    @Autowired
    private BarberRepository barberRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentService appointmentService;
    //@Autowired
    //private TelegramBotController telegramBotController;
    @GetMapping("/availableTimeSlots")
    public Map<LocalDate, List<String>> getAvailableTimeSlots(
            @RequestParam int barberId,
            @RequestParam int serviceId) {
        Barber barber = barberRepository.findById(barberId).orElse(null);
        Service service = serviceRepository.findById(serviceId).orElse(null);
        if (barber == null || service == null) {
            return new HashMap<>();
        }

        LocalDate startDate = LocalDate.now().plusDays(1); // Start from tomorrow
        LocalDate endDate = startDate.plusMonths(1); // Until one month from tomorrow
        List<Appointment> appointments = appointmentRepository.findByBarberId(barberId);
        Map<LocalDate, List<String>> availableTimeSlotsMap = new HashMap<>();

        for (LocalDate currentDate = startDate; currentDate.isBefore(endDate); currentDate = currentDate.plusDays(1)) {
            LocalDateTime startTime = LocalDateTime.of(currentDate, LocalTime.of(9, 0));
            LocalDateTime endTime = LocalDateTime.of(currentDate, LocalTime.of(18, 0));
            List<String> availableTimeSlots = new ArrayList<>();
            LocalDateTime currentSlot = startTime;

            while (currentSlot.isBefore(endTime)) {
                boolean isSlotAvailable = true;

                for (Appointment appointment : appointments) {
                    if (currentSlot.equals(appointment.getAppointmentDateTime()) ||
                            (currentSlot.isAfter(appointment.getAppointmentDateTime().minusHours(1)) &&
                                    currentSlot.isBefore(appointment.getAppointmentDateTime().plusHours(1)))) {
                        isSlotAvailable = false;
                        break;
                    }
                }

                if (isSlotAvailable) {
                    availableTimeSlots.add(formatDateTime(currentSlot));
                }

                currentSlot = currentSlot.plusHours(1);
            }

            // Если на текущую дату нет записей, добавляем пустой список
            if (availableTimeSlots.isEmpty()) {
                availableTimeSlotsMap.put(currentDate, new ArrayList<>());
            } else {
                availableTimeSlotsMap.put(currentDate, availableTimeSlots);
            }
        }

        return availableTimeSlotsMap;
    }

    public List<String> getAvailableTimeSlotsForDay(int barberId, LocalDate date) {
        LocalDateTime startTime = LocalDateTime.of(date, LocalTime.of(9, 0));
        LocalDateTime endTime = LocalDateTime.of(date, LocalTime.of(18, 0));

        List<Appointment> appointments = appointmentRepository.findByBarberIdAndAppointmentDateTimeBetween((long) barberId, startTime, endTime);

        List<String> availableTimeSlots = new ArrayList<>();
        LocalDateTime currentSlot = startTime;
        while (currentSlot.isBefore(endTime)) {
            boolean isSlotAvailable = true;
            for (Appointment appointment : appointments) {
                if (currentSlot.equals(appointment.getAppointmentDateTime()) ||
                        (currentSlot.isAfter(appointment.getAppointmentDateTime().minusHours(1)) &&
                                currentSlot.isBefore(appointment.getAppointmentDateTime().plusHours(1)))) {
                    isSlotAvailable = false;
                    break;
                }
            }
            if (isSlotAvailable) {
                availableTimeSlots.add(formatDateTime(currentSlot));
            }
            currentSlot = currentSlot.plusHours(1);
        }

        return availableTimeSlots;
    }

    private List<Appointment> getAppointmentsForDay(Long barberId, LocalDate day) {
        LocalDateTime startOfDay = day.atStartOfDay();
        LocalDateTime endOfDay = day.atTime(LocalTime.MAX);
        return appointmentRepository.findByBarberIdAndAppointmentDateTimeBetween(barberId, startOfDay, endOfDay);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return dateTime.format(formatter);
    }

    // Метод для создания записи на прием
    public boolean createAppointment(LocalDateTime appointmentDateTime, Integer barberId, Integer serviceId, String name) {
        // Получаем информацию о барбере и услуге
        Barber barber = barberRepository.findById(barberId).orElse(null);
        Service service = serviceRepository.findById(serviceId).orElse(null);

        // Проверяем, найдены ли барбер и услуга
        if (barber == null || service == null) {
            return false; // Возвращаем false, если барбер или услуга не найдены
        }

        // Создаем новую запись на прием
        Appointment appointment = new Appointment();
        appointment.setAppointmentDateTime(appointmentDateTime);
        appointment.setBarber(barber);
        appointment.setService(service);
        appointment.setName(name);

        // Сохраняем запись в репозитории
        appointmentRepository.save(appointment);

        //String answer = "Ваша запись создана, будем вас ждать!";
        //telegramBotController.sendMessage(telegramBotController.update.getCallbackQuery(), answer);

        return true; // Возвращаем true, если запись на прием успешно создана
    }
}
