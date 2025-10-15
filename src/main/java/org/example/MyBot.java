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
 * Обрабатывает команды пользователей для управления списками задач,
 * поддерживает экспорт и импорт задач через файлы.
 */
public class MyBot extends TelegramLongPollingBot {
    private final MessageHandler logic;
    private final String botUsername;
    private boolean isRunning;

    /**
     * Создает новый экземпляр Telegram бота с указанными параметрами.
     *
     * @param botUsername имя бота в Telegram
     * @param botToken токен бота для аутентификации
     * @param logic обработчик сообщений для бизнес-логики приложения
     */
    public MyBot(String botUsername, String botToken, MessageHandler logic) {
        super(botToken);
        this.botUsername = botUsername;
        this.logic = logic;
        this.isRunning = false;
    }

    /**
     * Создает новый экземпляр Telegram бота с параметрами из переменных окружения.
     * Использует переменные окружения TG_BOT_USERNAME и TG_TOKEN.
     */
    public MyBot() {
        this(System.getenv("TG_BOT_USERNAME"), System.getenv("TG_TOKEN"), new MessageHandler());
    }

    /**
     * Запускает бота и устанавливает флаг работы.
     * Выводит сообщение об успешном запуске в консоль.
     *
     */
    public MyBot start() {
        this.isRunning = true;
        System.out.println("Telegram бот запущен");
        return this;
    }

    /**
     * Останавливает бота и сбрасывает флаг работы.
     * Выводит сообщение об остановке в консоль.
     */
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

    /**
     * Обрабатывает команду экспорта задач в файл.
     * Создает JSON файл с задачами пользователя и отправляет его в чат.
     * Автоматически добавляет расширение .json если оно отсутствует.
     *
     * @param chatId идентификатор чата для отправки ответа
     * @param userId идентификатор пользователя
     * @param text полный текст команды с именем файла
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

    /**
     * Обрабатывает команду импорта задач из файла.
     * Загружает присланный JSON файл и импортирует задачи пользователя.
     *
     * @param chatId идентификатор чата для отправки результата
     * @param userId идентификатор пользователя
     * @param update объект обновления с информацией о документе
     */
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

    /**
     * Отправляет текстовое сообщение в указанный чат.
     * Обрабатывает исключения Telegram API.
     *
     * @param chatId идентификатор чата для отправки
     * @param text текст сообщения
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
