package com.example.telegrambarbershop.config;

import com.example.telegrambarbershop.controller.TelegramBotController;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Log4j
@Component
public class BotInitializer {

    private final TelegramBotController botController;

    @Autowired
    public BotInitializer(@Lazy TelegramBotController botController) {
        this.botController = botController;
    }

    @EventListener({ContextRefreshedEvent.class})
    public void init() throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        try {
            telegramBotsApi.registerBot(botController);
        }
        catch (TelegramApiException e) {
            log.error("Произошла ошибка: " + e.getMessage());
        }
    }

}
