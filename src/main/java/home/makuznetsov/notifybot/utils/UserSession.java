package home.makuznetsov.notifybot.utils;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserSession {
    private UserState state;
    private String title;
    private LocalDateTime scheduledTime;
}
