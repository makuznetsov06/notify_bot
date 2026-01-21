package home.makuznetsov.notifybot.service;

import home.makuznetsov.notifybot.impl.TelegramMessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotService {

    private final TelegramMessageSender telegramMessageSender;
    private final String COMMON_ERROR_TEXT = "Произошла ошибка. Попробуйте повторить текущее действие";

    public void sendMarkdownMessage(Long chatId, String markdownText) {
        telegramMessageSender.sendMarkdownMessage(chatId, markdownText);
    }

    public void sendErrorMarkdownMessage(Long chatId) {
        telegramMessageSender.sendMarkdownMessage(chatId, COMMON_ERROR_TEXT);
    }


    public void sendMessageWithInlineKeyboard(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        telegramMessageSender.sendMessageWithInlineKeyboard(chatId, text, keyboard);
    }

}
