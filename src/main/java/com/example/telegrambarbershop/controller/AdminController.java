package com.example.telegrambarbershop.controller;

import com.example.telegrambarbershop.entity.Barber;
import com.example.telegrambarbershop.entity.Service;
import com.example.telegrambarbershop.entity.User;
import com.example.telegrambarbershop.repositories.BarberRepository;
import com.example.telegrambarbershop.repositories.ServiceRepository;
import com.example.telegrambarbershop.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramBot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private String lastAdminCommand = "";

    @Autowired
    private BarberRepository barberRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TelegramBotController botController;

    // Добавление барбера
    @PostMapping("/addBarber")
    public void addBarber(String name, String phoneNumber, String specialty, double rating) {
        Barber barber = new Barber();
        barber.setName(name);
        barber.setPhoneNumber(phoneNumber);
        barber.setSpecialty(specialty);
        barber.setRating(rating);
        barberRepository.save(barber);
    }

    // Редактирование барбера
    @PutMapping("/editBarber")
    public String editBarber(@RequestParam int id, @RequestParam String name, @RequestParam String phoneNumber, @RequestParam String specialty, @RequestParam double rating) {
        Barber barber = barberRepository.findById(id).orElse(null);
        if (barber == null) {
            return "Барбер не найден";
        }
        barber.setName(name);
        barber.setPhoneNumber(phoneNumber);
        barber.setSpecialty(specialty);
        barber.setRating(rating);
        barberRepository.save(barber);
        return "Барбер обновлен";
    }

    // Удаление барбера
    @DeleteMapping("/deleteBarber")
    public String deleteBarber(@RequestParam int id) {
        barberRepository.deleteById(id);
        return "Барбер удален";
    }

    // Добавление услуги
    @PostMapping("/addService")
    public void addService(String serviceName, BigDecimal price) {
        Service service = new Service();
        service.setServiceName(serviceName);
        service.setPrice(price);
        serviceRepository.save(service);
    }

    // Редактирование услуги
    @PutMapping("/editService")
    public String editService(@RequestParam int id, @RequestParam String serviceName, @RequestParam BigDecimal price) {
        Service service = serviceRepository.findById(id).orElse(null);
        if (service == null) {
            return "Услуга не найдена";
        }
        service.setServiceName(serviceName);
        service.setPrice(price);
        serviceRepository.save(service);
        return "Услуга обновлена";
    }

    // Удаление услуги
    @DeleteMapping("/deleteService")
    public String deleteService(@RequestParam int id) {
        serviceRepository.deleteById(id);
        return "Услуга удалена";
    }

    // Настройка рабочих дней
    @PostMapping("/setWorkingDays")
    public String setWorkingDays(@RequestParam List<LocalDate> workingDays) {
        // Логика для настройки рабочих дней
        return "Рабочие дни установлены";
    }

    // Публикация постов
    @PostMapping("/postAnnouncement")
    public String postAnnouncement(@RequestParam String message) {
        // Логика для публикации постов
        sendTextMessageToAll(message);
        return "Объявление опубликовано";
    }

    // Публикация постов с изображениями
    @PostMapping("/postPhoto")
    public String postPhoto(@RequestParam String caption, @RequestParam String photoUrl) {
        sendPhotoMessageToAll(caption, photoUrl);
        return "Фото опубликовано";
    }

    // Публикация постов с голосовыми сообщениями
    @PostMapping("/postVoice")
    public String postVoice(@RequestParam String voiceUrl) {
        sendVoiceMessageToAll(voiceUrl);
        return "Голосовое сообщение опубликовано";
    }

    // Обработка команд администратора через бота
    public void handleAdminCommands(long chatId, String messageText) {
        if (messageText.startsWith("/addBarber")) {
            botController.sendMessage(chatId, "Введите данные барбера в формате: Имя_НомерТелефона_Специальность_Рейтинг");
        } else if (messageText.startsWith("/editBarber")) {
            botController.sendMessage(chatId, "Введите данные для редактирования барбера в формате: ID_Имя_НомерТелефона_Специальность_Рейтинг");
        } else if (messageText.startsWith("/deleteBarber")) {
            botController.sendMessage(chatId, "Введите ID барбера для удаления");
        } else if (messageText.startsWith("/addService")) {
            botController.sendMessage(chatId, "Введите данные услуги в формате: Название_Цена");
        } else if (messageText.startsWith("/editService")) {
            botController.sendMessage(chatId, "Введите данные для редактирования услуги в формате: ID_Название_Цена");
        } else if (messageText.startsWith("/deleteService")) {
            botController.sendMessage(chatId, "Введите ID услуги для удаления");
        } else if (messageText.startsWith("/setWorkingDays")) {
            botController.sendMessage(chatId, "Введите рабочие дни в формате: YYYY-MM-DD,YYYY-MM-DD,...");
        } else if (messageText.startsWith("/postAnnouncement")) {
            botController.sendMessage(chatId, "Введите текст объявления");
        } else if (messageText.startsWith("/postPhoto")) {
            botController.sendMessage(chatId, "Введите описание и URL фото в формате: Описание_URL");
        } else if (messageText.startsWith("/postVoice")) {
            botController.sendMessage(chatId, "Введите URL голосового сообщения");
        } else {
            botController.sendMessage(chatId, "Неизвестная команда администратора.");
        }
    }

    public void handleAdminInput(long chatId, String messageText) {
        // Здесь нужно добавить логику обработки ввода для каждой команды администратора
        if (messageText.startsWith("Имя_")) {
            String[] parts = messageText.split("_");
            if (parts.length == 4) {
                String name = parts[0];
                String phoneNumber = parts[1];
                String specialty = parts[2];
                double rating;
                try {
                    rating = Double.parseDouble(parts[3]);
                } catch (NumberFormatException e) {
                    botController.sendMessage(chatId, "Неверный формат рейтинга.");
                    return;
                }
                addBarber(name, phoneNumber, specialty, rating);
                botController.sendMessage(chatId, "Барбер добавлен.");
            } else {
                botController.sendMessage(chatId, "Неверный формат ввода.");
            }
        } else if (messageText.startsWith("Название_")) {
            String[] parts = messageText.split("_");
            if (parts.length == 2) {
                String serviceName = parts[0];
                BigDecimal price;
                try {
                    price = new BigDecimal(parts[1]);
                } catch (NumberFormatException e) {
                    botController.sendMessage(chatId, "Неверный формат цены.");
                    return;
                }
                addService(serviceName, price);
                botController.sendMessage(chatId, "Услуга добавлена.");
            } else {
                botController.sendMessage(chatId, "Неверный формат ввода.");
            }
        }
    }

    private void sendTextMessageToAll(String text) {
        // Логика отправки текстового сообщения всем пользователям
        // Получаем список всех пользователей (например, из базы данных)
        List<Long> userIds = getAllUserIds();
        for (Long userId : userIds) {
            botController.sendMessage(userId, text);
        }
    }

    private void sendPhotoMessageToAll(String caption, String photoUrl) {
        List<Long> userIds = getAllUserIds();
        for (Long userId : userIds) {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(userId.toString());
            sendPhoto.setPhoto(new InputFile(photoUrl));
            sendPhoto.setCaption(caption);
            try {
                botController.execute(sendPhoto);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendVoiceMessageToAll(String voiceUrl) {
        List<Long> userIds = getAllUserIds();
        for (Long userId : userIds) {
            SendVoice sendVoice = new SendVoice();
            sendVoice.setChatId(userId.toString());
            sendVoice.setVoice(new InputFile(voiceUrl));
            try {
                botController.execute(sendVoice);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private List<Long> getAllUserIds() {
        // Логика получения всех идентификаторов пользователей из базы данных
        return userRepository.findAll().stream().map(User::getChatId).collect(Collectors.toList());
    }

//    private void sendMessage(Long chatId, String text) {
//        SendMessage message = new SendMessage();
//        message.setChatId(chatId.toString());
//        message.setText(text);
//        try {
//            bot.execute(message);
//        } catch (TelegramApiException e) {
//            e.printStackTrace();
//        }
//    }

    @PostMapping("/makeAdmin")
    public String makeAdmin(@RequestParam Long chatId) {
        User user = userRepository.findByChatId(chatId);
        if (user == null) {
            return "Пользователь не найден";
        }
        user.setAdmin(true);
        userRepository.save(user);
        return "Пользователь назначен администратором";
    }
}
