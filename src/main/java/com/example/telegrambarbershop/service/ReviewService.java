package com.example.telegrambarbershop.service;

import com.example.telegrambarbershop.controller.TelegramBotController;
import com.example.telegrambarbershop.entity.*;
import com.example.telegrambarbershop.repositories.AppointmentRepository;
import com.example.telegrambarbershop.repositories.BarberRepository;
import com.example.telegrambarbershop.repositories.ReviewRepository;
import com.example.telegrambarbershop.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private BarberRepository barberRepository;

    @Autowired
    private TelegramNotificationService telegramNotificationService;

    // Метод для отправки запросов на отзыв
    @Transactional
    @Scheduled(fixedRate = 600000)  // Проверяем каждые 600 секунд
    public void sendReviewRequests() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime notificationTime = now.minusMinutes(1);

        List<Appointment> appointments = appointmentRepository.findCompletedAppointmentsWithNoNotification(notificationTime);
        for (Appointment appointment : appointments) {
            // Отправляем запрос на отзыв через Telegram
            sendReviewRequest(appointment);
            // Обновляем статус уведомления
            appointment.setReviewRequestSent(true);
            appointmentRepository.save(appointment);
        }
    }

    private void sendReviewRequest(Appointment appointment) {
        Long chatId = appointment.getUser().getChatId();  // Получаем chatId пользователя из записи
        String message = "Хотите оставить отзыв о выполненной услуге?";

        // Создаем кнопки "Да" и "Нет"
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton yesButton = new InlineKeyboardButton("Да");
        yesButton.setCallbackData("review_yes_" + appointment.getId());

        InlineKeyboardButton noButton = new InlineKeyboardButton("Нет");
        noButton.setCallbackData("review_no_" + appointment.getId());

        keyboard.add(Arrays.asList(yesButton, noButton));
        inlineKeyboardMarkup.setKeyboard(keyboard);

        // Отправляем сообщение с кнопками
        telegramNotificationService.sendMessageWithKeyboard(chatId, message, inlineKeyboardMarkup);
    }

    // Метод для обработки отзыва
    @Transactional
    public void handleReview(Double rating, String comment, Integer appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
        if (appointment == null) {
            return;
        }

        Barber barber = appointment.getBarber();
        Review review = new Review(barber, rating, comment, appointment);
        reviewRepository.save(review);

        // Пересчитываем средний рейтинг барбера
        barber.updateRating();
        barberRepository.save(barber);
    }

    public List<ReviewDTO> getAllReviews() {
        List<Review> reviews = reviewRepository.findAll();

        return reviews.stream().map(review -> {
            Barber barber = review.getBarber();
            barber.updateRating();  // Обновляем среднюю оценку для барбера

            BarberDTO barberDTO = new BarberDTO(
                    barber.getId(),
                    barber.getName(),
                    barber.getRating() // Передаем уже обновленную среднюю оценку
            );

            return new ReviewDTO(
                    review.getId(),
                    review.getRating(),
                    review.getComment(),
                    review.getCreatedAt(),
                    barberDTO,
                    review.getAppointment().getName()  // Имя пользователя из Appointment
            );
        }).collect(Collectors.toList());
    }
}

