package home.makuznetsov.notifybot.controller;

import home.makuznetsov.notifybot.chat.IncomingMessage;
import home.makuznetsov.notifybot.chat.inbound.ChatInbound;
import home.makuznetsov.notifybot.config.TelegramBotConfig;
import home.makuznetsov.notifybot.service.TelegramBotService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramBotController extends TelegramWebhookBot {

    private final TelegramBotConfig botConfig;
    private final TelegramBotService botService;
    private final ChatInbound chatInbound;

    @Override
    public String getBotUsername() {
        return botConfig.getUsername();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        log.debug("Received update: {}", update);
        IncomingMessage message = IncomingMessage.builder()
                .telegramId(update.getMessage().getChatId())
                .message(update.getMessage().getText())
                .build();
        try {
            chatInbound.handleMessage(message);
        } catch (Exception e) {
            log.error("Error processing update: {}", update, e);
            return createErrorMessage(update.getMessage().getChatId(), "Произошла ошибка. Попробуйте позже.");
        }

        return null;
    }

    @Override
    public String getBotPath() {
        return botConfig.getPath();
    }

    @PostConstruct
    public void init() {
        try {
            this.execute(new org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands(
                    getCommandsList(),
                    new BotCommandScopeDefault(),
                    null
            ));
            log.info("Telegram bot initialized successfully");
        } catch (TelegramApiException e) {
            log.error("Error setting bot commands", e);
        }
    }

    private List<BotCommand> getCommandsList() {
        List<BotCommand> commands = new ArrayList<>();
        commands.add(new BotCommand("/start", "Запустить бота"));
        commands.add(new BotCommand("/help", "Помощь"));
        commands.add(new BotCommand("/new", "Создать новое напоминание"));
        commands.add(new BotCommand("/list", "Мои напоминания"));
        commands.add(new BotCommand("/cancel", "Отменить текущее действие"));
        return commands;
    }

    private BotApiMethod<?> handleMessage(org.telegram.telegrambots.meta.api.objects.Message message) {
        Long chatId = message.getChatId();
        String text = message.getText();

        log.info("Message from {}: {}", chatId, text);

        if (text.startsWith("/")) {
            return handleCommand(chatId, text, message.getFrom());
        } else {
            return handleTextMessage(chatId, text, message.getFrom());
        }
    }

    private BotApiMethod<?> handleCommand(Long chatId, String command, org.telegram.telegrambots.meta.api.objects.User user) {
        switch (command.toLowerCase()) {
            case "/start":
                return handleStartCommand(chatId, user);
            case "/help":
                return handleHelpCommand(chatId);
            case "/new":
                return handleNewReminderCommand(chatId);
            case "/list":
                return handleListCommand(chatId, user);
            case "/cancel":
                return handleCancelCommand(chatId);
            default:
                return createMessage(chatId, "Неизвестная команда. Используйте /help для списка команд.");
        }
    }

    private BotApiMethod<?> handleStartCommand(Long chatId, org.telegram.telegrambots.meta.api.objects.User user) {
        // Регистрируем пользователя
        botService.registerUser(
                user.getId(),
                user.getUserName(),
                user.getFirstName(),
                user.getLastName(),
                user.getLanguageCode()
        );

        // Создаем приветственное сообщение
        SendMessage message = createMessage(chatId, getWelcomeMessage());
        message.setReplyMarkup(getMainMenuKeyboard());

        return message;
    }

    private String getWelcomeMessage() {
        return """
            🎉 *Добро пожаловать в ReminderBot!* 🎉
            
            Я помогу вам не забывать о важных событиях!
            
            *Что я умею:*
            📌 Создавать напоминания на любое время
            ⏰ Отправлять уведомления точно в срок
            🔁 Настраивать повторяющиеся напоминания
            📋 Показывать список ваших напоминаний
            
            *Основные команды:*
            /new - Создать новое напоминание
            /list - Показать мои напоминания
            /help - Помощь и инструкции
            
            Нажмите на кнопку ниже или используйте команду /new чтобы начать!
            """;
    }

    private BotApiMethod<?> handleHelpCommand(Long chatId) {
        String helpText = """
            *📖 Помощь по использованию бота*
            
            *Команды:*
            /start - Запустить бота
            /help - Эта справка
            /new - Создать новое напоминание
            /list - Показать все ваши напоминания
            /cancel - Отменить текущее действие
            
            *Как создать напоминание:*
            1. Нажмите /new или кнопку "Добавить напоминание"
            2. Введите название события
            3. Укажите дату и время (например: 2024-01-20 14:30)
            4. Выберите нужно ли повторение
            
            *Формат времени:*
            • 14:30 - сегодня в 14:30
            • 2024-01-20 14:30 - конкретная дата
            • через 2 часа - относительное время
            • завтра 10:00 - завтра в 10 утра
            
            *Повторения:*
            • Нет - одноразовое напоминание
            • Ежедневно - каждый день в это время
            • Еженедельно - каждый неделю в этот день
            • Ежемесячно - каждый месяц в это число
            
            *Вопросы и поддержка:*
            Если возникли проблемы, напишите @ваш_username
            """;

        return createMessage(chatId, helpText);
    }

    private BotApiMethod<?> handleNewReminderCommand(Long chatId) {
        return botService.startNewReminderProcess(chatId);
    }

    private BotApiMethod<?> handleListCommand(Long chatId, org.telegram.telegrambots.meta.api.objects.User user) {
        return botService.getUserReminders(chatId, user.getId());
    }

    private BotApiMethod<?> handleCancelCommand(Long chatId) {
        botService.cancelCurrentOperation(chatId);
        return createMessage(chatId, "Текущая операция отменена. Что вы хотите сделать?");
    }

    private BotApiMethod<?> handleTextMessage(Long chatId, String text, org.telegram.telegrambots.meta.api.objects.User user) {
        return botService.processUserInput(chatId, user.getId(), text);
    }

    private BotApiMethod<?> handleCallbackQuery(org.telegram.telegrambots.meta.api.objects.CallbackQuery callbackQuery) {
        // Обработка нажатий на inline-кнопки
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Long userId = callbackQuery.getFrom().getId();

        log.info("Callback from {}: {}", userId, data);

        return botService.processCallback(chatId, userId, data, callbackQuery.getMessage().getMessageId());
    }

    private SendMessage createMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.enableMarkdown(true);
        message.setParseMode("Markdown");
        return message;
    }

    private SendMessage createErrorMessage(Long chatId, String text) {
        SendMessage message = createMessage(chatId, "❌ " + text);
        message.setReplyMarkup(getMainMenuKeyboard());
        return message;
    }

    private ReplyKeyboardMarkup getMainMenuKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        // Первый ряд
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("📝 Добавить напоминание"));
        row1.add(new KeyboardButton("📋 Мои напоминания"));

        // Второй ряд
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("🆘 Помощь"));
        row2.add(new KeyboardButton("⚙️ Настройки"));

        keyboard.add(row1);
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }
}