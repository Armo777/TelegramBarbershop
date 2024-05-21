package com.example.telegrambarbershop.controller;

import com.example.telegrambarbershop.config.BotConfig;
import com.example.telegrambarbershop.entity.*;
import com.example.telegrambarbershop.repositories.*;
import com.example.telegrambarbershop.service.AppointmentService;
import com.example.telegrambarbershop.service.BarberService;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Log4j
public class TelegramBotController extends TelegramLongPollingBot {
    private BotConfig config;

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

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AdminController adminController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MainAdminRepository mainAdminRepository;

//    @Autowired
//    private MainAdmin mainAdmin;

    private final AbsSender bot;

    private BarberService barberService;
    private List<Barber> barbers; // список барберов

    private boolean isAuthenticated = false;
    private String lastAdminCommand = null;

    private long adminChatId = -1;

    static final String HELP_TEXT = "Этот бот создан для демонстрации возможностей Spring \n\n" +
        "Вы можете выполнять команды из главного меню слева или введя команду: " +
            "Введите /start, чтобы увидеть приветственное сообщение\n\n" +
            "Введите /bookappointment, чтобы записаться к барберу\n\n" +
            "Введите /adminlogin, чтобы войти в админку\n\n" +
            "Введите /help, чтобы снова увидеть это сообщение";

    public TelegramBotController(@Lazy BotConfig config, @Lazy AbsSender bot) {
        this.config = config;
        this.bot = bot;
        //this.barberService = barberService;
        //this.barbers = barberService.getAllBarbers();
        List<BotCommand> listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand("/start", "получите приветственное сообщение"));
        listofCommands.add(new BotCommand("/help", "информация о том, как использовать этого бота"));
        listofCommands.add(new BotCommand("/bookappointment", "Записаться на прием"));
        listofCommands.add(new BotCommand("/adminlogin", "войти в административную панель"));
        listofCommands.add(new BotCommand("/mainadmin", "войти в главную административную панель"));
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

            registerUser(chatId);  // Регистрация пользователя при получении сообщения

