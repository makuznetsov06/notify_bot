package home.makuznetsov.notifybot.entity;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum RecurrencePattern {
    NONE("Без повторений"),
    DAILY("Раз в день"),
    WEEKLY("Раз в неделю"),
    MONTHLY("Раз в месяц"),
    YEARLY("Раз в год");

    private String buttonText;

    RecurrencePattern(String buttonText) {
        this.buttonText = buttonText;
    }

    @Override
    public String toString() {
        return "RecurrencePattern{" +
                "buttonText='" + buttonText + '\'' +
                '}';
    }

    public static boolean contains(String value) {
        return Arrays.stream(RecurrencePattern.values())
                .anyMatch(e -> e.name().equalsIgnoreCase(value));
    }
}
