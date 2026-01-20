package home.makuznetsov.notifybot.chat.outbound;

public interface ChatOutbound {
    void sendMessage(String message, Long telegramId);
}
