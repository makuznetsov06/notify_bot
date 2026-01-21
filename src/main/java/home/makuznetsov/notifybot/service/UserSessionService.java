package home.makuznetsov.notifybot.service;

import home.makuznetsov.notifybot.entity.SessionType;
import home.makuznetsov.notifybot.entity.UserSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class UserSessionService {

    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();

    private String createKey(Long chatId, Long userId) {
        return chatId + ":" + userId;
    }

    public UserSession createSession(Long chatId, Long userId, SessionType currentAction) {
        String key = createKey(chatId, userId);
        UserSession session = new UserSession(chatId, userId, currentAction);
        sessions.put(key, session);
        log.debug("Created session: {} -> {}", key, currentAction);
        return session;
    }

    public Optional<UserSession> getSession(Long chatId, Long userId) {
        String key = createKey(chatId, userId);
        UserSession session = sessions.get(key);

        if (session != null && session.isExpired()) {
            sessions.remove(key);
            log.debug("Removed expired session: {}", key);
            return Optional.empty();
        }

        if (session != null) {
            session.updateActivity();
        }

        return Optional.ofNullable(session);
    }

    public String getAllUsersSessions() {
        return sessions.toString();
    }

    public void updateAction(Long chatId, Long userId, SessionType newAction) {
        getSession(chatId, userId).ifPresent(session -> {
            session.setCurrentAction(newAction);
            session.updateActivity();
            log.debug("Updated action for {}: {}", createKey(chatId, userId), newAction);
        });
    }

    public void cleanLastEnteredData(Long chatId, Long userId) {
        getSession(chatId, userId).ifPresent(session -> {
            session.setLastEnteredTitle(null);
            session.setLastEnteredDateTime(null);
            session.setLastEnteredRecurrencePattern(null);
            log.debug("Cleaned last entered data for {}", createKey(chatId, userId));
        });
    }

    /**
     * Удаляет сессию
     */
    public void removeSession(Long chatId, Long userId) {
        String key = createKey(chatId, userId);
        sessions.remove(key);
        log.debug("Removed session: {}", key);
    }


    /**
     * Получает текущее действие пользователя
     */
    public Optional<SessionType> getCurrentAction(Long chatId, Long userId) {
        return getSession(chatId, userId).map(UserSession::getCurrentAction);
    }
}