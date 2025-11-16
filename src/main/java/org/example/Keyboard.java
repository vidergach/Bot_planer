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
     *
     * @return объект с кнопками команд
     */
    public static ReplyKeyboardMarkup authorizationKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row0 = new KeyboardRow();
        row0.add("\uD83D\uDCDD Регистрация");
        row0.add("Войти в аккаунт");

        KeyboardRow row1 = new KeyboardRow();
        row1.add("\u2795 Добавить задачу");
        row1.add("\uD83D\uDCDD Показать список задач");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("\u2714 Выполнено");
        row2.add("\u2705 Список выполненных задач");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("\u2718 Удалить");
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
}