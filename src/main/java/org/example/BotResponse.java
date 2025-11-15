package org.example;

import java.io.File;

/**
 * Структура для возврата ответа бота
 * Содержит текстовое сообщение для отправки пользователю.
 */
public class BotResponse {
    private final String message;
    private final File file;
    private final String fileName;

    /**
     * Создает текстовый ответ
     *
     * @param message текстовое сообщение
     */
    public BotResponse(String message) {
        this.message = message;
        this.file = null;
        this.fileName = null;
    }

    /**
     * Создает ответ с файлом
     *
     * @param message текстовое сообщение
     * @param file файл
     * @param fileName имя файла
     */
    public BotResponse(String message, File file, String fileName) {
        this.message = message;
        this.file = file;
        this.fileName = fileName;
    }

    public String getMessage() { return message; }
    public File getFile() { return file; }
    public String getFileName() { return fileName; }
    public boolean hasFile() { return file != null; }
}

