package home.makuznetsov.notifybot.chat;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IncomingMessageDto {
    Long telegramId;
    String username;
    String message;
    String callbackData;
}
