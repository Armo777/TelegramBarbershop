package com.example.telegrambarbershop.controller;

import com.example.telegrambarbershop.entity.*;
import com.example.telegrambarbershop.repositories.*;
import com.example.telegrambarbershop.service.AppointmentService;
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
    private AppointmentService appointmentService;

    @Autowired
    private WorkingDayRepository workingDayRepository;

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
        return appointmentService.getAllAppointmentsWithDetails();
    }

    @GetMapping("/viewBarberAdmins")
    public List<BarberAdmin> viewBarberAdmins() {
        return barberAdminRepository.findAll();
    }

    @GetMapping("/viewWorkingDays")
    public List<WorkingDay> viewWorkingDays() {
        return workingDayRepository.findAll();
    }

    @PostMapping("/addBarber")
    public void addBarber(String name, String phoneNumber, String specialty, double rating) {
        Barber barber = new Barber();
        barber.setName(name);
        barber.setPhoneNumber(phoneNumber);
        barber.setSpecialty(specialty);
        barber.setRating(rating);
        barberRepository.save(barber);
    }

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

    @DeleteMapping("/deleteBarber")
    public String deleteBarber(@RequestParam int id) {
        barberRepository.deleteById(id);
        return "Барбер удален";
    }

    @PostMapping("/addService")
    public void addService(String serviceName, BigDecimal price, int durationMinutes) {
        Service service = new Service();
        service.setServiceName(serviceName);
        service.setPrice(price);
        service.setDurationMinutes(durationMinutes);
        serviceRepository.save(service);
    }

    @PutMapping("/editService")
    public String editService(@RequestParam int id, @RequestParam String serviceName, @RequestParam BigDecimal price, @RequestParam int durationMinutes) {
        Service service = serviceRepository.findById(id).orElse(null);
        if (service == null) {
            return "Услуга не найдена";
        }
        service.setServiceName(serviceName);
        service.setPrice(price);
        service.setDurationMinutes(durationMinutes);
        serviceRepository.save(service);
        return "Услуга обновлена";
    }

    @DeleteMapping("/deleteService")
    public String deleteService(@RequestParam int id) {
        serviceRepository.deleteById(id);
        return "Услуга удалена";
    }

    @DeleteMapping("/deleteWorkingDays")
    public String deleteWorkingDays(@RequestParam int id) {
        workingDayRepository.deleteById(id);
        return "Дата удалена";
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

    @PostMapping("/setWorkingDays")
    public String setWorkingDays(@RequestParam List<LocalDate> workingDays) {
        // Логика для настройки рабочих дней
        return "Рабочие дни установлены";
    }

    @PostMapping("/postAnnouncement")
    public String postAnnouncement(@RequestParam String message) {
        // Логика для публикации постов
        sendTextMessageToAll(message);
        return "Объявление опубликовано";
    }

    @PostMapping("/postPhoto")
    public String postPhoto(@RequestParam String caption, @RequestParam String photoUrl) {
        sendPhotoMessageToAll(caption, photoUrl);
        return "Фото опубликовано";
    }

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
                case "/viewWorkingDays":
                    List<WorkingDay> workingDays = viewWorkingDays();
                    botController.sendMessage(chatId, formatWorkingDay(workingDays));
                    break;
                case "/setWorkingDays":
                    botController.showCalendarForWorkingDays((int) chatId);
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
                    botController.sendMessage(chatId, "Введите данные в формате: Название_Цена_Таймер");
                    break;
                case "/editService":
                    botController.sendMessage(chatId, "Введите данные в формате: ID_Название_Цена_Таймер");
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
//                case "/setWorkingDays":
//                    botController.sendMessage(chatId, "Введите данные в формате: ID_РабочиеДни");
//                    break;
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
                case "/viewRequest":
                    botController.sendMessage(chatId, "Просмотр поступающих заявок:");
                    botController.showRequest(chatId);
                    break;
                case "/deleteWorkingDays":
                    botController.sendMessage(chatId, "Введите ID даты для удаления");
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
                    .append(", Таймер: ")
                    .append(service.getDurationMinutes())
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

    private String formatWorkingDay(List<WorkingDay> workingDays) {
        StringBuilder sb = new StringBuilder("Список список рабочих дней:\n");
        for (WorkingDay workingDay : workingDays) {
            sb.append(workingDay.getId())
                    .append(": Дата: ")
                    .append(workingDay.getDate())
                    .append("\n");
        }
        return sb.toString();
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
        List<Long> userIds = getAllUserIds();
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
        return userRepository.findAll().stream().map(User::getChatId).collect(Collectors.toList());
    }

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
