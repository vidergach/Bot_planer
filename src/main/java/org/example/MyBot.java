package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Telegram бот для управления задачами.
 * Обрабатывает входящие сообщения и управляет задачами через MessageHandler.
 * Использует Long Polling для получения обновлений от Telegram API.
 */
public class MyBot extends TelegramLongPollingBot {
    private final MessageHandler mainProcessor = new MessageHandler();
    private final String botUsername;

    /**
     * Конструктор бота с параметрами.
     * @param botUsername имя бота (может быть null)
     * @param botToken токен бота
     */
    public MyBot(String botUsername, String botToken) {
        super(botToken);
        this.botUsername = "test_my_super_demo_bot";
    }

    /**
     * Конструктор бота с параметрами из переменных окружения.
     */
    public MyBot() {
        this(System.getenv("BOT_USERNAME"), System.getenv("BOT_TOKEN"));
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String userInput = update.getMessage().getText();
            String userId = update.getMessage().getFrom().getId().toString();
            String chatId = update.getMessage().getChatId().toString();

            String response = mainProcessor.processUserInput(userInput, userId);
            sendMessage(chatId, response);
        }
    }

    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage(chatId, text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка при отправке сообщения: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }
}