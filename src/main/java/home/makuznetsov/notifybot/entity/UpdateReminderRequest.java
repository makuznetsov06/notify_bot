package home.makuznetsov.notifybot.entity;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class UpdateReminderRequest {
    private String title;
    private ZonedDateTime scheduledTime;
    private String recurrencePattern;
    private String metadata;
}