            if (messageText != null) {
                if (messageText.contains("_")) {
                    handleMainAdminInput(chatId, messageText);
                } else {
                    switch (messageText) {
                        case "/start":
                            startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                            break;
                        case "/help":
                            sendMessage(chatId, HELP_TEXT);
                            break;
                        case "/bookappointment":
                            showBarbers(chatId);
                            break;
                        case "/adminlogin":
                            adminLogin(chatId);
                            break;
                        case "/mainadmin":
                            mainAdminLogin(chatId);
                            break;
                        default:
                            if (isMainAdmin(chatId)) {
                                adminController.handleAdminCommands(chatId, messageText);
                            } else if (messageText.startsWith("/")) {
                                sendMessage(chatId, "Извините, такой команды нет.");
                            } else {
                                sendMessage(chatId, "Извините, такой команды нет.");
                            }
                            break;
                    }
                }
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            handleCallbackQuery(update.getCallbackQuery(), callbackData, chatId, messageId);
        }
    }

    public void handleMainAdminInput(long chatId, String messageText) {
        String[] parts = messageText.split("_");

        // Проверка формата ввода для разных команд
        if (parts.length == 2 && isMainAdmin(chatId)) {
            // Проверка ввода логина и пароля для главного администратора
            String username = parts[0];
            String password = parts[1];
            if (isMainAdminCredentials(username, password)) {
                sendMessage(chatId, "Добро пожаловать, главный администратор!");
                setMainAdmin(chatId);
                showAdminOptions(chatId);
            } else {
                sendMessage(chatId, "Ошибка аутентификации. Пожалуйста, проверьте логин и пароль и попробуйте снова.");
            }
        } else if (parts.length == 4 && isMainAdmin(chatId)) {
            // Добавление барбера
            try {
                String name = parts[0];
                String phoneNumber = parts[1];
                String specialty = parts[2];
                double rating = Double.parseDouble(parts[3]);
                adminController.addBarber(name, phoneNumber, specialty, rating);
                sendMessage(chatId, "Барбер успешно добавлен.");
            } catch (NumberFormatException e) {
                sendMessage(chatId, "Неверный формат рейтинга.");
            }
        } else if (parts.length == 2 && isMainAdmin(chatId)) {
            // Добавление услуги
            try {
                String serviceName = parts[0];
                BigDecimal price = new BigDecimal(parts[1]);
                adminController.addService(serviceName, price);
                sendMessage(chatId, "Услуга успешно добавлена.");
            } catch (NumberFormatException e) {
                sendMessage(chatId, "Неверный формат цены.");
            }
        } else {
            sendMessage(chatId, "Неверный формат ввода.");
        }
    }

    private void processAdminAction(long chatId, String messageText) {
        if (lastAdminCommand != null) {
            switch (lastAdminCommand) {
                case "/addBarber":
                    String[] barberParams = messageText.split("_");
                    if (barberParams.length == 4) {
                        try {
                            adminController.addBarber(barberParams[0], barberParams[1], barberParams[2], Double.parseDouble(barberParams[3]));
                            sendMessage(chatId, "Барбер успешно добавлен.");
                        } catch (NumberFormatException e) {
                            sendMessage(chatId, "Неверный формат рейтинга. Пожалуйста, введите данные в формате Имя_НомерТелефона_Специальность_Рейтинг.");
                        }
                    } else {
                        sendMessage(chatId, "Неверный формат ввода для добавления барбера. Введите данные в формате Имя_НомерТелефона_Специальность_Рейтинг.");
                    }
                    break;
                case "/addService":
                    String[] serviceParams = messageText.split("_");
                    if (serviceParams.length == 2) {
                        try {
                            adminController.addService(serviceParams[0], BigDecimal.valueOf(Double.parseDouble(serviceParams[1])));
                            sendMessage(chatId, "Услуга успешно добавлена.");
                        } catch (NumberFormatException e) {
                            sendMessage(chatId, "Неверный формат цены. Пожалуйста, введите данные в формате Название_Цена.");
                        }
                    } else {
                        sendMessage(chatId, "Неверный формат ввода для добавления услуги. Введите данные в формате Название_Цена.");
                    }
                    break;
                // Добавьте другие случаи здесь
                default:
                    sendMessage(chatId, "Неизвестная команда администратора.");
                    break;
            }
            lastAdminCommand = null;  // Сброс последней команды
        } else {
            sendMessage(chatId, "Извините, такой команды нет.");
        }
    }

    private void setMainAdmin(long chatId) {
        User user = userRepository.findByChatId(chatId);
        if (user != null) {
            user.setAdmin(true);
            userRepository.save(user);
        }
    }

    private void showAdminOptions(long chatId) {
        List<String> adminOptions = Arrays.asList("/addBarber", "/editBarber", "/deleteBarber", "/addService", "/editService", "/deleteService", "/setWorkingDays", "/postAnnouncement", "/postPhoto", "/postVoice");
        StringBuilder options = new StringBuilder("Выберите команду администратора:\n");
        for (String option : adminOptions) {
            options.append(option).append("\n");
        }
        sendMessage(chatId, options.toString());
    }

    private void mainAdminCommands(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Добро пожаловать, главный администратор!");

        // Добавление кнопок команд для главного администратора
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("/addBarber");
        row1.add("/editBarber");
        row1.add("/deleteBarber");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("/addService");
        row2.add("/editService");
        row2.add("/deleteService");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("/setWorkingDays");
        row3.add("/postAnnouncement");
        row3.add("/postPhoto");
        row3.add("/postVoice");

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboardMarkup.setKeyboard(keyboard);

        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleUserCommands(Update update) {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        // Пример обработки команды пользователя
        if (messageText.equals("/start")) {
            registerUser(chatId);
            sendMessage(chatId, "Добро пожаловать! Чем я могу помочь?");
        } else {
            sendMessage(chatId, "Неизвестная команда.");
        }
    }

    private void registerUser(long chatId) {
        User user = userRepository.findByChatId(chatId);
        if (user == null) {
            user = new User();
            user.setChatId(chatId);
            user.setAdmin(false);  // По умолчанию не администратор
            userRepository.save(user);
        }
    }

    private boolean isAdmin(long chatId) {
        User user = userRepository.findByChatId(chatId);
        return user != null && user.isAdmin();
    }



    public void handleUpdate(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            if (messageText.startsWith("/createMonthly")) {
                String[] parts = messageText.split(" ");
                if (parts.length > 1) {
                    Long barberId = Long.parseLong(parts[1]);
                    createMonthlyAppointments(chatId, Math.toIntExact(barberId));
                } else {
                    sendMessage(chatId, "Используйте: /createMonthly <barberId>");
                }
            } else if (messageText.startsWith("/getAppointmentsForDay")) {
                String[] parts = messageText.split(" ");
                if (parts.length > 2) {
                    Long barberId = Long.parseLong(parts[1]);
                    String day = parts[2];
                    getAppointmentsForDay(chatId, barberId, day);
                } else {
                    sendMessage(chatId, "Используйте: /getAppointmentsForDay <barberId> <дата в формате YYYY-MM-DD>");
                }
            } else {
                sendMessage(chatId, "Неизвестная команда.");
            }
        }
    }

    private void createMonthlyAppointments(Long chatId, Integer barberId) {
        List<Appointment> appointments = appointmentService.createMonthlyAppointments(Long.valueOf(barberId));
        sendMessage(chatId, "Создано " + appointments.size() + " записей на месяц вперед.");
    }

    private void getAppointmentsForDay(Long chatId, Long barberId, String day) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(day + "T00:00:00");
            List<Appointment> appointments = appointmentService.getAppointmentsForDay(barberId, dateTime);
            if (appointments.isEmpty()) {
                sendMessage(chatId, "Записей на " + day + " не найдено.");
            } else {
                String appointmentsList = appointments.stream()
                        .map(a -> a.getAppointmentDateTime().toString())
                        .collect(Collectors.joining("\n"));
                sendMessage(chatId, "Записи на " + day + ":\n" + appointmentsList);
            }
        } catch (Exception e) {
            sendMessage(chatId, "Неверный формат даты. Используйте: YYYY-MM-DD");
        }
    }

    public void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void handleCallbackQuery(CallbackQuery callbackQuery, String data, long chatId, long messageId) {
        String callbackData = callbackQuery.getData();
        if (callbackData.startsWith("barber")) {
            String[] dataParts = callbackData.split("_");
            if (dataParts.length != 2) {
                sendMessage(chatId, "Ошибка обработки данных барбера.");
                return;
            }
            Integer barberId = Integer.parseInt(dataParts[1]);
            showServices(chatId, barberId);
        } else if (callbackData.startsWith("service")) {
            String[] dataParts = callbackData.split("_");
            if (dataParts.length != 3) {
                sendMessage(chatId, "Ошибка обработки данных услуги.");
                return;
            }
            Integer serviceId = Integer.parseInt(dataParts[1]);
            Integer barberId = Integer.parseInt(dataParts[2]);
            showAvailableDays(chatId, barberId, serviceId);
        } else if (callbackData.startsWith("day")) {
            String[] dataParts = callbackData.split("_");
            if (dataParts.length != 4) {
                sendMessage(chatId, "Ошибка обработки данных дня.");
                return;
            }
            LocalDate date = LocalDate.parse(dataParts[1]);
            Integer barberId = Integer.parseInt(dataParts[2]);
            Integer serviceId = Integer.parseInt(dataParts[3]);
            showAvailableTimeSlots(chatId, date, barberId, serviceId);
        } else if (callbackData.startsWith("slot")) {
            String[] dataParts = callbackData.split("_");
            if (dataParts.length != 4) {
                sendMessage(chatId, "Ошибка обработки данных временного слота.");
                return;
            }
            String slot = dataParts[1];
            Integer barberId = Integer.parseInt(dataParts[2]);
            Integer serviceId = Integer.parseInt(dataParts[3]);
            LocalDateTime slotDateTime = LocalDateTime.parse(slot, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            appointmentController.createAppointment(slotDateTime, barberId, serviceId, callbackQuery.getFrom().getFirstName());
            registrationRecord(chatId, callbackQuery.getFrom().getFirstName());
        } else if (callbackData.startsWith("back_barber")) {
            showBarbers(chatId);
        } else if (callbackData.startsWith("back_service")) {
            String[] dataParts = callbackData.split("_");
            if (dataParts.length != 3) {
                sendMessage(chatId, "Ошибка обработки данных для кнопки 'Назад' на этапе выбора услуги.");
                return;
            }
            Integer barberId = Integer.parseInt(dataParts[2]);
            showServices(chatId, barberId);
        } else if (callbackData.startsWith("back_day")) {
            String[] dataParts = callbackData.split("_");
            if (dataParts.length != 4) {
                sendMessage(chatId, "Ошибка обработки данных для кнопки 'Назад' на этапе выбора дня.");
                return;
            }
            Integer barberId = Integer.parseInt(dataParts[2]);
            Integer serviceId = Integer.parseInt(dataParts[3]);
            showAvailableDays(chatId, barberId, serviceId);
        } else {
            sendMessage(chatId, "Некорректный выбор.");
        }
    }

//    public void createOrUpdateAppointment(LocalDateTime appointmentDateTime, Barber barber, Service service, String userName) {
//        Appointment existingAppointment = appointmentRepository.findByBarberAndAppointmentDateTime(barber, appointmentDateTime);
//
//        if (existingAppointment != null) {
//            // Обновляем существующую запись Appointment
//            existingAppointment.setService(service);
//            existingAppointment.setNameUser(userName);
//            appointmentRepository.save(existingAppointment);
//        } else {
//            // Создаем новую запись Appointment
//            Appointment newAppointment = new Appointment();
//            newAppointment.setAppointmentDateTime(appointmentDateTime);
//            newAppointment.setBarber(barber);
//            newAppointment.setService(service);
//            newAppointment.setNameUser(userName);
//            appointmentRepository.save(newAppointment);
//        }
//    }


//    private void handleUserCallback(Update update, String callbackData) {
//        String[] dataParts = callbackData.split("_");
//        String choiceType = dataParts[0];
//
//        if (choiceType.equals("barber")) {
//            // Пользователь выбрал барбера, показываем список услуг
//            Integer barberId = Integer.parseInt(dataParts[1]);
//            showServices(update.getCallbackQuery().getMessage().getChatId(), barberId);
//        } else if (choiceType.equals("service")) {
//            // Пользователь выбрал услугу, показываем доступные временные слоты
//            Integer serviceId = Integer.parseInt(dataParts[1]);
//            showAvailableTimeSlots(update.getCallbackQuery().getMessage().getChatId(), LocalDateTime.now(), 1, serviceId);
//        } else if (choiceType.equals("slot")) {
//            // Пользователь выбрал временной слот, создаем запись
//            String dataTime = dataParts[1];
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd' 'HH:mm");
//            LocalDateTime dateTimeFormat = LocalDateTime.parse(dataTime, formatter);
//            appointmentController.createAppointment(dateTimeFormat, Integer.parseInt(dataParts[2]), 1, update.getCallbackQuery().getFrom().getFirstName());
//            registrationRecord(update.getCallbackQuery().getMessage().getChatId(), update.getCallbackQuery().getFrom().getFirstName());
//        }
//    }

    private void showAvailableTimeSlots(long chatId, LocalDate date, Integer barberId, Integer serviceId) {
        Map<LocalDate, List<String>> availableTimeSlotsMap = appointmentController.getAvailableTimeSlots(barberId, serviceId);

        List<String> availableTimeSlots = availableTimeSlotsMap.getOrDefault(date, new ArrayList<>());
        if (availableTimeSlots.isEmpty()) {
            sendMessage(chatId, "На выбранную дату нет доступных слотов для записи.");
        } else {
            InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
            for (String slot : availableTimeSlots) {
                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(slot);
                button.setCallbackData("slot_" + slot + "_" + barberId + "_" + serviceId);
                rowInline.add(button);
                rowsInline.add(rowInline);
            }

            // Добавляем кнопку "Назад"
            List<InlineKeyboardButton> backButtonRow = new ArrayList<>();
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("Назад");
            backButton.setCallbackData("back_day_" + barberId + "_" + serviceId);
            backButtonRow.add(backButton);
            rowsInline.add(backButtonRow);

            markupInline.setKeyboard(rowsInline);
            SendMessage message = new SendMessage(String.valueOf(chatId), "Выберите удобное время для записи:");
            message.setReplyMarkup(markupInline);
            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error("Произошла ошибка: " + e.getMessage());
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

    public List<Appointment> getAppointmentsForBarber(Long barberId) {
        return appointmentRepository.findByBarberId(barberId);
    }

    private void viewAppointments(long chatId, Long barberId) {
        List<Appointment> appointments = getAppointmentsForBarber(barberId);
        StringBuilder messageText = new StringBuilder();
        messageText.append("Список записей:\n");

        for (Appointment appointment : appointments) {
            messageText.append("Дата и время: ").append(appointment.getAppointmentDateTime().toString()).append("\n");
            messageText.append("Услуга: ").append(appointment.getService().getServiceName()).append("\n");
            messageText.append("Имя клиента: ").append(appointment.getName()).append("\n\n");
        }

        sendMessage(chatId, messageText.toString());
    }

    private void handleAdminCredentials(long chatId, String credentials) {
        String[] parts = credentials.split("_");
        if (parts.length == 2) {
            String login = parts[0];
            String password = parts[1];
            BarberAdmin admin = barberAdminRepository.findByUsername(login);
            if (admin != null && admin.getPassword().equals(password)) {
                Barber barber = barberRepository.findByAdminId(admin.getId());
                if (barber != null) {
                    viewAppointments(chatId, Long.valueOf(barber.getId()));
                } else {
                    sendMessage(chatId, "Не удалось найти связанные записи для этого администратора.");
                }
            } else {
                sendMessage(chatId, "Ошибка аутентификации. Пожалуйста, проверьте логин и пароль и попробуйте снова.");
            }
        } else {
            sendMessage(chatId, "Неправильный формат логина и пароля. Введите их через символ '_'. Например, 'admin_password'.");
        }
    }

    private void showServices(long chatId, Integer barberId) {
        List<Service> services = serviceRepository.findAll();
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (Service service : services) {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(service.getServiceName());
            button.setCallbackData("service_" + service.getId() + "_" + barberId);
            rowInline.add(button);
            rowsInline.add(rowInline);
        }

        // Добавляем кнопку "Назад"
        List<InlineKeyboardButton> backButtonRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("Назад");
        backButton.setCallbackData("back_barber");
        backButtonRow.add(backButton);
        rowsInline.add(backButtonRow);

        markupInline.setKeyboard(rowsInline);
        SendMessage message = new SendMessage(String.valueOf(chatId), "Выберите услугу:");
        message.setReplyMarkup(markupInline);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Произошла ошибка: " + e.getMessage());
        }
    }

    private void showAvailableDays(long chatId, Integer barberId, Integer serviceId) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (int i = 0; i < 30; i++) {
            LocalDate day = LocalDate.now().plusDays(i);
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(day.toString());
            button.setCallbackData("day_" + day.toString() + "_" + barberId + "_" + serviceId);
            rowInline.add(button);
            rowsInline.add(rowInline);
        }

        // Добавляем кнопку "Назад"
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
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Произошла ошибка: " + e.getMessage());
        }
    }

    private void showBarbers(long chatId) {
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
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Произошла ошибка: " + e.getMessage());
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

//    public void sendMessage(long chatId, String textToSend) {
//        SendMessage message = new SendMessage();
//        message.setChatId(String.valueOf((chatId)));
//        message.setText(textToSend);
//
//        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
//        List<KeyboardRow> keyboardRows = new ArrayList<>();
//        KeyboardRow row = new KeyboardRow();
//        row.add("Погода");
//        row.add("получите случайную шутку");
//        keyboardRows.add(row);
//        row = new KeyboardRow();
//        row.add("зарегистрировать");
//        row.add("проверьте мои данные");
//        row.add("удалите мои данные");
//        keyboardRows.add(row);
//        keyboardMarkup.setKeyboard(keyboardRows);
//        message.setReplyMarkup(keyboardMarkup);
//
//        try {
//            execute(message);
//        }
//        catch (TelegramApiException e) {
//            log.error("Произошла ошибка: " + e.getMessage());
//        }
//    }

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

    private void mainAdminLogin(long chatId) {
        String messageText = "Пожалуйста, введите логин и пароль главного администратора в формате: логин_пароль";
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(messageText);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void mainHandleAdminCredentials(long chatId, String credentials) {
        String[] parts = credentials.split("_");
        if (parts.length == 2) {
            String login = parts[0];
            String password = parts[1];
            MainAdmin mainAdmin = mainAdminRepository.findByUsername(login);
            if (mainAdmin != null && mainAdmin.getPassword().equals(password)) {
                sendMessage(chatId, "Вы успешно вошли в систему как главный администратор.");
                // Логика для отображения главной административной панели
            } else {
                sendMessage(chatId, "Неверный логин или пароль.");
            }
        } else {
            sendMessage(chatId, "Неверный формат. Пожалуйста, введите логин и пароль в формате: логин_пароль");
        }
    }

    private boolean isMainAdminCredentials(String username, String password) {
        MainAdmin mainAdmin = mainAdminRepository.findByUsername(username);
        return mainAdmin != null && mainAdmin.getPassword().equals(password);
    }

    private boolean isMainAdmin(long chatId) {
        User user = userRepository.findByChatId(chatId);
        return user != null && user.isAdmin();
    }


    public List<Appointment> getAppointmentsForBarber(int barberId) {
        return appointmentRepository.findByBarberId(barberId);
    }

    public Barber getBarberByAdminId(int adminId) {
        return barberRepository.findByAdminId(adminId);
    }
}
