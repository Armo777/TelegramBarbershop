package com.example.telegrambarbershop.controller;

import com.example.telegrambarbershop.entity.*;
import com.example.telegrambarbershop.repositories.*;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideoNote;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramBot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
public class AdminController {

    //private String lastAdminCommand = "";

    @Autowired
    private BarberRepository barberRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TelegramBotController botController;

    @Autowired
    private BarberAdminRepository barberAdminRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private MainAdminRepository mainAdminRepository;

    @GetMapping("/viewBarbers")
    public List<Barber> viewBarbers() {
        return barberRepository.findAll();
    }

    @GetMapping("/viewServices")
    public List<Service> viewServices() {
        return serviceRepository.findAll();
    }

    @GetMapping("/viewAppointments")
    public List<Appointment> viewAppointments() {
        return appointmentRepository.findAll();
    }

    @GetMapping("/viewBarberAdmins")
    public List<BarberAdmin> viewBarberAdmins() {
        return barberAdminRepository.findAll();
    }

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

    @PutMapping("/addBarberAdmin")
    public void addBarberAdmin(String username, String password, long barberId) {
        Barber barber = barberRepository.findById((int) barberId).orElse(null);
        if (barber == null) {
            throw new IllegalArgumentException("Барбер с указанным ID не найден.");
        }
        BarberAdmin barberAdmin = new BarberAdmin();
        barberAdmin.setUsername(username);
        barberAdmin.setPassword(password);
        barberAdmin.setBarber(barber);
        barberAdminRepository.save(barberAdmin);
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

    @PostMapping("/postVideoNote")
    public String postVideoNote(@RequestParam String videoNoteId) {
        sendVideoNoteToAll(videoNoteId);
        return "Видеосообщение успешно опубликовано";
    }

    @PutMapping("/updateBarberAdmin/{id}")
    public ResponseEntity<BarberAdmin> updateBarberAdmin(@PathVariable Long id, @RequestBody BarberAdmin updatedBarberAdmin) {
        Optional<BarberAdmin> barberAdminOptional = barberAdminRepository.findById(id);
        if (barberAdminOptional.isPresent()) {
            BarberAdmin barberAdmin = barberAdminOptional.get();
            barberAdmin.setUsername(updatedBarberAdmin.getUsername());
            barberAdmin.setPassword(updatedBarberAdmin.getPassword());
            barberAdmin.setBarber(updatedBarberAdmin.getBarber());
            barberAdminRepository.save(barberAdmin);
            return ResponseEntity.ok(barberAdmin);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/deleteBarberAdmin/{id}")
    public ResponseEntity<Void> deleteBarberAdmin(@PathVariable Long id) {
        if (barberAdminRepository.existsById(id)) {
            barberAdminRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Обработка команд администратора через бота
    public void handleAdminCommands(long chatId, String messageText) {
        if (botController.lastAdminCommand != null) {
            switch (botController.lastAdminCommand) {
                case "/viewBarbers":
                    List<Barber> barbers = viewBarbers();
                    botController.sendMessage(chatId, formatBarbers(barbers));
                    break;
                case "/viewServices":
                    List<Service> services = viewServices();
                    botController.sendMessage(chatId, formatServices(services));
                    break;
                case "/viewAppointments":
                    List<Appointment> appointments = viewAppointments();
                    botController.sendMessage(chatId, formatAppointments(appointments));
                    break;
                case "/viewBarberAdmins":
                    List<BarberAdmin> barberAdmins = viewBarberAdmins();
                    botController.sendMessage(chatId, formatBarberAdmins(barberAdmins));
                    break;
                case "/addBarber":
                    botController.sendMessage(chatId, "Введите данные в формате: Имя_НомерТелефона_Специальность_Рейтинг");
                    break;
                case "/editBarber":
                    botController.sendMessage(chatId, "Введите данные в формате: ID_Имя_НомерТелефона_Специальность_Рейтинг");
                    break;
                case "/deleteBarber":
                    botController.sendMessage(chatId, "Введите ID барбера для удаления");
                    break;
                case "/addService":
                    botController.sendMessage(chatId, "Введите данные в формате: Название_Цена");
                    break;
                case "/editService":
                    botController.sendMessage(chatId, "Введите данные в формате: ID_Название_Цена");
                    break;
                case "/deleteService":
                    botController.sendMessage(chatId, "Введите ID услуги для удаления");
                    break;
                case "/addBarberAdmin":
                    botController.sendMessage(chatId, "Введите данные в формате: Логин_Пароль_IDСотрудника");
                    break;
                case "/updateBarberAdmin":
                    botController.sendMessage(chatId, "Введите данные в формате: Логин_Пароль_IDСотрудника");
                    break;
                case "/deleteBarberAdmin":
                    botController.sendMessage(chatId, "Введите ID администратора сотрудника для удаления");
                    break;
                case "/setWorkingDays":
                    botController.sendMessage(chatId, "Введите данные в формате: ID_РабочиеДни");
                    break;
                case "/postAnnouncement":
                    botController.sendMessage(chatId, "Введите текст объявления");
                    break;
                case "/postPhoto":
                    botController.sendMessage(chatId, "Пришлите фото для публикации");
                    break;
                case "/postVoice":
                    botController.sendMessage(chatId, "Пришлите голосовое сообщение для публикации");
                    break;
                case "/postVideoNote":
                    botController.sendMessage(chatId, "Пришлите видеосообщение для публикации");
                    break;
                default:
                    botController.sendMessage(chatId, "Неизвестная команда администратора.");
                    break;
            }
        } else {
            botController.sendMessage(chatId, "Извините, такой команды нет.");
        }
    }

    private String formatBarbers(List<Barber> barbers) {
        StringBuilder sb = new StringBuilder("Список барберов:\n");
        for (Barber barber : barbers) {
            sb.append(barber.getId())
                    .append(": ")
                    .append(barber.getName())
                    .append(", Телефон: ")
                    .append(barber.getPhoneNumber())
                    .append(", Специальность: ")
                    .append(barber.getSpecialty())
                    .append(", Рейтинг: ")
                    .append(barber.getRating())
                    .append("\n");
        }
        return sb.toString();
    }

    private String formatServices(List<Service> services) {
        StringBuilder sb = new StringBuilder("Список услуг:\n");
        for (Service service : services) {
            sb.append(service.getId())
                    .append(": ")
                    .append(service.getServiceName())
                    .append(", Цена: ")
                    .append(service.getPrice())
                    .append("\n");
        }
        return sb.toString();
    }

    private String formatAppointments(List<Appointment> appointments) {
        StringBuilder sb = new StringBuilder("Список записей:\n");
        for (Appointment appointment : appointments) {
            sb.append(appointment.getId())
                    .append(": Клиент: ")
                    .append(appointment.getNameUser())
                    .append(", Барбер: ")
                    .append(appointment.getBarber().getName())
                    .append(", Дата: ")
                    .append(appointment.getAppointmentDateTime())
                    .append(", Услуга: ")
                    .append(appointment.getService().getServiceName())
                    .append("\n");
        }
        return sb.toString();
    }

    private String formatBarberAdmins(List<BarberAdmin> barberAdmins) {
        StringBuilder sb = new StringBuilder("Список администраторов барберов:\n");
        for (BarberAdmin barberAdmin : barberAdmins) {
            sb.append(barberAdmin.getId())
                    .append(": Логин: ")
                    .append(barberAdmin.getUsername())
                    .append(", Пароль: ")
                    .append(barberAdmin.getPassword())
                    .append(", Барбер: ")
                    .append(barberAdmin.getBarber().getName())
                    .append("\n");
        }
        return sb.toString();
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

    public void sendPhotoMessageToAll(String caption, String photoUrl) {
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

    public void sendVoiceMessageToAll(String voiceUrl) {
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

    public void sendVideoNoteToAll(String fileId) {
        List<Long> userIds = getAllUserIds(); // Метод получения всех chatId пользователей
        for (Long chatId : userIds) {
            SendVideoNote videoNote = new SendVideoNote();
            videoNote.setChatId(chatId.toString());
            videoNote.setVideoNote(new InputFile(fileId));
            try {
                botController.execute(videoNote);
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
