package com.example.telegrambarbershop.controller;

import com.example.telegrambarbershop.config.BotConfig;
import com.example.telegrambarbershop.entity.*;
import com.example.telegrambarbershop.repositories.*;
import com.example.telegrambarbershop.service.BarberService;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@Log4j
public class TelegramBotController extends TelegramLongPollingBot {
    final BotConfig config;

    @Autowired
    private BarberAdminRepository barberAdminRepository;

    @Autowired
    private BarberRepository barberRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentController appointmentController;

    private BarberService barberService;
    private List<Barber> barbers; // список барберов

    static final String HELP_TEXT = "Этот бот создан для демонстрации возможностей Spring \n\n" +
        "Вы можете выполнять команды из главного меню слева или введя команду: " +
            "Введите /start, чтобы увидеть приветственное сообщение\n\n" +
            "Введите /mydata, чтобы просмотреть сохраненные о вас данные\n\n" +
            "Введите /help, чтобы снова увидеть это сообщение";

    public TelegramBotController(BotConfig config) {
        this.config = config;
        //this.barberService = barberService;
        //this.barbers = barberService.getAllBarbers();
        List<BotCommand> listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand("/start", "получите приветственное сообщение"));
        listofCommands.add(new BotCommand("/mydata", "сохраните ваши данные"));
        listofCommands.add(new BotCommand("/deletedata", "удалите мои данные"));
        listofCommands.add(new BotCommand("/help", "информация о том, как использовать этого бота"));
        listofCommands.add(new BotCommand("/settings", "установите свои предпочтения"));
        listofCommands.add(new BotCommand("/bookappointment", "Записаться на прием"));
        listofCommands.add(new BotCommand("/adminlogin", "войти в административную панель"));
        try {
            this.execute(new SetMyCommands(listofCommands, new BotCommandScopeDefault(), null));
        }
        catch (TelegramApiException e) {
            log.error("Ошибка настройки списка команд бота: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getBotToken();
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText != null) {
                switch (messageText) {
                    case "/start":
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;
                    case "/help":
                        sendMessage(chatId, HELP_TEXT);
                        break;
                    case "/register":
                        register(chatId);
                        break;
                    case "/bookappointment":
                        showBarbers(chatId);
                        break;
                    case "/adminlogin":
                        adminLogin(chatId);
                        break;
                    default:
                        if (messageText.startsWith("/")) {
                            sendMessage(chatId, "Извините, такой команды нет");
                        } else {
                            if (messageText.contains("_")) {
                                handleAdminCredentials(chatId, messageText);
                            } else {
                                sendMessage(chatId, "Извините, такой команды нет");
                            }
                        }
                        break;
                }
            } else if (update.hasCallbackQuery()) {
                String callbackData = update.getCallbackQuery().getData();
                //handleCallbackQuery(update.getCallbackQuery(), callbackData);
            } else {
                log.error("Получен пустой текст сообщения.");
            }

        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.equals("YES_BUTTON")) {
                String text = "Вы нажали кнопку Да";
                EditMessageText message = new EditMessageText();
                message.setChatId(String.valueOf(chatId));
                message.setText(text);
                message.setMessageId((int) messageId);

                try {
                    execute(message);
                }
                catch (TelegramApiException e) {
                    log.error("Произошла ошибка: " + e.getMessage());
                }
            }
            else if(callbackData.equals("NO_BUTTON")) {
                String text = "Вы нажали кнопку Нет";
                EditMessageText message = new EditMessageText();
                message.setChatId(String.valueOf(chatId));
                message.setText(text);
                message.setMessageId((int) messageId);

                try {
                    execute(message);
                }
                catch (TelegramApiException e) {
                    log.error("Произошла ошибка: " + e.getMessage());
                }
            } else {
                // Добавьте обработку callbackData для выбора барбера и услуги
                String[] dataParts = callbackData.split("_");
                if ((dataParts.length == 2) && (!dataParts[0].equals("slot"))) {
                    String choiceType = dataParts[0];
                    Integer choiceId = Integer.parseInt(dataParts[1]);

                    if (choiceType.equals("barber")) {
                        //saveBarberId(choiceId);
                        Integer barberId = choiceId;
                        Barber barber = barberRepository.findById(barberId).orElse(null);
                        showServices(chatId, choiceId);
                    }

                    if (choiceType.equals("service")) {
                        showPrices(chatId, choiceId);
                        //Integer barberId = choiceId;
                        Integer serviceId = Integer.parseInt(dataParts[1]);
                        showAvailableTimeSlots(chatId, LocalDateTime.now(), 1, serviceId);
                        //challengeBarberIdRepository.deleteById(1);
                    }
                } else {
                    String[] dataPartsTime = callbackData.split("_");
                    String choiceType = dataPartsTime[0];
                    String dataTime = dataPartsTime[1];
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd' 'HH:mm"); // Формат времени
                    LocalDateTime dateTimeFormat = LocalDateTime.parse(dataTime, formatter);
                    appointmentController.createAppointment(dateTimeFormat, 1, 1, update.getCallbackQuery().getFrom().getFirstName());
                    registrationRecord(chatId, update.getCallbackQuery().getFrom().getFirstName());
                }
            }
        }
    }

    private void showAvailableTimeSlots(long chatId, LocalDateTime date, Integer barberId, Integer serviceId) {
        List<String> availableTimeSlots = appointmentController.getAvailableTimeSlots(date, barberId, serviceId);

        if (availableTimeSlots.isEmpty()) {
            sendMessage(chatId, "На выбранную дату нет доступных слотов для записи.");
            return;
        }

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (String slot : availableTimeSlots) {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(slot.toString());
            button.setCallbackData("slot_" + slot.toString());
            rowInline.add(button);
            rowsInline.add(rowInline);
        }

        markupInline.setKeyboard(rowsInline);
        SendMessage message = new SendMessage(String.valueOf(chatId), "Выберите удобное время для записи:");
        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Произошла ошибка: " + e.getMessage());
        }
    }

    private boolean authenticateAdmin(String username, String password) {
        // Здесь выполняется запрос к базе данных для получения данных администратора по логину
        // Замените эту строку на ваш запрос к базе данных
        BarberAdmin admin = barberAdminRepository.findByUsername(username);

        // Проверяем, найден ли администратор с указанным логином
        if (admin != null) {
            // Проверяем совпадение пароля
            return admin.getPassword().equals(password);
        } else {
            // Возвращаем false, если администратор с таким логином не найден
            return false;
        }
    }

    public List<Appointment> getAppointmentsForBarber(Long barberId) {
        return appointmentRepository.findByBarberId(barberId);
    }

    private void viewAppointments(long chatId) {
        sendMessage(chatId, "Здесь будет отображена информация о записях.");
        List<Appointment> appointments = getAppointmentsForBarber(Long.valueOf(1)); // Замените 1 на id барбера
        // Далее вы можете отобразить эти записи в сообщении для администратора
        // Например, вы можете отправить сообщение с информацией о записях
        // Например:
        StringBuilder messageText = new StringBuilder();
        messageText.append("Список записей:\n");
        for (Appointment appointment : appointments) {
            messageText.append("- ").append(appointment.getAppointmentDateTime()).append("\n");
            // Добавьте другие необходимые поля записи
        }
        sendMessage(chatId, messageText.toString());
    }

    private void handleAdminCredentials(long chatId, String credentials) {
        // Разделение логина и пароля по символу '_'
        String[] parts = credentials.split("_");
        if (parts.length == 2) {
            String login = parts[0];
            String password = parts[1];

            // Попытка аутентификации администратора
            if (authenticateAdmin(login, password)) {
                // Если аутентификация успешна, предоставляем доступ к административным функциям
                viewAppointments(chatId);
            } else {
                // Если аутентификация неуспешна, отправляем сообщение об ошибке
                sendMessage(chatId, "Ошибка аутентификации. Пожалуйста, проверьте логин и пароль и попробуйте снова.");
            }
        } else {
            // Если сообщение не содержит правильного формата логина и пароля, отправляем сообщение об ошибке
            sendMessage(chatId, "Неправильный формат логина и пароля. Введите их через символ '_'. Например, 'admin_password'.");
        }
    }

    private void showServices(long chatId, Integer barberId) {
        Barber barber = barberRepository.findById(barberId).orElse(null);
        if (barber == null) {
            log.error("Барбер с ID " + barberId + " не найден");
            return;
        }

        List<Service> services = serviceRepository.findAll();

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (Service service : services) {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(service.getServiceName());
            button.setCallbackData("service_" + service.getId());
            rowInline.add(button);
            rowsInline.add(rowInline);
        }

        markupInline.setKeyboard(rowsInline);
        SendMessage message = new SendMessage(String.valueOf(chatId), "Выберите услугу:");
        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Произошла ошибка: " + e.getMessage());
        }
    }

    private void showBarbers(long chatId) {
        List<Barber> barbers = (List<Barber>) barberRepository.findAll(); // Получаем список всех барберов из репозитория
        SendMessage message = new SendMessage(String.valueOf(chatId), "Выберите барбера:");

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
        message.setReplyMarkup(markupInline);

        try {
            execute(message); // Отправляем сообщение с кнопками
        } catch (TelegramApiException e) {
            log.error("Сообщение об ошибке при отправке сообщения: " + e.getMessage());
        }
    }

    private void showPrices(long chatId, Integer serviceId) {
        Service service = serviceRepository.findById(serviceId).orElse(null);
        if (service == null) {
            log.error("Услуга с ID " + serviceId + " не найдена");
            return;
        }

        SendMessage message = new SendMessage(String.valueOf(chatId), "Цена за услугу " + service.getServiceName() + ": " + service.getPrice());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Произошла ошибка: " + e.getMessage());
        }
    }

    private void register(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Вы действительно хотите зарегистрироваться?");

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        var yesButton = new InlineKeyboardButton();

        yesButton.setText("Да");
        yesButton.setCallbackData("YES_BUTTON");

        var noButton = new InlineKeyboardButton();
        noButton.setText("Нет");
        noButton.setCallbackData("NO_BUTTON");

        rowInLine.add(yesButton);
        rowInLine.add(noButton);

        rowsInLine.add(rowInLine);

        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        try {
            execute(message);
        }
        catch (TelegramApiException e) {
            log.error("Произошла ошибка: " + e.getMessage());
        }
    }

    private void startCommandReceived(long chatId, String name) {
        if (name != null) {
            String answer = "Привет, " + name + " приятно познакомиться!";
            log.info("Ответил пользователю " + name);
            sendMessage(chatId, answer);
        } else {
            log.error("Получено пустое имя пользователя.");
        }
    }

    private void registrationRecord(long chatId, String name) {
        if (name != null) {
            String answer = "Ваша заявка успешно создана, будем вас ждать!";
        sendMessage(chatId, answer);
        } else {
            log.error("Получено пустое имя пользователя.");
        }
    }

    public void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf((chatId)));
        message.setText(textToSend);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Погода");
        row.add("получите случайную шутку");
        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("зарегистрировать");
        row.add("проверьте мои данные");
        row.add("удалите мои данные");
        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        }
        catch (TelegramApiException e) {
            log.error("Произошла ошибка: " + e.getMessage());
        }
    }

    public void adminLogin(long chatId) {
        // Здесь может быть логика аутентификации администратора
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Введите логин и пароль в формате: логин_пароль");

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Произошла ошибка: " + e.getMessage());
        }
    }
}
