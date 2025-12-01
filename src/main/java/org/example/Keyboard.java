package org.example;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс для создания и настройки клавиатуры Telegram бота.
 */
public class Keyboard {

    /**
     * Создает и возвращает клавиатуру авторизации с основными командами бота.
     */
    public ReplyKeyboardMarkup authorizationKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row0 = new KeyboardRow();
        row0.add("\uD83D\uDCDD Регистрация");
        row0.add("Войти в аккаунт");

        KeyboardRow row1 = new KeyboardRow();
        row1.add("\uD83D\uDCDD Показать список задач");
        row1.add("\u2705 Список выполненных задач");
        row1.add("Расширить задачу");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("\u2795 Добавить задачу");
        row2.add("\u2714 Выполнено");
        row2.add("\u2718 Удалить");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("Экспорт");
        row3.add("Импорт");

        KeyboardRow row4 = new KeyboardRow();
        row4.add("Выйти из аккаунта");

        KeyboardRow row5 = new KeyboardRow();
        row5.add("Помощь");

        rows.add(row0);
        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(row4);
        rows.add(row5);

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    /**
     * Создает клавиатуру для работы с подзадачами
     */
    public ReplyKeyboardMarkup subtaskKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row0 = new KeyboardRow();
        row0.add("\u2795 Добавить подзадачу");
        row0.add("\u2718 Удалить подзадачу");
        row0.add("GPT добавление подзадач");

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Изменить подзадачу");
        row1.add("Сохранить");
        row1.add("Удалить");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Окончить расширение");

        rows.add(row0);
        rows.add(row1);
        rows.add(row2);

        keyboard.setKeyboard(rows);
        return keyboard;
    }
}