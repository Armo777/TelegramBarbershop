package com.example.telegrambarbershop.controller;

import com.example.telegrambarbershop.config.BotConfig;
import com.example.telegrambarbershop.entity.Appointment;
import com.example.telegrambarbershop.entity.Barber;
import com.example.telegrambarbershop.entity.BarberAdmin;
import com.example.telegrambarbershop.entity.Service;
import com.example.telegrambarbershop.repositories.AppointmentRepository;
import com.example.telegrambarbershop.repositories.BarberAdminRepository;
import com.example.telegrambarbershop.repositories.BarberRepository;
import com.example.telegrambarbershop.repositories.ServiceRepository;
import com.example.telegrambarbershop.service.BarberService;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
            //boolean commandProcessed = false; // Флаг, указывающий, что команда уже была обработана

            if (messageText != null) {
                switch (messageText) {
                    case "/start":
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        //commandProcessed = true;
                        break;
                    case "/help":
                        sendMessage(chatId, HELP_TEXT);
                        //commandProcessed = true;
                        break;
                    case "/register":
                        register(chatId);
                        //commandProcessed = true;
                        break;
                    case "/bookappointment":
                        showBarbers(chatId);
                        //commandProcessed = true;
                        break;
                    case "/adminlogin":
                        adminLogin(chatId);
                        //commandProcessed = true;
                        break;
                    default:
                        if (messageText.startsWith("/")) {
                            sendMessage(chatId, "Извините, такой команды нет");
                            //commandProcessed = true;
                        } else {
                            if (messageText.contains("_")) {
                                handleAdminCredentials(chatId, messageText);
                                //commandProcessed = true;
                            } else {
                                sendMessage(chatId, "Извините, такой команды нет");
                                //commandProcessed = true;
                            }
                        }
                        break;
                }
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
                if (dataParts.length == 2) {
                    String choiceType = dataParts[0];
                    Integer choiceId = Integer.parseInt(dataParts[1]);

                    if (choiceType.equals("barber")) {
                        showServices(chatId, choiceId);
                    } else if (choiceType.equals("service")) {
                        showPrices(chatId, choiceId);
                    }
                }
            }
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

    private void showAdminPanel(long chatId) {
        // Отправка сообщения с кнопкой для просмотра записей
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Просмотр записей");
        button.setCallbackData("view_appointments");
        rowInline.add(button);
        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);

        SendMessage message = new SendMessage(String.valueOf(chatId), "Добро пожаловать в административную панель.");
        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Произошла ошибка: " + e.getMessage());
        }
    }

    private void viewAppointments(long chatId) {
        sendMessage(chatId, "Здесь будет отображена информация о записях.");
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        long messageId = callbackQuery.getMessage().getMessageId();
        long chatId = callbackQuery.getMessage().getChatId();

        if (callbackData.equals("YES_BUTTON")) {
            String text = "Вы нажали кнопку Да";
            EditMessageText message = new EditMessageText();
            message.setChatId(String.valueOf(chatId));
            message.setText(text);
            message.setMessageId((int) messageId);

            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error("Произошла ошибка: " + e.getMessage());
            }
        } else if(callbackData.equals("NO_BUTTON")) {
            String text = "Вы нажали кнопку Нет";
            EditMessageText message = new EditMessageText();
            message.setChatId(String.valueOf(chatId));
            message.setText(text);
            message.setMessageId((int) messageId);

            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error("Произошла ошибка: " + e.getMessage());
            }
        } else {
            // Добавьте обработку callbackData для выбора барбера и услуги
            String[] dataParts = callbackData.split("_");
            if (dataParts.length == 2) {
                String choiceType = dataParts[0];
                Integer choiceId = Integer.parseInt(dataParts[1]);

                if (choiceType.equals("barber")) {
                    showServices(chatId, choiceId);
                } else if (choiceType.equals("service")) {
                    showPrices(chatId, choiceId);
                }
            }
        }

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
        /*List<Barber> barbers = (List<Barber>) barberRepository.findAll();

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        List<KeyboardRow> rows = new ArrayList<>();

        for (Barber barber : barbers) {
            KeyboardRow row = new KeyboardRow();
            row.add(barber.getName());
            rows.add(row);
        }

        markup.setKeyboard(rows);
        SendMessage message = new SendMessage(String.valueOf(chatId), "Выберите барбера:");
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Произошла ошибка: " + e.getMessage());
        }*/
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

    private void bookAppointment(long chatId, Integer barberId) {
        //Service service = serviceRepository.findByName(serviceName);

        Barber barber = barberRepository.findById(Math.toIntExact(barberId)).orElse(null);
        if (barber == null) {
            log.error("Барбер с ID " + barberId + " не найден");
            return;
        }

        // Проверка доступности времени
        LocalDateTime currentTime = LocalDateTime.now();
        List<Appointment> appointments = appointmentRepository.findByBarberAndAppointmentTimeAfter(barber, currentTime);

        /*service = serviceRepository.findByName(serviceName);
        if (service == null) {
            log.error("Услуга с именем " + serviceName + " не найдена");
            return;
        }*/

        if (!appointments.isEmpty()) {
            // Доступные слоты найдены, можно показать их пользователю для выбора
            // Например, создать клавиатуру с кнопками для каждого доступного слота и отправить пользователю
            ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
            List<KeyboardRow> rows = new ArrayList<>();

            for (Appointment appointment : appointments) {
                KeyboardRow row = new KeyboardRow();
                row.add(appointment.getAppointmentDateTime().toString()); // Предположим, что это дата и время слота
                rows.add(row);
            }

            markup.setKeyboard(rows);
            SendMessage message = new SendMessage(String.valueOf(chatId), "Выберите время для записи:");
            message.setReplyMarkup(markup);

            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error("Произошла ошибка: " + e.getMessage());
            }
        } else {
            // Все слоты на ближайшее время заняты, сообщаем об этом пользователю
            SendMessage message = new SendMessage(String.valueOf(chatId), "Извините, все слоты на ближайшее время забронированы. Пожалуйста, выберите другое время.");
            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error("Произошла ошибка: " + e.getMessage());
            }
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

    private  void startCommandReceived(long chatId, String name) {
        if (name != null) {
            String answer = "Привет, " + name + " приятно познакомиться!";
            log.info("Ответил пользователю " + name);
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

        /*if (textToSend != null && !textToSend.isEmpty()) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));

            //message.setText("Выберите услугу:");

            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setSelective(true);
            keyboardMarkup.setResizeKeyboard(true);
            keyboardMarkup.setOneTimeKeyboard(false);

            List<KeyboardRow> keyboardRows = new ArrayList<>();

            // Пример кнопок с услугами и барберами
            KeyboardRow row1 = new KeyboardRow();
            row1.add("Мужские стрижки");
            row1.add("Борода");
            row1.add("Комплексные услуги");

            KeyboardRow row2 = new KeyboardRow();
            row2.add("Барбер Антон");
            row2.add("Барбер Арман");

            keyboardRows.add(row1);
            keyboardRows.add(row2);

            keyboardMarkup.setKeyboard(keyboardRows);
            message.setReplyMarkup(keyboardMarkup);


            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error("Произошла ошибка: " + e.getMessage());
            }
        } else {
            log.error("Получен пустой текст сообщения для отправки.");
        }*/
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

    /*public void sendAppointmentKeyboard(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите услугу:");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        // Пример кнопок с услугами и барберами
        KeyboardRow row1 = new KeyboardRow();
        row1.add("Мужские стрижки");
        row1.add("Борода");
        row1.add("Комплексные услуги");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Барбер Антон");
        row2.add("Барбер Арман");

        keyboardRows.add(row1);
        keyboardRows.add(row2);

        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Произошла ошибка: " + e.getMessage());
        }
    }*/

    public static void handleAppointmentChoice(Long chatId, String choice) {
        // Логика обработки выбора барбера и услуги
        String confirmationMessage = "Вы записаны на услугу: " + choice;
        //sendMessage(chatId, confirmationMessage);
        new TelegramBotController(new BotConfig()).sendMessage(chatId, confirmationMessage);
    }

    private void handleAppointmentTimeChoice(long chatId, Integer barberId, LocalDateTime chosenTime) {
        // Отправляем пользователю сообщение с подтверждением записи
        sendMessage(chatId, "Вы успешно записаны на прием на " + chosenTime + ". Приятного дня!");
    }

    private void showAppointmentTimes(long chatId, Integer barberId) {
        // Здесь мы должны получить список доступных временных слотов для выбранного барбера
        // Предположим, что у нас есть метод, который возвращает список свободных временных слотов для барбера
        List<LocalDateTime> availableTimes = getAvailableTimesForBarber(barberId);

        // Создаем клавиатуру с кнопками для каждого доступного временного слота
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (LocalDateTime time : availableTimes) {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(time.toString());
            button.setCallbackData("appointment_" + barberId + "_" + time.toString()); // Передаем данные о выбранном времени
            rowInline.add(button);
            rowsInline.add(rowInline);
        }

        markupInline.setKeyboard(rowsInline);
        SendMessage message = new SendMessage(String.valueOf(chatId), "Выберите время для записи:");
        message.setReplyMarkup(markupInline);

        try {
            execute(message); // Отправляем сообщение с кнопками выбора времени
        } catch (TelegramApiException e) {
            log.error("Произошла ошибка: " + e.getMessage());
        }
    }

    private void bookAppointmentTime(long chatId, Integer barberId) {
        // Показываем пользователю доступные временные слоты для выбранного барбера
        showAppointmentTimes(chatId, barberId);
    }



    public List<LocalDateTime> getAvailableTimesForBarber(Integer barberId) {
        // Получаем все записи на прием для указанного барбера
        List<Appointment> appointments = appointmentRepository.findByBarberIdOrderByAppointmentDateTime(barberId);

        // Предположим, что у нас есть список всех возможных временных слотов для барбера
        // Например, 9:00, 9:30, 10:00, и т.д. до конца рабочего дня

        // Создаем список свободных временных слотов, начинающийся с первого времени работы и заканчивающийся последним
        List<LocalDateTime> availableTimes = generateAllPossibleTimes();

        // Проходимся по списку записей на прием и удаляем занятые временные слоты
        for (Appointment appointment : appointments) {
            availableTimes.remove(appointment.getAppointmentDateTime());
        }

        return availableTimes;
    }

    private List<LocalDateTime> generateAllPossibleTimes() {
        // Предположим, что рабочий день начинается в 9:00 и заканчивается в 18:00
        LocalDateTime startTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(9, 0));
        LocalDateTime endTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(18, 0));

        // Создаем список для хранения всех возможных временных слотов
        List<LocalDateTime> allTimes = new ArrayList<>();

        // Генерируем все возможные временные слоты с интервалом в 30 минут
        while (startTime.isBefore(endTime)) {
            allTimes.add(startTime);
            startTime = startTime.plusMinutes(30); // Увеличиваем время на 30 минут
        }

        return allTimes;
    }

    private void saveAppointment(Appointment appointment) {
        appointmentRepository.save(appointment);
    }

    /* @Override
    public void onUpdateReceived(Update update) {
        var originalMessage = update.getMessage();
        log.debug(originalMessage.getText());

        var response = new SendMessage();
        response.setChatId(originalMessage.getChatId().toString());
        response.setText("Hello from bot");
        sendAnswerMessage(response);
    }

    public void sendAnswerMessage(SendMessage message) {
        if (message != null) {
            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error(e);
            }
        }
    }*/


}
