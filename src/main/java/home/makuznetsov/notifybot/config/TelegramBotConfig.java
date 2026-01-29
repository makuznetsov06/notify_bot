package home.makuznetsov.notifybot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "telegram.bot")
public class TelegramBotConfig {
    private String username;
    private String token;
    private String path;

    @Bean
    public TelegramBotsApi telegramBotsApi() throws TelegramApiException {
        return new TelegramBotsApi(DefaultBotSession.class);
    }

    @Bean
    public DefaultBotOptions botOptions() {
        DefaultBotOptions options = new DefaultBotOptions();
        options.setMaxThreads(10);
        return options;
    }
}
