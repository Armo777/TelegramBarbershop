package com.example.telegrambarbershop.controller;

import com.example.telegrambarbershop.entity.Appointment;
import com.example.telegrambarbershop.entity.Barber;
import com.example.telegrambarbershop.entity.Service;
import com.example.telegrambarbershop.repositories.AppointmentRepository;
import com.example.telegrambarbershop.repositories.BarberRepository;
import com.example.telegrambarbershop.repositories.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    @Autowired
    private BarberRepository barberRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;
    //@Autowired
    //private TelegramBotController telegramBotController;
    @GetMapping("/availableTimeSlots")
    public List<String> getAvailableTimeSlots(
            @RequestParam LocalDateTime date,
            @RequestParam int barberId,
            @RequestParam int serviceId) {

        Barber barber = barberRepository.findById(barberId).orElse(null);
        Service service = serviceRepository.findById(serviceId).orElse(null);

        if (barber == null || service == null) {
            return new ArrayList<>();
        }

        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        LocalDateTime startTime = LocalDateTime.of(tomorrow.toLocalDate(), LocalTime.of(9, 0));
        LocalDateTime endTime = LocalDateTime.of(tomorrow.toLocalDate(), LocalTime.of(18, 0));

        List<Appointment> appointments = appointmentRepository.findAll();

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

    private String formatDateTime(LocalDateTime dateTime) {
        int year = dateTime.getYear();
        int month = dateTime.getMonthValue();
        int day = dateTime.getDayOfMonth();
        int hour = dateTime.getHour();
        int minute = dateTime.getMinute();

        String formattedDateTime = String.format("%04d-%02d-%02d %02d:%02d", year, month, day, hour, minute);
        return formattedDateTime;
    }

    // Метод для создания записи на прием
    public boolean createAppointment(LocalDateTime appointmentDateTime, Integer barberId, Integer serviceId, String nameUser) {
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
        appointment.setNameUser(nameUser);

        // Сохраняем запись в репозитории
        appointmentRepository.save(appointment);

        //String answer = "Ваша запись создана, будем вас ждать!";
        //telegramBotController.sendMessage(telegramBotController.update.getCallbackQuery(), answer);

        return true; // Возвращаем true, если запись на прием успешно создана
    }
}
