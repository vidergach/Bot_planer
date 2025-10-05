package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Telegram бот для управления задачами.
 * Обрабатывает входящие сообщения и управляет задачами через MessageHandler.
 * Использует Long Polling для получения обновлений от Telegram API.
 *
 * @see TelegramLongPollingBot
 * @see MessageHandler
 */
public class MyBot extends TelegramLongPollingBot {
    private final MessageHandler mainProcessor = new MessageHandler();

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

    /**
     * Отправляет текстовое сообщение в указанный чат.
     * Обрабатывает исключения при отправке.
     *
     * @param chatId идентификатор чата для отправки
     * @param text текст сообщения для отправки
     */
    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка при отправке сообщения: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return "test_my_super_demo_bot";
    }

    @Override
    public String getBotToken() {
        return System.getenv("BOT_TOKEN");
    }
}