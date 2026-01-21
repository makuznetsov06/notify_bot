package home.makuznetsov.notifybot.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderMessage {
    private Long reminderId;
    private Long userId;
    private Long chatId;
    private String title;
    private ZonedDateTime scheduledTime;
    private String metadata;

    public static ReminderMessage fromReminder(Reminder reminder) {
        return ReminderMessage.builder()
                .reminderId(reminder.getId())
                .userId(reminder.getUser().getId())
                .chatId(reminder.getUser().getTelegramUserId())
                .title(reminder.getTitle())
                .scheduledTime(reminder.getScheduledTime())
                .metadata(reminder.getMetadata())
                .build();
    }
}