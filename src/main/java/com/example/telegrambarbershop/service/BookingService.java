package com.example.telegrambarbershop.service;

import com.example.telegrambarbershop.controller.TelegramBotController;
import com.example.telegrambarbershop.entity.Barber;
import com.example.telegrambarbershop.repositories.BarberRepository;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Log4j
public class BookingService {
    @Autowired
    private BarberRepository barberRepository;

    @Autowired
    private TelegramBotController telegramBotController;

    @Autowired
    private WorkingDayService workingDayService;

    public void showAvailableDays(long chatId, Integer barberId, Integer serviceId) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<LocalDate> workingDays = workingDayService.getAllWorkingDays();

        for (int i = 0; i < 30; i++) {
            LocalDate day = LocalDate.now().plusDays(i);

            if (workingDays.contains(day)) {
                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                InlineKeyboardButton button = new InlineKeyboardButton();

                String dayLabel = day.toString();
                if (workingDays.contains(day)) {
                    dayLabel = "✅ " + dayLabel;
                }

                String callbackData = (barberId == null || serviceId == null) ?
                        "toggle_day_" + day.toString() :
                        "day_" + day.toString() + "_" + barberId + "_" + serviceId;

                button.setText(dayLabel);
                button.setCallbackData(callbackData);
                rowInline.add(button);
                rowsInline.add(rowInline);
            }
        }

        List<InlineKeyboardButton> confirmButtonRow = new ArrayList<>();
        InlineKeyboardButton confirmButton = new InlineKeyboardButton();
        confirmButton.setText("Подтвердить");
        confirmButton.setCallbackData("confirm_days_" + barberId + "_" + serviceId);
        confirmButtonRow.add(confirmButton);
        rowsInline.add(confirmButtonRow);

        List<InlineKeyboardButton> backButtonRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("Назад");
        backButton.setCallbackData("back_service_" + barberId);
        backButtonRow.add(backButton);
        rowsInline.add(backButtonRow);

        markupInline.setKeyboard(rowsInline);
        SendMessage message = new SendMessage(String.valueOf(chatId), "Выберите день:");
        message.setReplyMarkup(markupInline);
        try {
            telegramBotController.execute(message);
        } catch (TelegramApiException e) {
            log.error("Произошла ошибка: " + e.getMessage());
        }
    }

    public void showBarbers(long chatId) {
        List<Barber> barbers = barberRepository.findAll();
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (Barber barber : barbers) {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(barber.getName());
            button.setCallbackData("barber_" + barber.getId());
            rowInline.add(button);
            rowsInline.add(rowInline);
        }

        markupInline.setKeyboard(rowsInline);
        SendMessage message = new SendMessage(String.valueOf(chatId), "Выберите барбера:");
        message.setReplyMarkup(markupInline);
        try {
            telegramBotController.execute(message);
        } catch (TelegramApiException e) {
            log.error("Произошла ошибка: " + e.getMessage());
        }
    }
}
