package com.example.telegrambarbershop.controller;

import com.example.telegrambarbershop.config.BotConfig;
import com.example.telegrambarbershop.entity.*;
import com.example.telegrambarbershop.repositories.*;
import com.example.telegrambarbershop.service.AppointmentService;
import com.example.telegrambarbershop.service.BarberService;
import com.example.telegrambarbershop.service.ReviewService;
import com.example.telegrambarbershop.service.TelegramNotificationService;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private TelegramNotificationService telegramNotificationService;

    private final AbsSender bot;

    private BarberService barberService;
    private List<Barber> barbers; // список барберов

    private boolean isAuthenticated = false;
    public String lastAdminCommand = null;

    private long adminChatId = -1;

    private final Map<Long, UserRating> userRatingMap = new ConcurrentHashMap<>();

    private ReviewSessionManager sessionManager = new ReviewSessionManager();

    private Map<Long, UserReviewSession> userReviewSessionMap = new HashMap<>();

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
        if (update.hasMessage() && update.getMessage() != null) {
            Message message = update.getMessage();
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            registerUser(chatId);  // Регистрация пользователя при получении сообщения

            boolean isReviewHandled = false;

//            handleTextMessage(chatId, messageText, update);

            if (userRatingMap.containsKey(chatId)) {
                UserRating userRating = userRatingMap.get(chatId);

                if ("/skip".equals(messageText)) {
                    // Если пользователь выбрал пропуск отзыва, устанавливаем пустой комментарий
                    reviewService.handleReview(userRating.getRating(), "", userRating.getAppointmentId());

                    // Подтверждаем пользователю, что отзыв пропущен
                    sendMessage(chatId, "Комментарий пропущен.");
                    userRatingMap.remove(chatId);
                    isReviewHandled = true;
                } else if (messageText != null && messageText.startsWith("/")) {
                    // Если пользователь вводит другую команду, когда ожидается отзыв
                    sendMessage(chatId, "Вы не завершили оставление отзыва. Пожалуйста, напишите ваш отзыв или напишите /skip, чтобы пропустить.");
                    return;
                } else {
                    // Сохраняем отзыв с текстом комментария
                    reviewService.handleReview(userRating.getRating(), messageText, userRating.getAppointmentId());

                    // Подтверждаем пользователю сохранение отзыва
                    sendMessage(chatId, "Спасибо за ваш отзыв!");
                    isReviewHandled = true;
                }

                // После обработки отзыва, оставляем запись в Map, но обновляем ее (например, можем сбросить комментарий)
                // userRatingMap.put(chatId, updatedUserRating); // если требуется обновление
            }
//            UserReviewSession userReviewSession = userReviewSessionMap.get(chatId);
//
//            if (userReviewSession != null && userReviewSession.isAwaitingReview()) {
//                // Вызываем метод handleReview для сохранения отзыва и оценки
//                reviewService.handleReview(userReviewSession.getBarberId(), userReviewSession.getRating(), messageText, userReviewSession.getAppointmentId());
//
//                // Удаляем запись из Map, так как отзыв сохранен
//                userReviewSessionMap.remove(chatId);
//
//                // Подтверждаем пользователю сохранение отзыва
//                sendMessage(chatId, "Спасибо за ваш отзыв!");
//                isReviewHandled = true;
//            }

            if (!isReviewHandled && messageText != null && messageText.startsWith("/")) {
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
                        lastAdminCommand = "/adminlogin";
                        break;
                    case "/mainadmin":
                        mainAdminLogin(chatId);
                        lastAdminCommand = "/mainadmin";
                        break;
                    case "/skip":
                        sendMessage(chatId, "Комментарий пропущен.");
                        break;
                    case "/viewBarbers":
                    case "/viewServices":
                    case "/viewAppointments":
                    case "/viewBarberAdmins":
                    case "/addBarber":
                    case "/editBarber":
                    case "/deleteBarber":
                    case "/addService":
                    case "/editService":
                    case "/deleteService":
                    case "/addBarberAdmin":
                    case "/updateBarberAdmin":
                    case "/deleteBarberAdmin":
                    case "/setWorkingDays":
                    case "/postAnnouncement":
                    case "/postPhoto":
                    case "/postVoice":
                    case "/postVideoNote":
                    case "/viewRequest":
                    case "/viewReview":
                        if (isAuthenticated && chatId == adminChatId) {
                            lastAdminCommand = messageText;
                            adminController.handleAdminCommands(chatId, messageText);
                            //sendMessage(chatId, "Введите данные в соответствующем формате.");
                        } else {
                            sendMessage(chatId, "Вы не авторизованы как главный администратор.");
                        }
                        break;
                    default:
                        sendMessage(chatId, "Извините, такой команды нет.");
                        break;
                }
            } else if (!isReviewHandled && messageText != null) {
                if ("/adminlogin".equals(lastAdminCommand)) {
                    handleAdminCredentials(chatId, messageText);
                } else if ("/mainadmin".equals(lastAdminCommand)) {
                    handleMainAdminInput(chatId, messageText);
                } else {
                    handleMainAdminInput(chatId, messageText);
                }
            }

            if (message.hasPhoto()) {
                if ("/postPhoto".equals(lastAdminCommand)) {
                    List<PhotoSize> photos = message.getPhoto();
                    String fileId = photos.get(photos.size() - 1).getFileId();
                    String caption = message.getCaption() != null ? message.getCaption() : "";
                    adminController.sendPhotoMessageToAll(caption, fileId);
                    sendMessage(chatId, "Фото успешно опубликовано.");
                    lastAdminCommand = null;
                }
            } else if (message.hasVoice()) {
                if ("/postVoice".equals(lastAdminCommand)) {
                    String fileId = message.getVoice().getFileId();
                    adminController.sendVoiceMessageToAll(fileId);
                    sendMessage(chatId, "Голосовое сообщение успешно опубликовано.");
                    lastAdminCommand = null;
                }
            } else if (message.hasVideoNote()) {
                if ("/postVideoNote".equals(lastAdminCommand)) {
                    String fileId = message.getVideoNote().getFileId();
                    adminController.sendVideoNoteToAll(fileId);
                    sendMessage(chatId, "Видеосообщение успешно опубликовано.");
                    lastAdminCommand = null;
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
        // Проверка аутентификации
        if (isAuthenticated && chatId == adminChatId) {
            handleAdminCommandInput(chatId, messageText);
        } else {

            if (messageText.contains("_")) {
                String[] credentials = messageText.split("_");
                if (credentials.length == 2) {
                    if (isMainAdminCredentials(credentials[0], credentials[1])) {
                        sendMessage(chatId, "Добро пожаловать, главный администратор!");
                        isAuthenticated = true;
                        adminChatId = chatId;
                        showAdminOptions(chatId);
                    } else {
                        sendMessage(chatId, "Ошибка аутентификации. Пожалуйста, проверьте логин и пароль и попробуйте снова.");
                    }
                } else {
                    sendMessage(chatId, "Неверный формат ввода.");
                }
            } else {
                sendMessage(chatId, "Извините, такой команды нет.");
            }
        }
    }

    private void handleAdminCommandInput(long chatId, String messageText) {
        if (messageText.contains("_")) {
            String[] credentials = messageText.split("_");
            if (credentials.length == 2 && isMainAdminCredentials(credentials[0], credentials[1])) {
                sendMessage(chatId, "Добро пожаловать, главный администратор!");
                isAuthenticated = true;
                adminChatId = chatId;
                showAdminOptions(chatId);
                return;
            }
        }

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
                case "/editBarber":
                    String[] editBarberParams = messageText.split("_");
                    if (editBarberParams.length == 5) {
                        try {
                            adminController.editBarber(Integer.parseInt(editBarberParams[0]), editBarberParams[1], editBarberParams[2], editBarberParams[3], Double.parseDouble(editBarberParams[4]));
                            sendMessage(chatId, "Барбер успешно отредактирован.");
                        } catch (NumberFormatException e) {
                            sendMessage(chatId, "Неверный формат ID или рейтинга. Пожалуйста, введите данные в формате ID_Имя_НомерТелефона_Специальность_Рейтинг.");
                        }
                    } else {
                        sendMessage(chatId, "Неверный формат ввода для редактирования барбера. Введите данные в формате ID_Имя_НомерТелефона_Специальность_Рейтинг.");
                    }
                    break;
                case "/deleteBarber":
                    try {
                        long barberId = Long.parseLong(messageText);
                        adminController.deleteBarber((int) barberId);
                        sendMessage(chatId, "Барбер успешно удален.");
                    } catch (NumberFormatException e) {
                        sendMessage(chatId, "Неверный формат ID. Пожалуйста, введите ID барбера.");
                    }
                    break;
                case "/addService":
                    String[] serviceParams = messageText.split("_");
                    if (serviceParams.length == 3) {
                        try {
                            adminController.addService(serviceParams[0], BigDecimal.valueOf(Double.parseDouble(serviceParams[1])), Integer.parseInt(serviceParams[2]));
                            sendMessage(chatId, "Услуга успешно добавлена.");
                        } catch (NumberFormatException e) {
                            sendMessage(chatId, "Неверный формат цены. Пожалуйста, введите данные в формате Название_Цена_Таймер.");
                        }
                    } else {
                        sendMessage(chatId, "Неверный формат ввода для добавления услуги. Введите данные в формате Название_Цена_Таймер.");
                    }
                    break;
                case "/editService":
                    String[] editServiceParams = messageText.split("_");
                    if (editServiceParams.length == 4) {
                        try {
                            adminController.editService(Integer.parseInt(editServiceParams[0]), editServiceParams[1], BigDecimal.valueOf(Double.parseDouble(editServiceParams[2])), Integer.parseInt(editServiceParams[3]));
                            sendMessage(chatId, "Услуга успешно отредактирована.");
                        } catch (NumberFormatException e) {
                            sendMessage(chatId, "Неверный формат ID или цены. Пожалуйста, введите данные в формате ID_Название_Цена.");
                        }
                    } else {
                        sendMessage(chatId, "Неверный формат ввода для редактирования услуги. Введите данные в формате ID_Название_Цена.");
                    }
                    break;
                case "/deleteService":
                    try {
                        long serviceId = Long.parseLong(messageText);
                        adminController.deleteService((int) serviceId);
                        sendMessage(chatId, "Услуга успешно удалена.");
                    } catch (NumberFormatException e) {
                        sendMessage(chatId, "Неверный формат ID. Пожалуйста, введите ID услуги.");
                    }
                    break;
                case "/addBarberAdmin":
                    String[] adminParams = messageText.split("_");
                    if (adminParams.length == 3) {
                        try {
                            adminController.addBarberAdmin(adminParams[0], adminParams[1], Long.parseLong(adminParams[2]));
                            sendMessage(chatId, "Администратор барбера успешно добавлен.");
                        } catch (Exception e) {
                            sendMessage(chatId, "Ошибка при добавлении администратора барбера. Пожалуйста, попробуйте снова.");
                        }
                    } else {
                        sendMessage(chatId, "Неверный формат ввода для добавления администратора барбера. Введите данные в формате Логин_Пароль.");
                    }
                    break;
                case "/updateBarberAdmin":
                    String[] updateData = messageText.split("_");

                    BarberAdmin updatedBarberAdmin = new BarberAdmin();
                    updatedBarberAdmin.setUsername(updateData[0]);
                    updatedBarberAdmin.setPassword(updateData[1]);
                    Long updateId = Long.valueOf(updateData[2]);
                    // Предположим, что здесь используется ID барбера, которого нужно назначить администратором
                    updatedBarberAdmin.setBarber(barberRepository.findById((int) Long.parseLong(updateData[2])).orElse(null));
                    adminController.updateBarberAdmin(updateId, updatedBarberAdmin);
                    sendMessage(chatId, "Администратор барбера успешно обновлен.");
                    break;
                case "/deleteBarberAdmin":
                    Long deleteId = Long.parseLong(messageText);
                    adminController.deleteBarberAdmin(deleteId);
                    sendMessage(chatId, "Администратор барбера успешно удален.");
                    break;
                case "/postAnnouncement":
                    adminController.postAnnouncement(messageText);
                    sendMessage(chatId, "Объявление успешно опубликовано.");
                    break;
                case "/postPhoto":
                    sendMessage(chatId, "Пришлите фото для публикации.");
                    break;
                case "/postVoice":
                    sendMessage(chatId, "Пришлите голосовое сообщение для публикации.");
                    break;
                case "/viewApplications":
                    // Добавьте код для обработки команды /viewApplications
                    showRequest(chatId);
                    break;
                case "/viewReview":
                    // Добавьте код для обработки команды /viewApplications
                    showReview(chatId);
                    break;
                default:
                    sendMessage(chatId, "Неизвестная команда администратора.");
                    break;
            }
            lastAdminCommand = null;
        } else {
            sendMessage(chatId, "Извините, такой команды нет.");
        }
    }

    private void showAdminOptions(long chatId) {
        List<String> adminOptions = Arrays.asList(
                "/viewBarbers - Просмотр барберов",
                "/viewServices - Просмотр услуг",
                "/viewAppointments - Просмотр записей",
                "/viewBarberAdmins - Просмотр администраторов барберов",
                "/addBarber - Добавить барбера",
                "/editBarber - Редактировать барбера",
                "/deleteBarber - Удалить барбера",
                "/addService - Добавить услугу",
                "/editService - Редактировать услугу",
                "/deleteService - Удалить услугу",
                "/addBarberAdmin - Добавить администратора барбера",
                "/updateBarberAdmin - Обновить администратора барбера",
                "/deleteBarberAdmin - Удалить администратора барбера",
                "/setWorkingDays - Установить рабочие дни",
                "/postAnnouncement - Разместить объявление",
                "/postPhoto - Разместить фото",
                "/postVoice - Разместить голосовое сообщение",
                "/postVideoNote - Разместить видео",
                "/viewRequest - Просмотр заявок",
                "/viewReview - Просмотр отзывов"
        );

        StringBuilder optionsDescription = new StringBuilder("Выберите команду администратора:\n");
        for (String option : adminOptions) {
            optionsDescription.append(option).append("\n");
        }
        sendMessage(chatId, optionsDescription.toString());

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Также добавлен дополнительная боковая панель");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        for (String option : adminOptions) {
            KeyboardRow row = new KeyboardRow();
            row.add(option.split(" - ")[0]); // Используем только команду без описания
            keyboard.add(row);
        }

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void showRequest(long chatId) {
        List<Request> applications = requestRepository.findAll();
        if (applications.isEmpty()) {
            sendMessage(chatId, "Нет новых заявок.");
        } else {
            StringBuilder applicationsList = new StringBuilder("Список заявок:\n");
            for (Request application : applications) {
                applicationsList.append("ID: ").append(application.getId())
                        .append("\nИмя: ").append(application.getName())
                        .append("\nТелефон: ").append(application.getPhone())
                        .append("\nДата и время: ").append(application.getTimestamp())
                        .append("\n\n");
            }
            sendMessage(chatId, applicationsList.toString());
        }
    }

    public void showReview(long chatId) {
        List<Review> Reviews = reviewRepository.findAll();
        if (Reviews.isEmpty()) {
            sendMessage(chatId, "Нету отзывов.");
        } else {
            StringBuilder applicationsList = new StringBuilder("Список отзывов:\n");
            for (Review Review : Reviews) {
                applicationsList.append("ID: ").append(Review.getId())
                        .append("\nкомментарий: ").append(Review.getComment())
                        .append("\nДата и время: ").append(Review.getCreatedAt())
                        .append("\nОценка: ").append(Review.getRating())
                        .append("\nID записи: ").append(Review.getAppointment())
                        .append("\nID барбера: ").append(Review.getBarber())
                        .append("\n\n");
            }
            sendMessage(chatId, applicationsList.toString());
        }
    }

    private void registerUser(long chatId) {
        User user = userRepository.findByChatId(chatId);
        if (user == null) {
            user = new User();
            user.setChatId(chatId);
            user.setAdmin(false);
            userRepository.save(user);
        }
    }

    private boolean isAdmin(long chatId) {
        User user = userRepository.findByChatId(chatId);
        return user != null && user.isAdmin();
    }

    private void createMonthlyAppointments(Long chatId, Integer barberId) {
        List<Appointment> appointments = appointmentService.createMonthlyAppointments(Long.valueOf(barberId));
        sendMessage(chatId, "Создано " + appointments.size() + " записей на месяц вперед.");
    }

    private void getAppointmentsForDay(Long chatId, Long barberId, String day) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(day + " 00:00:00");
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
        Long userId = callbackQuery.getFrom().getId();
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
            appointmentController.createAppointment(slotDateTime, barberId, serviceId, callbackQuery.getFrom().getFirstName(), userId);
            registrationRecord(chatId, callbackQuery.getFrom().getFirstName(), slotDateTime.toLocalDate().toString(), slotDateTime.toLocalTime().toString());
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
        }  else if (callbackData.startsWith("review_yes_")) {
            // Пользователь выбрал "Да", теперь предлагаем оценить услугу
            String[] dataParts = callbackData.split("_");
            if (dataParts.length == 3) {
                Integer appointmentId = Integer.valueOf(dataParts[2]);

                String message = "Пожалуйста, оцените услугу от 1 до 5.";
                InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

                // Создаем кнопки для оценки от 1 до 5
                for (int i = 1; i <= 5; i++) {
                    InlineKeyboardButton button = new InlineKeyboardButton(String.valueOf(i));
                    button.setCallbackData("rating_" + i + "_" + appointmentId);
                    keyboard.add(Collections.singletonList(button));
                }
                inlineKeyboardMarkup.setKeyboard(keyboard);

                telegramNotificationService.sendMessageWithKeyboard(chatId, message, inlineKeyboardMarkup);
            }
        } else if (callbackData.startsWith("review_no_")) {
            // Пользователь выбрал "Нет"
            sendMessage(chatId, "Спасибо! Мы не будем вас больше беспокоить по этому вопросу.");

            // Сохраняем состояние, чтобы больше не беспокоить пользователя
            String[] dataParts = callbackData.split("_");
            if (dataParts.length == 3) {
                Integer appointmentId = Integer.valueOf(dataParts[2]);
                Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
                if (appointment != null) {
                    appointment.setReviewRequestSent(true);
                    appointmentRepository.save(appointment);
                }
            }
        } else if (callbackData.startsWith("rating_")) {
            // Обработка оценки
            String[] dataParts = callbackData.split("_");
            if (dataParts.length == 3) {
                double rating = Integer.parseInt(dataParts[1]);
                Integer appointmentId = Integer.valueOf(dataParts[2]);

                userRatingMap.put(chatId, new UserRating(rating, appointmentId));
                sendMessage(chatId, "Спасибо за оценку! Пожалуйста, напишите ваш отзыв, если хотите.");
            }
        }else {
            sendMessage(chatId, "Некорректный выбор.");
        }
    }

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

            String buttonText = String.format("%s\nЦена: %.2f\nВремя: %d мин",
                    service.getServiceName(), service.getPrice(), service.getDurationMinutes());

            button.setText(buttonText);
            button.setCallbackData("service_" + service.getId() + "_" + barberId);
            rowInline.add(button);
            rowsInline.add(rowInline);
        }

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

    private void startCommandReceived(long chatId, String name) {
        if (name != null) {
            String answer = "Привет, " + name + " приятно познакомиться!";
            log.info("Ответил пользователю " + name);
            sendMessage(chatId, answer);
        } else {
            log.error("Получено пустое имя пользователя.");
        }
    }

    private void registrationRecord(long chatId, String name, String appointmentDate, String appointmentTime) {
        if (name != null) {
            sendAppointmentConfirmation(chatId, name, appointmentDate, appointmentTime);
        } else {
            log.error("Получено пустое имя пользователя.");
        }
    }

    public void sendAppointmentConfirmation(Long chatId, String userName, String appointmentDate, String appointmentTime) {
        String confirmationMessage = String.format(
                "Вас приветствует Барбершоп CROP. Я система оповещения R2-D2.\n\n" +
                        "Уважаемый(ая) %s, ждем вас в Барбершоп CROP %s в %s.\n\n" +
                        "Если возникла аварийная ситуация и вы не сможете явиться в назначенное время, то наберите +79515161121.",
                userName, appointmentDate, appointmentTime
        );
        sendMessage(chatId, confirmationMessage);
    }

    public void adminLogin(long chatId) {
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
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));

        if (!isMainAdmin(chatId)) {
            message.setText("Пожалуйста, введите логин и пароль главного администратора в формате: логин_пароль");
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }

        if (isMainAdmin(chatId)) {
            message.setText("Добро пожаловать, главный администратор!\n\nВыберите команду администратора:");
            message.setReplyMarkup(getMainAdminCommandsKeyboard());
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private InlineKeyboardMarkup getMainAdminCommandsKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Создаем кнопки для всех команд
        rows.add(Collections.singletonList(createInlineButton("/viewBarbers", "Просмотр Барберов")));
        rows.add(Collections.singletonList(createInlineButton("/viewServices", "Просмотр Услуг")));
        rows.add(Collections.singletonList(createInlineButton("/viewAppointments", "Просмотр Записей")));
        rows.add(Collections.singletonList(createInlineButton("/viewBarberAdmins", "Просмотр Администраторов Барберов")));
        rows.add(Collections.singletonList(createInlineButton("/addBarber", "Добавить Барбера")));
        rows.add(Collections.singletonList(createInlineButton("/editBarber", "Редактировать Барбера")));
        rows.add(Collections.singletonList(createInlineButton("/deleteBarber", "Удалить Барбера")));
        rows.add(Collections.singletonList(createInlineButton("/addService", "Добавить Услугу")));
        rows.add(Collections.singletonList(createInlineButton("/editService", "Редактировать Услугу")));
        rows.add(Collections.singletonList(createInlineButton("/deleteService", "Удалить Услугу")));
        rows.add(Collections.singletonList(createInlineButton("/addBarberAdmin", "Добавить Администратора Барбера")));
        rows.add(Collections.singletonList(createInlineButton("/updateBarberAdmin", "Обновить Администратора Барбера")));
        rows.add(Collections.singletonList(createInlineButton("/deleteBarberAdmin", "Удалить Администратора Барбера")));
        rows.add(Collections.singletonList(createInlineButton("/setWorkingDays", "Установить Рабочие Дни")));
        rows.add(Collections.singletonList(createInlineButton("/postAnnouncement", "Опубликовать Объявление")));
        rows.add(Collections.singletonList(createInlineButton("/postPhoto", "Опубликовать Фото")));
        rows.add(Collections.singletonList(createInlineButton("/postVoice", "Опубликовать Голосовое Сообщение")));
        rows.add(Collections.singletonList(createInlineButton("/postVideoNote", "Опубликовать Видео Сообщение")));
        rows.add(Collections.singletonList(createInlineButton("/viewRequest", "Просмотр поступающих заявок")));
        rows.add(Collections.singletonList(createInlineButton("/viewReview", "Просмотр отзывов")));

        markup.setKeyboard(rows);
        return markup;
    }

    private InlineKeyboardButton createInlineButton(String command, String text) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(command);
        return button;
    }

    private boolean isMainAdminCredentials(String username, String password) {
        MainAdmin mainAdmin = mainAdminRepository.findByUsername(username);
        return mainAdmin != null && mainAdmin.getPassword().equals(password);
    }

    private boolean isMainAdmin(long chatId) {
        User user = userRepository.findByChatId(chatId);
        return user != null && user.isAdmin();
    }
}
