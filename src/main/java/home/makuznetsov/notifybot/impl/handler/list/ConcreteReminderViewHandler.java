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
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConcreteReminderViewHandler implements ChatInbound {

    private final UserSessionService sessionService;
    private final TelegramBotService botService;
    private final ReminderService reminderService;
    private final LocalizationService localizationService;
    private final KeyboardService keyboardService;

    @Override
    public boolean canHandle(IncomingMessageDto message, Optional<User> user) {
        if (user.isPresent()) {
            log.info("Getting user wit Id: {}", user.get().getId().toString());
            Optional<SessionType> currentAction = sessionService.getCurrentAction(message.getTelegramId(),
                    user.get().getId());
            log.info("Current action: {}", currentAction.orElse(null));
            return isCurrentActionViewConcreteReminder(currentAction) &&
                    isActionButtonEditReminder(message);
        }
        return false;
    }

    @Override
    public void handle(IncomingMessageDto message, Optional<User> user) {
        try{
            sessionService.updateAction(message.getTelegramId(),
                    user.orElseThrow(Exception::new).getId(),
                    SessionType.REMINDER_EDIT);
            String callbackText = message.getCallbackData();
            Long reminderId = Long.parseLong(callbackText.substring(callbackText.indexOf(":")+1));
            reminderService.findById(reminderId).ifPresent(reminder -> {
                sendConcreteReminderInfo(reminder, user);
            });
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private void sendConcreteReminderInfo(Reminder reminder, Optional<User> user) {
        String messageText = String.format("""
            📌 Детали напоминания
            
            Заголовок: %s
            Время: %s
            Статус: %s
            Повторение: %s
            """,
                reminder.getTitle(),
                reminder.getScheduledTime()
                        .format(DateTimeFormatter.ofPattern(localizationService.getDateTimeFormat())),
                reminder.getStatus().getButtonText(),
                reminder.getRecurrencePattern()
        );
        botService.sendMessageWithInlineKeyboard(user.get().getTelegramUserId(),
                messageText,
                keyboardService.getConcreteReminderKeyboard(reminder));
    }

    private boolean isCurrentActionViewConcreteReminder(Optional<SessionType> currentAction) {
        return currentAction
                .map(SessionType::name)
                .map(name -> name.equals(SessionType.REMINDER_VIEW.name()))
                .orElse(false);
    }

    private boolean isActionButtonEditReminder(IncomingMessageDto message) {
        return message.getCallbackData().startsWith(ActionButton.EDIT_REMINDER.name());
    }
}
