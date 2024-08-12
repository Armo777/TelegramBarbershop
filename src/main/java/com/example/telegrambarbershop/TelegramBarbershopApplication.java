package com.example.telegrambarbershop;

import com.example.telegrambarbershop.controller.TelegramBotController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
@EnableScheduling
public class TelegramBarbershopApplication {
    public static void main(String[] args) {
        //TelegramBotsApi botsApi = new TelegramBotsApi();

        /*try {
            // Регистрация вашего бота
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new TelegramBotController());
            System.out.println("Бот успешно зарегистрирован и запущен!");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }*/

        SpringApplication.run(TelegramBarbershopApplication.class, args);
    }
}
