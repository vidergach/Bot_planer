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
 * Telegram бот для управления задачами с кнопками
 */
public class TelegramBot extends TelegramLongPollingBot {
    private final MessageHandler logic;
    private final String botUsername;
    private final Keyboard keyboard;

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
        this.keyboard = new Keyboard();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) return;

        String userId = update.getMessage().getFrom().getId().toString();
        String chatId = update.getMessage().getChatId().toString();

        try {
            if (update.getMessage().hasDocument()) {
                handleImportCommand(chatId, userId, update);
                return;
            }

            if (update.getMessage().hasText()) {
                String text = update.getMessage().getText();

                String command = convertButton(text);

                String PLATFORM_TYPE = "telegram";
                BotResponse response = logic.processUserInput(command, userId, PLATFORM_TYPE);
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(response.getMessage());

                if (logic.shouldShowGptKeyboard(userId)) {
                    // Если нужно показать клавиатуру GPT
                    Object gptKeyboard = logic.getGptKeyboard();
                    if (gptKeyboard instanceof org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup) {
                        message.setReplyMarkup((org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup) gptKeyboard);
                    }
                }
                else if (logic.isUserInSubtaskMode(userId)) {
                    message.setReplyMarkup(keyboard.subtaskKeyboard());
                } else {
                    message.setReplyMarkup(keyboard.authorizationKeyboard());
                }

                if (response.hasFile()) {
                    SendDocument document = new SendDocument();
                    document.setChatId(chatId);
                    document.setDocument(new InputFile(response.getFile(), response.getFileName()));
                    document.setCaption(response.getMessage());
                    execute(document);
                } else {
                    execute(message);
                }
            }


        } catch (Exception e) {
            try {
                SendMessage error = new SendMessage();
                error.setChatId(chatId);
                error.setText("Ошибка: " + e.getMessage());
                error.setReplyMarkup(keyboard.authorizationKeyboard());
                execute(error);
            } catch (TelegramApiException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Конвертирует текст кнопок в команды бота
     */
    private String convertButton(String button) {
        return switch (button) {
            case "\u2795 Добавить задачу" -> "/add";
            case "\uD83D\uDCDD Показать список задач" -> "/tasks";
            case "\u2705 Список выполненных задач" -> "/dTask";
            case "\u2718 Удалить" -> "/delete";
            case "\u2714 Выполнено" -> "/done";
            case "Экспорт" -> "/export";
            case "Импорт" -> "/import";
            case "Помощь" -> "/help";
            case "\uD83D\uDCDD Регистрация" -> "/registration";
            case "Войти в аккаунт" -> "/login";
            case "Выйти из аккаунта" -> "/exit";
            case "Расширить задачу" -> "/expand";

            case "\u2795 Добавить подзадачу" -> "/add_subtask";
            case "\u2718 Удалить подзадачу" -> "/delete_subtask";
            case "Изменить подзадачу" -> "/edit_subtask";
            case "Окончить расширение" -> "/finish_expand";
            case "GPT добавление подзадач" -> "/add_subtasks_with_gpt";
            case "Сохранить" -> "/save_subtasks_from_gpt";
            case "Удалить" -> "/delete_subtasks_from_gpt";
            default -> button;
        };
    }

    /**
     * Обрабатывает команду импорта задач из файлового вложения.
     *
     * @param chatId идентификатор чата для отправки ответа
     * @param userId идентификатор пользователя для задач
     * @param update объект обновления с информацией о файле
     */
    private void handleImportCommand(String chatId, String userId, Update update) {
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