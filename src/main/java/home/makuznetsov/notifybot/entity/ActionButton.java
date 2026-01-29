package home.makuznetsov.notifybot.entity;

import lombok.Getter;

@Getter
public enum ActionButton {
    ADD_NEW_REMINDER("📝 Добавить"),
    MY_REMINDERS_LIST("📋 Созданные"),
    EDIT_REMINDER(""),
    DELETE_REMINDER("❌ Удалить"),
    MAIN_MENU("К меню"),
    BOT_HELP("🆘 Помощь"),
    BOT_SETTINGS("⚙️ Настройки");

    private String buttonText;

    ActionButton(String buttonText) {
        this.buttonText = buttonText;
    }

    @Override
    public String toString() {
        return "ActionButton{" +
                "buttonText='" + buttonText + '\'' +
                '}';
    }
}
