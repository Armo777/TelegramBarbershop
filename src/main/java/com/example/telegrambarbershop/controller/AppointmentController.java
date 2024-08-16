package com.example.telegrambarbershop.controller;

import com.example.telegrambarbershop.entity.Appointment;
import com.example.telegrambarbershop.entity.Barber;
import com.example.telegrambarbershop.entity.Service;
import com.example.telegrambarbershop.entity.User;
import com.example.telegrambarbershop.repositories.AppointmentRepository;
import com.example.telegrambarbershop.repositories.BarberRepository;
import com.example.telegrambarbershop.repositories.ServiceRepository;
import com.example.telegrambarbershop.repositories.UserRepository;
import com.example.telegrambarbershop.service.AppointmentService;
import com.example.telegrambarbershop.service.WorkingDayService;
import jakarta.transaction.Transactional;
import org.hibernate.Hibernate;
import org.slf4j.ILoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkingDayService workingDayService;

    @GetMapping("/availableTimeSlots")
    public Map<LocalDate, List<String>> getAvailableTimeSlots(
            @RequestParam int barberId,
            @RequestParam int serviceId) {
        Barber barber = barberRepository.findById(barberId).orElse(null);
        Service service = serviceRepository.findById(serviceId).orElse(null);
        if (barber == null || service == null) {
            return new HashMap<>();
        }

        int serviceDuration = service.getDurationMinutes();  // Получаем длительность выполнения услуги
        int breakDuration = 5;  // Перерыв в минутах

        List<LocalDate> workingDays = workingDayService.getAllWorkingDays();

        LocalDate startDate = LocalDate.now().plusDays(0); // Начинаем с завтрашнего дня
        LocalDate endDate = startDate.plusMonths(1); // До одного месяца от завтрашнего дня
        List<Appointment> appointments = appointmentRepository.findByBarberId(barberId);
        Map<LocalDate, List<String>> availableTimeSlotsMap = new HashMap<>();

        for (LocalDate currentDate = startDate; currentDate.isBefore(endDate); currentDate = currentDate.plusDays(1)) {
            if (!workingDays.contains(currentDate)) {
                continue;
            }

            LocalDateTime startTime = LocalDateTime.of(currentDate, LocalTime.of(9, 0));
            LocalDateTime endTime = LocalDateTime.of(currentDate, LocalTime.of(23, 0));
            List<String> availableTimeSlots = new ArrayList<>();
            LocalDateTime currentSlot = startTime;

            while (currentSlot.isBefore(endTime)) {
                boolean isSlotAvailable = true;

                for (Appointment appointment : appointments) {
                    LocalDateTime appointmentEnd = appointment.getAppointmentDateTime()
                            .plusMinutes(appointment.getService().getDurationMinutes())
                            .plusMinutes(breakDuration);

                    if ((currentSlot.isEqual(appointment.getAppointmentDateTime()) || currentSlot.isBefore(appointmentEnd)) &&
                            currentSlot.plusMinutes(serviceDuration + breakDuration).isAfter(appointment.getAppointmentDateTime())) {
                        isSlotAvailable = false;
                        break;
                    }
                }

                if (isSlotAvailable) {
                    availableTimeSlots.add(formatDateTime(currentSlot));
                }

                currentSlot = currentSlot.plusMinutes(serviceDuration + breakDuration);
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

    public List<String> getAvailableTimeSlotsForDay(int barberId, LocalDate date, int serviceDuration) {
        LocalDateTime startTime = LocalDateTime.of(date, LocalTime.of(9, 0));
        LocalDateTime endTime = LocalDateTime.of(date, LocalTime.of(18, 0));
        int breakDuration = 5;  // Перерыв в минутах

        List<Appointment> appointments = appointmentRepository.findByBarberIdAndAppointmentDateTimeBetween(Long.valueOf(barberId), startTime, endTime);

        List<String> availableTimeSlots = new ArrayList<>();
        LocalDateTime currentSlot = startTime;
        while (currentSlot.isBefore(endTime)) {
            boolean isSlotAvailable = true;
            for (Appointment appointment : appointments) {
                LocalDateTime appointmentEnd = appointment.getAppointmentDateTime()
                        .plusMinutes(appointment.getService().getDurationMinutes())
                        .plusMinutes(breakDuration);

                if ((currentSlot.isEqual(appointment.getAppointmentDateTime()) || currentSlot.isBefore(appointmentEnd)) &&
                        currentSlot.plusMinutes(serviceDuration + breakDuration).isAfter(appointment.getAppointmentDateTime())) {
                    isSlotAvailable = false;
                    break;
                }
            }
            if (isSlotAvailable) {
                availableTimeSlots.add(formatDateTime(currentSlot));
            }
            currentSlot = currentSlot.plusMinutes(serviceDuration + breakDuration);
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

    private static final Logger logger = LoggerFactory.getLogger(AppointmentController.class);

    // Метод для создания записи на прием
    @Transactional
    public boolean createAppointment(LocalDateTime appointmentDateTime, Integer barberId, Integer serviceId, String name, Long userId) {
        logger.debug("Создание записи на прием с параметрами: appointmentDateTime={}, barberId={}, serviceId={}, name={}, userId={}",
                appointmentDateTime, barberId, serviceId, name, userId);

        // Найдем пользователя по chatId
        User user = userRepository.findByChatId(userId);
        if (user == null) {
            return false;
        }

        // Получаем информацию о барбере и услуге
        Barber barber = barberRepository.findById(barberId).orElse(null);
        Service service = serviceRepository.findById(serviceId).orElse(null);
        //User user = userRepository.findById(userId).orElse(null);

        logger.debug("Результаты поиска: barber={}, service={}, user={}", barber, service, user);

        // Проверяем, найдены ли барбер, услуга и пользователь
        if (barber == null || service == null) {
            return false; // Возвращаем false, если барбер, услуга или пользователь не найдены
        }

        Hibernate.initialize(barber.getBarberAdmins());
        Hibernate.initialize(barber.getReviews());

        // Создаем новую запись на прием
        Appointment appointment = new Appointment();
        appointment.setAppointmentDateTime(appointmentDateTime);
        appointment.setBarber(barber);
        appointment.setService(service);
        appointment.setUser(user);
        appointment.setName(name);
        // Сохраняем запись в репозитории
        appointmentRepository.save(appointment);

        logger.debug("Запись на прием успешно создана: {}", appointment);

        //String answer = "Ваша запись создана, будем вас ждать!";
        //telegramBotController.sendMessage(telegramBotController.update.getCallbackQuery(), answer);

        return true; // Возвращаем true, если запись на прием успешно создана
    }
}

