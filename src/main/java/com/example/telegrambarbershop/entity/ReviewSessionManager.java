package com.example.telegrambarbershop.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReviewSessionManager {
    private List<UserReviewSession> sessions = new ArrayList<>();

    public void addSession(UserReviewSession session) {
        sessions.add(session);
    }

    public Optional<UserReviewSession> findSessionByChatId(Long chatId) {
        return sessions.stream()
                .filter(session -> session.getChatId().equals(chatId))
                .findFirst();
    }

    public void removeSessionByChatId(Long chatId) {
        sessions.removeIf(session -> session.getChatId().equals(chatId));
    }
}
