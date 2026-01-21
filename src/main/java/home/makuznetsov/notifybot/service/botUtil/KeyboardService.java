package home.makuznetsov.notifybot.service.botUtil;

import home.makuznetsov.notifybot.entity.ActionButton;
import home.makuznetsov.notifybot.entity.RecurrencePattern;
import home.makuznetsov.notifybot.entity.Reminder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KeyboardService {

    public InlineKeyboardMarkup getMainMenuKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        createButton(ActionButton.ADD_NEW_REMINDER.getButtonText(), ActionButton.ADD_NEW_REMINDER.name()),
                        createButton(ActionButton.MY_REMINDERS_LIST.getButtonText(), ActionButton.MY_REMINDERS_LIST.name())
                ))
//                .keyboardRow(List.of(
//                        createButton(ActionButton.BOT_HELP.getButtonText(), ActionButton.BOT_HELP),
//                        createButton(ActionButton.BOT_SETTINGS.getButtonText(), ActionButton.BOT_SETTINGS)
//                ))
                .build();
    }

    public InlineKeyboardMarkup getRecurrencePatternKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        createButton(RecurrencePattern.NONE.getButtonText(), RecurrencePattern.NONE.name()),
                        createButton(RecurrencePattern.DAILY.getButtonText(), RecurrencePattern.DAILY.name())
                ))
                .keyboardRow(List.of(
                        createButton(RecurrencePattern.WEEKLY.getButtonText(), RecurrencePattern.WEEKLY.name()),
                        createButton(RecurrencePattern.MONTHLY.getButtonText(), RecurrencePattern.MONTHLY.name())
                ))
                .keyboardRow(List.of(
                        createButton(RecurrencePattern.YEARLY.getButtonText(), RecurrencePattern.YEARLY.name())
                ))
                .build();
    }

    public InlineKeyboardMarkup getReminderListKeyboard(List<Reminder> reminders) {
       List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

       for (int reminderNumber = 1; reminderNumber <= reminders.size(); reminderNumber++) {
           String buttonText = Integer.toString(reminderNumber);
           keyboard.add(List.of(
                   InlineKeyboardButton.builder()
                           .text(buttonText)
                           .callbackData(ActionButton.EDIT_REMINDER.name() + ":" + reminders.get(reminderNumber-1).getId())
                           .build())
           );
       }
       keyboard.add(List.of(createButton(ActionButton.MAIN_MENU.getButtonText(), ActionButton.MAIN_MENU.name())));
       return new InlineKeyboardMarkup(keyboard);
    }

    public InlineKeyboardMarkup getConcreteReminderKeyboard(Reminder reminder) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                                InlineKeyboardButton.builder()
                                        .text(ActionButton.DELETE_REMINDER.getButtonText())
                                        .callbackData(ActionButton.DELETE_REMINDER.name() + ":" + reminder.getId())
                                        .build(),
                        createButton(ActionButton.MAIN_MENU.getButtonText(), ActionButton.MAIN_MENU.name())
                ))
                .build();
    }

    private InlineKeyboardButton createButton(String text, String action) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(action)
                .build();
    }

}
