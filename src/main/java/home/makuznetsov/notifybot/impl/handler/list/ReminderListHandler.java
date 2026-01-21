package home.makuznetsov.notifybot.impl.handler.list;

import home.makuznetsov.notifybot.chat.IncomingMessageDto;
import home.makuznetsov.notifybot.chat.inbound.ChatInbound;
import home.makuznetsov.notifybot.entity.*;
import home.makuznetsov.notifybot.service.ReminderService;
import home.makuznetsov.notifybot.service.TelegramBotService;
import home.makuznetsov.notifybot.service.UserSessionService;
import home.makuznetsov.notifybot.service.botUtil.KeyboardService;
import home.makuznetsov.notifybot.service.botUtil.LocalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderListHandler implements ChatInbound {

    private final UserSessionService sessionService;
    private final TelegramBotService botService;
    private final ReminderService reminderService;
    private final LocalizationService localizationService;
    private final KeyboardService keyboardService;

    @Override
    public boolean canHandle(IncomingMessageDto message, Optional<User> user) {
        if (user.isPresent()) {
            log.info("Getting user wit Id: {}",user.get().getId().toString());
            Optional<SessionType> currentAction = sessionService.getCurrentAction(message.getTelegramId(),
                    user.get().getId());
            log.info("Current action: {}", currentAction.orElse(null));
            return currentAction.isPresent() &&
                    currentAction.get().equals(SessionType.MAIN_MENU) &&
                    message.getCallbackData().equals(ActionButton.MY_REMINDERS_LIST.name());
        }
        return false;
    }

    @Override
    public void handle(IncomingMessageDto message, Optional<User> user) {
        try{
            User currentUser = user.orElseThrow(Exception::new);
            sessionService.updateAction(message.getTelegramId(),
                    currentUser.getId(),
                    SessionType.REMINDER_VIEW);

            List<Reminder> userReminderList = reminderService.getUserReminders(currentUser.getId());
            if (userReminderList.isEmpty()) {
                sendNoRemindersMessage(message.getTelegramId(), currentUser.getId());
                return;
            }

            Integer page = 0;
            String preparedMessage = formatRemindersList(userReminderList, page);

            botService.sendMessageWithInlineKeyboard(message.getTelegramId(),
                    preparedMessage,
                    keyboardService.getReminderListKeyboard(userReminderList));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private void sendNoRemindersMessage(Long telegramId, Long userId) {
        botService.sendMarkdownMessage(telegramId, "Нет напоминаний");
        sessionService.updateAction(telegramId,
                userId,
                SessionType.MAIN_MENU);
        botService.sendMessageWithInlineKeyboard(telegramId,
                localizationService.getMainMenuText(),
                keyboardService.getMainMenuKeyboard());
    }

    private String formatRemindersList(List<Reminder> reminders, int currentPage) {
        int totalReminders = reminders.size();
        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) totalReminders / pageSize);

        StringBuilder sb = new StringBuilder();

        sb.append("📋 Ваши напоминания\n\n");

        // Статистика
        sb.append(String.format("Всего: %d напоминаний\n", totalReminders));
//        sb.append(String.format("Страница: %d из %d\n\n", currentPage + 1, totalPages));

        // Список напоминаний
        for (int i = 0; i < totalReminders && i < pageSize; i++) {
            Reminder reminder = reminders.get(i);
            String formatted = formatReminderShort(reminder, i + 1 + (currentPage * 10));
            sb.append(formatted).append("\n");
        }

        sb.append("\n👇 Выберите напоминание для просмотра деталей или управления");

        return sb.toString();
    }

    /**
     * Короткий формат напоминания для списка
     */
    private String formatReminderShort(Reminder reminder, int number) {
        String timeFormatted = reminder.getScheduledTime()
                .format(DateTimeFormatter.ofPattern(localizationService.getDateTimeFormat()));

        String statusIcon = getStatusIcon(reminder.getStatus());
        String recurrenceIcon = (reminder.getRecurrencePattern() != null) &&
                (!reminder.getRecurrencePattern().equalsIgnoreCase(RecurrencePattern.NONE.name()))
                ? "🔁" : "⏺️";

        return String.format("%d) %s %s %s %s\n",
                number,
                statusIcon,
                reminder.getTitle(),
                timeFormatted,
                recurrenceIcon
        );
    }

    private String getStatusIcon(ReminderStatus status) {
        return switch (status) {
            case SCHEDULED -> "⏳";
            case SENT -> "✅";
            case CANCELLED -> "❌";
        };
    }
}
