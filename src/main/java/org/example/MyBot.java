package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;

/**
 * Telegram бот для управления задачами.
 */
public class MyBot extends TelegramLongPollingBot {
    private final MessageHandler logic;
    private final String botUsername;
    private boolean isRunning;

    public MyBot(String botUsername, String botToken, MessageHandler logic) {
        super(botToken);
        this.botUsername = botUsername;
        this.logic = logic;
        this.isRunning = false;
    }

    public MyBot() {
        this(System.getenv("TG_BOT_USERNAME"), System.getenv("TG_TOKEN"), new MessageHandler());
    }

    public MyBot start() {
        this.isRunning = true;
        System.out.println("Telegram бот запущен");
        return this;
    }

    public void stop() {
        this.isRunning = false;
        System.out.println("Telegram бот остановлен");
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!isRunning || !update.hasMessage()) return;

        String userId = update.getMessage().getFrom().getId().toString();
        String chatId = update.getMessage().getChatId().toString();

        if (update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            if (text.startsWith("/export")) {
                ExportCommand(chatId, userId, text);
            } else {
                String answer = logic.processUserInput(text, userId);
                sendMessage(chatId, answer);
            }
        }

        if (update.getMessage().hasDocument()) {
            ImportCommand(chatId, userId, update);
        }
    }

    private void ExportCommand(String chatId, String userId, String text) {
        try {
            String filename = text.substring("/export".length()).trim();
            if (filename.isEmpty()) {
                sendMessage(chatId, "Напиши имя файла после /export");
                return;
            }
            File exportFile = logic.Export(userId, filename);

            SendDocument document = new SendDocument();
            document.setChatId(chatId);
            document.setDocument(new InputFile(exportFile, exportFile.getName()));
            document.setCaption("Ваш список задач в виде документа " + exportFile.getName());

            execute(document);
            logic.clean(exportFile);

        } catch (Exception e) {
            sendMessage(chatId, "Ошибка при экспорте");
        }
    }

    private void ImportCommand(String chatId, String userId, Update update) {
        try {
            String fileId = update.getMessage().getDocument().getFileId();
            GetFile getFile = new GetFile(fileId);
            org.telegram.telegrambots.meta.api.objects.File file = execute(getFile);

            File tempFile = File.createTempFile("import", ".json");
            String fileUrl = "https://api.telegram.org/file/bot" + getBotToken() + "/" + file.getFilePath();

            try (java.io.InputStream in = new java.net.URL(fileUrl).openStream()) {
                java.nio.file.Files.copy(in, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            String result = logic.Import(tempFile, userId);
            sendMessage(chatId, result);
            logic.clean(tempFile);

        } catch (Exception e) {
            sendMessage(chatId, "Ошибка при импорте файла");
        }
    }

    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage(chatId, text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }
}
