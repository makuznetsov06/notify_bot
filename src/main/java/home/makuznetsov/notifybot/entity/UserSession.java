package home.makuznetsov.notifybot.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserSession {

    private Long chatId;
    private Long userId;
    private SessionType currentAction;
    private String lastEnteredTitle;
    private String lastEnteredDateTime;
    private String lastEnteredRecurrencePattern;
    private LocalDateTime createdAt = LocalDateTime.now();

    public UserSession(Long chatId, Long userId, SessionType currentAction) {
        this.chatId = chatId;
        this.userId = userId;
        this.currentAction = currentAction;
    }

    public void updateActivity() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(createdAt.plusDays(30));
    }
}