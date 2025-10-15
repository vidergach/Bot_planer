package org.example;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Главный класс приложения для запуска Telegram и Discord ботов.
 */
public class BotApplication {
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

            String telegramBotName = System.getenv("BOT_USERNAME");
            String telegramBotToken = System.getenv("BOT_TOKEN");
            MessageHandler logic = new MessageHandler();

            MyBot bot = new MyBot(telegramBotName, telegramBotToken, logic);
            botsApi.registerBot(bot);
            bot.start();

            System.out.println("Telegram бот запущен");

            String discordToken = System.getenv("DISCORD_TOKEN"); // или передайте токен явно
            MessageHandler messageHandler = new MessageHandler();
            DiscordBot discordBot = new DiscordBot(discordToken, messageHandler);
            discordBot.start();
            System.out.println("Discord бот запущен");

        } catch (TelegramApiException e) {
            e.printStackTrace();
            System.err.println("Ошибка при запуске Telegram бота: " + e.getMessage());
        }
    }
}
