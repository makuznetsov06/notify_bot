package home.makuznetsov.notifybot.entity;

import lombok.Getter;

@Getter
public enum ReminderStatus {
    SCHEDULED("Запланировано"),
    SENT("Отправлено"),
    CANCELLED("Отменено");

    private String buttonText;

    ReminderStatus(String buttonText) { this.buttonText = buttonText; }

    @Override
    public String toString() {
        return "ReminderStatus{" +
                "buttonText='" + buttonText + '\'' +
                '}';
    }
}
