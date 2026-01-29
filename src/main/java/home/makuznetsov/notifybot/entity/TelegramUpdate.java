package home.makuznetsov.notifybot.entity;

public record TelegramUpdate(
        Long updateId,
        Message message,
        CallbackQuery callback_query
) {
    public record Message(
            Long messageId,
            UserFrom from,
            String text
    ) {}

    public record CallbackQuery(
            String id,
            UserFrom from,
            Message message,
            String data
    ) {}

    public record UserFrom(
            Long id,
            String username
    ) {}
}
