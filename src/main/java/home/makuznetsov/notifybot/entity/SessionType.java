package home.makuznetsov.notifybot.entity;

public enum SessionType {
    REGISTRATION,
    MAIN_MENU,          // Регистрация пользователя
    REMINDER_CREATION,
    REMINDER_VIEW,// Создание напоминания
    REMINDER_EDIT,      // Редактирование напоминания
    SETTINGS,           // Настройки профиля
    SUPPORT,            // Обращение в поддержку
    FEEDBACK,           // Обратная связь
    CUSTOM
}
