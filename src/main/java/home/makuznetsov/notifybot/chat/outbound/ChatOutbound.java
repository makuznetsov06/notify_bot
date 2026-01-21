package home.makuznetsov.notifybot.chat.outbound;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public interface ChatOutbound {
    void sendMarkdownMessage(Long chatId, String markdownText);
    void sendMessageWithInlineKeyboard(Long chatId, String text, InlineKeyboardMarkup keyboard);
}
