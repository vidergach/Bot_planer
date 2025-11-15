package org.example;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.InputStream;

/**
 * Telegram бот для управления задачами.
 */
public class TelegramBot extends TelegramLongPollingBot {
    private final MessageHandler logic;
    private final String botUsername;

    /**
     * Создаем новый экземпляр Telegram бота.
     *
     * @param botUsername имя бота в Telegram
     * @param botToken токен для доступа к API Telegram Bot
     * @param logic обработчик логики команд и сообщений
     */
    public TelegramBot(String botUsername, String botToken, MessageHandler logic) {
        super(botToken);
        this.botUsername = botUsername;
        this.logic = logic;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) return;

        String userId = update.getMessage().getFrom().getId().toString();
        String chatId = update.getMessage().getChatId().toString();

        try {
            if (update.getMessage().hasDocument()) {
                handleImport(chatId, userId, update);
                return;
            }

            if (update.getMessage().hasText()) {
                String text = update.getMessage().getText();
                BotResponse response = logic.processUserInput(text, userId);

                if (response.hasFile()) {
                    SendDocument document = new SendDocument();
                    document.setChatId(chatId);
                    document.setDocument(new InputFile(response.getFile(), response.getFileName()));
                    document.setCaption(response.getMessage());
                    execute(document);
                } else {
                    execute(new SendMessage(chatId, response.getMessage()));
                }
            }

        } catch (Exception e) {
            try {
                e.printStackTrace();
                execute(new SendMessage(chatId, "Ошибка: " + e.getMessage()));
            } catch (TelegramApiException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Обрабатывает команду импорта задач из файлового вложения.
     *
     * @param chatId идентификатор чата для отправки ответа
     * @param userId идентификатор пользователя для задач
     * @param update объект обновления с информацией о файле
     */
    private void handleImport(String chatId, String userId, Update update) {
        try {
            String fileId = update.getMessage().getDocument().getFileId();
            GetFile getFile = new GetFile(fileId);
            org.telegram.telegrambots.meta.api.objects.File file = execute(getFile);

            java.io.File downloadedFile = downloadFile(file);
            try (InputStream inputStream = new java.io.FileInputStream(downloadedFile)) {
                BotResponse response = logic.processImport(inputStream, userId);
                execute(new SendMessage(chatId, response.getMessage()));
            }

        } catch (Exception e) {
            try {
                e.printStackTrace();
                execute(new SendMessage(chatId, "Ошибка импорта: " + e.getMessage()));
            } catch (TelegramApiException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }
}
