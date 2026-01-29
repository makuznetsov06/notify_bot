package home.makuznetsov.notifybot.impl.handler.remove;

import home.makuznetsov.notifybot.chat.IncomingMessageDto;
import home.makuznetsov.notifybot.chat.inbound.ChatInbound;
import home.makuznetsov.notifybot.entity.ActionButton;
import home.makuznetsov.notifybot.entity.SessionType;
import home.makuznetsov.notifybot.entity.User;
import home.makuznetsov.notifybot.service.ReminderService;
import home.makuznetsov.notifybot.service.TelegramBotService;
import home.makuznetsov.notifybot.service.UserSessionService;
import home.makuznetsov.notifybot.service.botUtil.KeyboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderRemoveHandler implements ChatInbound {

    private final UserSessionService sessionService;
    private final TelegramBotService botService;
    private final ReminderService reminderService;
    private final KeyboardService keyboardService;

    @Override
    public boolean canHandle(IncomingMessageDto message, Optional<User> user) {
        if (user.isPresent()) {
            log.info("Getting user wit Id: {}", user.get().getId().toString());
            Optional<SessionType> currentAction = sessionService.getCurrentAction(message.getTelegramId(),
                    user.get().getId());
            log.info("Current action: {}", currentAction.orElse(null));
            return isCurrentActionRemoveReminder(currentAction) &&
                    isActionButtonContainsReminderId(message);
        }
        return false;
    }

    @Override
    @Transactional
    public void handle(IncomingMessageDto message, Optional<User> user) {
        try {
            Long reminderId = getReminderIdFromCallback(message.getCallbackData()).get();
            reminderService.cancelReminder(reminderId);
            sessionService.updateAction(message.getTelegramId(), user.get().getId(), SessionType.MAIN_MENU);
            botService.sendMessageWithInlineKeyboard(message.getTelegramId(),
                    "Напоминание отменено",
                    keyboardService.getMainMenuKeyboard());
        } catch (Exception e) {
            log.error("Error in ReminderRemoveHandler", e);
        }
    }

    private boolean isCurrentActionRemoveReminder(Optional<SessionType> currentAction) {
        return currentAction
                .map(SessionType::name)
                .map(name -> name.equals(SessionType.REMINDER_EDIT.name()))
                .orElse(false);
    }

    private boolean isActionButtonContainsReminderId(IncomingMessageDto message) {
        String callbackText = message.getCallbackData();
        return callbackText.startsWith(ActionButton.DELETE_REMINDER.name())
                && getReminderIdFromCallback(callbackText).isPresent();
    }

    private Optional<Long> getReminderIdFromCallback(String callbackText) {
        if (callbackText == null || callbackText.isBlank()) {
            return Optional.empty();
        }

        int colonIndex = callbackText.indexOf(":");
        if (colonIndex == -1 || colonIndex == callbackText.length() - 1) {
            return Optional.empty();
        }

        String idPart = callbackText.substring(colonIndex + 1).trim();
        if (idPart.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Long.parseLong(idPart));
        } catch (NumberFormatException e) {
            log.debug("Failed to parse reminder ID from callback: {}", callbackText, e);
            return Optional.empty();
        }
    }
}
