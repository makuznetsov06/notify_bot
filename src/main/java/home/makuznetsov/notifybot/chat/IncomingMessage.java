package home.makuznetsov.notifybot.chat;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IncomingMessage {
    Long telegramId;
    String message;
}
