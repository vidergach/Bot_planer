package org.example;

import java.io.File;

/**
 * Класс, представляющий ответ бота на запрос.
 */
public class BotResponse {
    private final String message;
    private final File file;
    private final String fileName;

    /**
     * Конструктор для создания текстового ответа.
     *
     * @param message текстовое сообщение
     */
    public BotResponse(String message) {
        this.message = message;
        this.file = null;
        this.fileName = null;
    }

    /**
     * Конструктор для создания ответа с файлом.
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

    /**
     * Возвращает текстовое сообщение ответа.
     *
     * @return текстовое сообщение
     */
    public String getMessage() {
        return message;
    }

    /**
     * Возвращает файл ответа.
     *
     * @return файл для отправки
     */
    public File getFile() {
        return file;
    }

    /**
     * Возвращает имя файла.
     *
     * @return имя файла
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Проверяет, содержит ли ответ файл.
     *
     * @return true если ответ содержит файл, false в противном случае
     */
    public boolean hasFile() {
        return file != null;
    }
}

    /**
     * Возвращает имя файла.
     *
     * @return имя файла
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Проверяет, содержит ли ответ файл.
     *
     * @return true если ответ содержит файл, false в противном случае
     */
    public boolean hasFile() {
        return file != null;
    }
}