package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.io.File;

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
     *
     * @param botUsername имя бота (может быть null)
     * @param botToken токен бота для авторизации в Telegram API
     */
    public MyBot(String botUsername, String botToken) {
        super(botToken);
        this.botUsername = botUsername;
    }

    /**
     * Конструктор бота с параметрами из переменных окружения.
     * Использует переменные окружения BOT_USERNAME и BOT_TOKEN.
     */
    public MyBot() {
        this(System.getenv("BOT_USERNAME"), System.getenv("BOT_TOKEN"));
    }


    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) return;

        String userId = update.getMessage().getFrom().getId().toString();
        String chatId = update.getMessage().getChatId().toString();

        if (update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            if (text.startsWith("/export")) {
                ExportCommand(chatId, userId, text);
            } else {
                String answer = mainProcessor.processUserInput(text, userId);
                sendMessage(chatId, answer);
            }
        }
        if (update.getMessage().hasDocument()) {
            ImportCommand(chatId, userId, update);
        }
    }

    /**
     * Обрабатывает команду экспорта задач в файл.
     * Создает файл с задачами пользователя и отправляет его в чат.
     *
     * @param chatId идентификатор чата для отправки сообщения
     * @param userId идентификатор пользователя для получения его задач
     * @param text текст команды, содержащий имя файла
     */
    private void ExportCommand(String chatId, String userId, String text) {
        try {
            String filename = text.substring("/export".length()).trim();
            if (filename.isEmpty()) {
                sendMessage(chatId, "Напиши имя файла после /export");
                return;
            }
            filename=filename.replace("\"", "")
                    .replace("'", "")
                    .replace("”", "")
                    .trim();
            if (!filename.endsWith(".json")){
                filename += ".json";
            }

            File exportFile = mainProcessor.Export(userId, filename);

            SendDocument document = new SendDocument();
            document.setChatId(chatId);
            document.setDocument(new InputFile(exportFile, filename));
            document.setCaption("Ваш список задач в виде документа " + filename + "\"");

            execute(document);
            mainProcessor.clean(exportFile);

        } catch (Exception e) {
            sendMessage(chatId, "Ошибка при экспорте");
        }
    }

    /**
     * Обрабатывает команду импорта задач из файла.
     * Скачивает файл из Telegram, импортирует задачи и удаляет временный файл.
     *
     * @param chatId идентификатор чата для отправки результата
     * @param userId идентификатор пользователя для привязки импортированных задач
     * @param update объект Update с информацией о сообщении и документе
     */
    private void ImportCommand(String chatId, String userId, Update update) {
        try {
            // Скачиваем файл из Telegram
            String fileId = update.getMessage().getDocument().getFileId();
            GetFile getFile = new GetFile(fileId);
            org.telegram.telegrambots.meta.api.objects.File file = execute(getFile);

            // Создаем временный файл
            File tempFile = File.createTempFile("import", ".json");

            // Скачиваем по URL
            String fileUrl = "https://api.telegram.org/file/bot" + getBotToken() + "/" + file.getFilePath();
            try (java.io.InputStream in = new java.net.URL(fileUrl).openStream()) {
                java.nio.file.Files.copy(in, tempFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            // Импортируем задачи
            String result = mainProcessor.Import(tempFile, userId);
            sendMessage(chatId, result);

            // Удаляем временный файл
            mainProcessor.clean(tempFile);

        } catch (Exception e) {
            sendMessage(chatId, "Ошибка при импорте файла");
        }
    }

    /**
     * Отправляет текстовое сообщение в указанный чат.
     *
     * @param chatId идентификатор чата для отправки сообщения
     * @param text текст сообщения для отправки
     */
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