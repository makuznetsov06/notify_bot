package home.makuznetsov.notifybot.service;

import home.makuznetsov.notifybot.entity.User;
import home.makuznetsov.notifybot.repository.UserRepository;
import home.makuznetsov.notifybot.utils.UserSession;
import home.makuznetsov.notifybot.utils.UserState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotService {

    private final UserRepository userRepository;

    // Временное хранилище состояний пользователей
    private final Map<Long, UserSession> userSessions = new HashMap<>();

    @Transactional
    public void registerUser(Long telegramUserId, String username, String firstName,
                             String lastName, String languageCode) {
        userRepository.findByTelegramUserId(telegramUserId)
                .ifPresentOrElse(
                        user -> {
//                            // Обновляем данные существующего пользователя
//                            if (!user.getUsername().equals(username) ||
//                                    !user.getFirstName().equals(firstName)) {
//                                user.setUsername(username);
//                                user.setFirstName(firstName);
//                                user.setLastName(lastName);
//                                user.setLanguageCode(languageCode);
//                                userRepository.save(user);
//                                log.info("User updated: {}", telegramUserId);
//                        }
                        },
                        () -> {
                            // Создаем нового пользователя
                            User newUser = User.builder()
                                    .telegramUserId(telegramUserId)
                                    .username(username)
                                    .firstName(firstName)
                                    .lastName(lastName)
                                    .languageCode(languageCode)
                                    .isActive(true)
                                    .build();
                            userRepository.save(newUser);
                            log.info("New user registered: {}", telegramUserId);
                        }
                );
    }

    public BotApiMethod<?> startNewReminderProcess(Long chatId) {
        UserSession session = new UserSession();
        session.setState(UserState.WAITING_FOR_TITLE);
        userSessions.put(chatId, session);

        return createMessage(chatId,
                "📝 *Создание нового напоминания*\n\n" +
                        "Пожалуйста, введите *название* для вашего напоминания:\n" +
                        "(Например: 'Встреча с командой', 'Оплатить счета')");
    }

    public BotApiMethod<?> processUserInput(Long chatId, Long userId, String text) {
        UserSession session = userSessions.get(chatId);

        if (session == null) {
            return createMessage(chatId, "Пожалуйста, начните с команды /new или используйте меню.");
        }

        switch (session.getState()) {
            case WAITING_FOR_TITLE:
                return processTitleInput(chatId, userId, text, session);
            case WAITING_FOR_TIME:
                return processTimeInput(chatId, userId, text, session);
            case WAITING_FOR_RECURRENCE:
                return processRecurrenceInput(chatId, userId, text, session);
            default:
                userSessions.remove(chatId);
                return createMessage(chatId, "Сессия сброшена. Начните заново с /new");
        }
    }

    private BotApiMethod<?> processTitleInput(Long chatId, Long userId, String title, UserSession session) {
        if (title.length() > 500) {
            return createMessage(chatId, "❌ Слишком длинное название. Максимум 500 символов.\nПожалуйста, введите более короткое название:");
        }

        session.setTitle(title);
        session.setState(UserState.WAITING_FOR_TIME);

        return createMessage(chatId,
                "✅ Название сохранено: *" + title + "*\n\n" +
                        "Теперь укажите *дату и время* напоминания:\n" +
                        "Вы можете использовать различные форматы:\n" +
                        "• *2024-01-20 14:30* (конкретная дата)\n" +
                        "• *14:30* (сегодня в это время)\n" +
                        "• *завтра 10:00*\n" +
                        "• *через 2 часа*\n" +
                        "• *понедельник 9:00*\n\n" +
                        "*Важно:* Используйте ваш часовой пояс: UTC+3");
    }

    private BotApiMethod<?> processTimeInput(Long chatId, Long userId, String timeText, UserSession session) {
        try {
            LocalDateTime scheduledTime = parseTimeInput(timeText);

            if (scheduledTime.isBefore(LocalDateTime.now())) {
                return createMessage(chatId,
                        "❌ Нельзя установить напоминание в прошлом!\n" +
                                "Пожалуйста, укажите будущее время:");
            }

            session.setScheduledTime(scheduledTime);
            session.setState(UserState.WAITING_FOR_RECURRENCE);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

            SendMessage message = createMessage(chatId,
                    "✅ Время установлено: *" + scheduledTime.format(formatter) + "*\n\n" +
                            "Нужно ли повторять это напоминание?");

            // Добавляем inline-кнопки для выбора повторения
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            List<InlineKeyboardButton> row1 = new ArrayList<>();
            row1.add(createInlineButton("Нет", "recurrence_none"));
            row1.add(createInlineButton("Ежедневно", "recurrence_daily"));

            List<InlineKeyboardButton> row2 = new ArrayList<>();
            row2.add(createInlineButton("Еженедельно", "recurrence_weekly"));
            row2.add(createInlineButton("Ежемесячно", "recurrence_monthly"));

            List<InlineKeyboardButton> row3 = new ArrayList<>();
            row3.add(createInlineButton("Отменить", "recurrence_cancel"));

            rows.add(row1);
            rows.add(row2);
            rows.add(row3);

            markup.setKeyboard(rows);
            message.setReplyMarkup(markup);

            return message;

        } catch (DateTimeParseException e) {
            return createMessage(chatId,
                    "❌ Не могу распознать время!\n" +
                            "Пожалуйста, укажите время в одном из форматов:\n" +
                            "• *2024-01-20 14:30*\n" +
                            "• *14:30*\n" +
                            "• *завтра 10:00*\n" +
                            "• *через 2 часа*");
        }
    }

    private BotApiMethod<?> processRecurrenceInput(Long chatId, Long userId, String input, UserSession session) {
        // Обработка выбора из inline-кнопок
        String recurrencePattern = null;

        switch (input) {
            case "recurrence_none":
                recurrencePattern = null;
                break;
            case "recurrence_daily":
                recurrencePattern = "DAILY";
                break;
            case "recurrence_weekly":
                recurrencePattern = "WEEKLY";
                break;
            case "recurrence_monthly":
                recurrencePattern = "MONTHLY";
                break;
            case "recurrence_cancel":
                userSessions.remove(chatId);
                return createMessage(chatId, "Создание напоминания отменено.");
            default:
                return createMessage(chatId, "Пожалуйста, выберите вариант из кнопок.");
        }

        // Сохраняем напоминание в БД
        saveReminder(userId, session.getTitle(), session.getScheduledTime(), recurrencePattern);

        // Очищаем сессию
        userSessions.remove(chatId);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        String timeStr = session.getScheduledTime().format(formatter);

        String response = "🎉 *Напоминание успешно создано!*\n\n" +
                "📌 *Название:* " + session.getTitle() + "\n" +
                "⏰ *Время:* " + timeStr + "\n" +
                "🔁 *Повторение:* " + (recurrencePattern != null ?
                getRecurrenceText(recurrencePattern) : "Нет") + "\n\n" +
                "Я отправлю вам уведомление в указанное время!";

        return createMessage(chatId, response);
    }

    public BotApiMethod<?> processCallback(Long chatId, Long userId, String callbackData, Integer messageId) {
        return processRecurrenceInput(chatId, userId, callbackData, userSessions.get(chatId));
    }

    public BotApiMethod<?> getUserReminders(Long chatId, Long userId) {
        Optional<User> userOpt = userRepository.findByTelegramUserId(userId);

        if (userOpt.isEmpty()) {
            return createMessage(chatId, "Вы еще не зарегистрированы. Используйте /start");
        }

        User user = userOpt.get();

        if (user.getReminders().isEmpty()) {
            return createMessage(chatId,
                    "📭 У вас пока нет напоминаний.\n" +
                            "Создайте первое напоминание командой /new");
        }

        StringBuilder response = new StringBuilder();
        response.append("📋 *Ваши напоминания:*\n\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        user.getReminders().stream()
                .filter(r -> r.getStatus() == home.makuznetsov.notifybot.entity.Reminder.ReminderStatus.SCHEDULED)
                .sorted(Comparator.comparing(home.makuznetsov.notifybot.entity.Reminder::getScheduledTime))
                .forEach(reminder -> {
                    response.append("• *").append(reminder.getTitle()).append("*\n");
                    response.append("  ⏰ ").append(reminder.getScheduledTime().format(formatter)).append("\n");
                    if (reminder.getRecurrencePattern() != null) {
                        response.append("  🔁 ").append(getRecurrenceText(reminder.getRecurrencePattern())).append("\n");
                    }
                    response.append("  🆔 ID: `").append(reminder.getId()).append("`\n\n");
                });

        response.append("\nВсего: ").append(user.getReminders().size()).append(" напоминаний");

        SendMessage message = createMessage(chatId, response.toString());

        // Добавляем кнопки для управления
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createInlineButton("🗑 Удалить напоминание", "delete_reminder"));
        row1.add(createInlineButton("✏️ Редактировать", "edit_reminder"));

        rows.add(row1);
        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        return message;
    }

    public void cancelCurrentOperation(Long chatId) {
        userSessions.remove(chatId);
    }

    private void saveReminder(Long userId, String title, LocalDateTime scheduledTime, String recurrencePattern) {
        // TODO: Реализовать сохранение в БД через ReminderRepository
        log.info("Saving reminder for user {}: {} at {} with recurrence {}",
                userId, title, scheduledTime, recurrencePattern);
    }

    private LocalDateTime parseTimeInput(String timeText) {
        // TODO: Реализовать парсинг различных форматов времени
        // Пока простой парсинг
        return LocalDateTime.parse(timeText.replace(" ", "T"));
    }

    private String getRecurrenceText(String pattern) {
        return switch (pattern) {
            case "DAILY" -> "Ежедневно";
            case "WEEKLY" -> "Еженедельно";
            case "MONTHLY" -> "Ежемесячно";
            default -> "Кастомное";
        };
    }

    private InlineKeyboardButton createInlineButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }

    private SendMessage createMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.enableMarkdown(true);
        message.setParseMode("Markdown");
        return message;
    }
}