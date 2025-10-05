package org.example;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * Главный класс приложения для запуска Telegram бота.
 * Инициализирует и регистрирует бота в Telegram API.
 *
 * @author Vika
 * @version 1.0
 * @since 2024
 */
public class BotApplication {
    /**
     * Точка входа в приложение Telegram бота.
     * Создает экземпляр бота, регистрирует его и запускает сессию для обработки сообщений.
     *
     * @param args аргументы командной строки
     * @throws TelegramApiException если произошла ошибка при регистрации бота
     *
     */
    public static void main(String[] args) throws TelegramApiException {
        MyBot bot = new MyBot();
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(bot);
        System.out.println("Бот работает");
    }
}