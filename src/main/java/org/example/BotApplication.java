package org.example;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Главный класс приложения для запуска Telegram и Discord ботов.
 * Инициализирует и запускает обоих ботов параллельно.
 */
public class BotApplication {
    /**
     * Основной метод приложения, запускающий Telegram и Discord ботов.
     * Инициализирует ботов с использованием параметров из переменных окружения
     *
     */
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

            String telegramBotName = System.getenv("BOT_USERNAME");
            String telegramBotToken = System.getenv("BOT_TOKEN");
            String discordToken = System.getenv("DISCORD_TOKEN");

            MessageHandler messageHandler = new MessageHandler();

            TelegramBot bot = new TelegramBot(telegramBotName, telegramBotToken, messageHandler);
            botsApi.registerBot(bot);
            System.out.println("Telegram бот запущен");

            DiscordBot discordBot = new DiscordBot(discordToken, messageHandler);
            System.out.println("Discord бот запущен");

        } catch (TelegramApiException e) {
            e.printStackTrace();
            System.err.println("Ошибка при запуске Telegram бота: " + e.getMessage());
        }
    }
}
