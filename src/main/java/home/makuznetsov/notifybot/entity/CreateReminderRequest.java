package home.makuznetsov.notifybot.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
public class CreateReminderRequest {
    @NotBlank
    private String title;

    @NotNull
    private ZonedDateTime scheduledTime;

    private String recurrencePattern;
    private String metadata;
}
