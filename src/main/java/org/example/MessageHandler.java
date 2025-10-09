package org.example;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Основной обработчик сообщений для Telegram бота планировщика задач.
 * Отвечает за парсинг пользовательских команд, маршрутизацию и форматирование ответов.
 * Обеспечивает изоляцию данных между пользователями и потокобезопасность.
 *
 * @author Vika
 * @version 2.0
 * @see UserData
 * @see ConcurrentHashMap
 */
public class MessageHandler {
    private final Map<String, UserData> userDataMap = new ConcurrentHashMap<>();

    /**
     * Вспомогательный класс для хранения разобранных частей пользовательской команды.
     * Содержит команду и параметр, извлеченные из пользовательского ввода.
     */
    private class CommandParts {
        private final String command;
        private final String parameter;

        /**
         * Создает новый экземпляр CommandParts с указанными командой и параметром.
         *
         * @param command
         * @param parameter
         */
        public CommandParts(String command, String parameter) {
            this.command = command;
            this.parameter = parameter;
        }

        /**
         * Возвращает команду пользователя.
         *
         * @return команда в виде строки
         */
        public String getCommand() {
            return command;
        }

        /**
         * Возвращает параметр команды.
         *
         * @return параметр в виде строки, может быть пустой строкой если параметр не указан
         */
        public String getParameter() {
            return parameter;
        }
    }

    /**
     * Парсит пользовательский ввод на команду и параметр.
     * Разделяет входную строку по первому пробелу, если он присутствует.
     *
     * @param userInput пользовательский ввод для парсинга
     * @return объект CommandParts с разобранной командой и параметром
     */
    private CommandParts parseCommand(String userInput) {
        if (userInput.isBlank()) {
            return new CommandParts("", "");
        }

        String trimmedInput = userInput.trim();
        String[] parts = trimmedInput.split("\\s+", 2);

        String command = parts[0];
        String parameter = parts.length > 1 ? parts[1].trim() : "";

        return new CommandParts(command, parameter);
    }

    /**
     * Основной метод обработки пользовательского ввода.
     * Автоматически создает хранилище данных для новых пользователей.
     * Логирует входящие сообщения и исходящие ответы.
     *
     * @param userInput текст сообщения от пользователя
     * @param userId    уникальный идентификатор пользователя для изоляции данных
     * @return текстовый ответ бота
     * @see UserData
     */
    public String processUserInput(String userInput, String userId) {
        System.out.println("сообщение: " + userInput + " от: " + userId);
        UserData userData = userDataMap.computeIfAbsent(userId, k -> new UserData());

        CommandParts parts = parseCommand(userInput);
        String command = parts.getCommand();
        String parameter = parts.getParameter();

        String outputText = processCommand(command, parameter, userData);
        System.out.println("Ответ: " + outputText);
        return outputText;
    }

    /**
     * Определяет и выполняет соответствующую команду пользователя.
     * Маршрутизирует команды к соответствующим методам обработки.
     *
     * @param command  команда для выполнения
     * @param parameter параметр команды
     * @param userData данные пользователя для операции
     * @return результат выполнения команды в виде форматированного текста
     */
    private String processCommand(String command, String parameter, UserData userData) {
        if ("/start".equals(command)) {
            return Start_Message;
        } else if ("/help".equals(command)) {
            return Help_Message;
        } else if ("/add".equals(command)) {
            return addTask(parameter, userData);
        } else if ("/tasks".equals(command)) {
            return showTasks(userData);
        } else if ("/done".equals(command)) {
            return markTaskDone(parameter, userData);
        } else if ("/dTask".equals(command)) {
            return showCompletedTasks(userData);
        } else if ("/delete".equals(command)) {
            return deleteTask(parameter, userData);
        } else {
            return "Неизвестная команда.\nВведите /help для просмотра доступных команд.";
        }
    }

    /**
     * Приветственное сообщение, отправляемое пользователю при старте бота.
     * Содержит описание функционала и список доступных команд.
     */
    private final String Start_Message = """ 
                Добро пожаловать в планировщик задач! \uD83D\uDC31 📝
                Я могу организовывать ваши задачи.
                Команды:
                /add - добавить задачу
                /tasks - показать список задач
                /done - отметить выполненной
                /dTask - список выполненных задач
                /delete - удалить задачу
                /help - помощь""";

    /**
     * Справочное сообщение с подробным описанием работы бота.
     * Содержит примеры использования всех команд с ожидаемыми ответами.
     */
    private final String Help_Message = """ 
                Справка по работе:
                Я планировщик задач😊 📝
                Мои команды:
                /add - добавить задачу
                /tasks - показать список задач
                /done - отметить выполненной
                /dTask - список выполненных задач
                /delete - удалить задачу
                /help - помощь

                Например:
                /add Полить цветы
                - Задача "Полить цветы" добавлена!

                /add Накормить кота
                - Задача "Накормить кота" добавлена!

                /add Полить цветы
                - Задача "Полить цветы" уже есть в списке!

                /tasks
                - Вот список ваших задач:
                  1. Полить цветы
                  2. Накормить кота

                /done Полить цветы
                - Задача "Полить цветы" отмечена выполненной!

                /dTask
                - ✅ Вот список выполненных задач:
                  1. Полить цветы ✔

                /delete Накормить кота
                - 🗑️ Задача "Накормить кота" удалена из списка задач!
            """;

    /**
     * Добавляет новую задачу в список пользователя.
     * Проверяет наличие параметра и обрабатывает исключения от UserData.
     *
     * @param parameter текст задачи для добавления
     * @param userData  данные пользователя
     * @return сообщение о результате операции добавления задачи
     * @see UserData#addTask(String)
     */
    private String addTask(String parameter, UserData userData) {
        if (parameter.isEmpty()) {
            return """
                    Упс\uD83D\uDE05, похоже вы забыли указать задачу после команды /add
                    Например: /add Полить цветы""";
        }
        try {
            userData.addTask(parameter);
            return "Задача \"" + parameter + "\" добавлена!";
        } catch (IllegalArgumentException | IllegalStateException e) {
            return e.getMessage();
        }
    }

    /**
     * Показывает список текущих задач пользователя.
     * Форматирует задачи в нумерованный список.
     *
     * @param userData данные пользователя
     * @return форматированный список задач или сообщение о пустом списке
     * @see UserData#getTasks()
     * @see UserData#hasTasks()
     */
    private String showTasks(UserData userData) {
        if (!userData.hasTasks()) {
            return "Список задач пуст!";
        }
        List<String> tasks = userData.getTasks();
        StringBuilder sb = new StringBuilder("Вот список ваших задач:\n");
        for (int i = 0; i < tasks.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(tasks.get(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * Отмечает задачу как выполненную.
     * Перемещает задачу из списка текущих в список выполненных задач.
     *
     * @param parameter текст задачи для отметки как выполненной
     * @param userData  данные пользователя
     * @return сообщение о результате операции
     * @see UserData#markTaskDone(String)
     */
    private String markTaskDone(String parameter, UserData userData) {
        if (parameter.isEmpty()) {
            return """
                    Упс\uD83D\uDE05, похоже вы забыли указать задачу после команды /done
                    Например: /done Полить цветы""";
        }
        try {
            userData.markTaskDone(parameter);
            return "Задача \"" + parameter + "\" отмечена выполненной!";
        } catch (IllegalArgumentException | IllegalStateException e) {
            return e.getMessage();
        }
    }

    /**
     * Показывает список выполненных задач пользователя.
     * Форматирует задачи с использованием эмодзи для визуального выделения.
     *
     * @param userData данные пользователя
     * @return форматированный список выполненных задач или сообщение о пустом списке
     * @see UserData#getCompletedTasks()
     * @see UserData#hasCompletedTasks()
     */
    private String showCompletedTasks(UserData userData) {
        if (!userData.hasCompletedTasks()) {
            return "Список выполненных задач пуст!";
        }
        var completedTasks = userData.getCompletedTasks();
        StringBuilder sb = new StringBuilder("✅ Вот список выполненных задач:\n");
        for (int i = 0; i < completedTasks.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(completedTasks.get(i)).append(" ✔\n");
        }
        return sb.toString();
    }

    /**
     * Удаляет задачу из списка пользователя.
     * Полностью удаляет задачу без перемещения в список выполненных.
     *
     * @param parameter текст задачи для удаления
     * @param userData  данные пользователя
     * @return сообщение о результате операции удаления
     * @see UserData#deleteTask(String)
     */
    private String deleteTask(String parameter, UserData userData) {
        if (parameter.isEmpty()) {
            return """
                    Упс\uD83D\uDE05, похоже вы забыли указать задачу после команды /delete.
                    Например: /delete Полить цветы""";
        }
        try {
            userData.deleteTask(parameter);
            return "🗑️ Задача \"" + parameter + "\" удалена из списка задач!";
        } catch (IllegalArgumentException | IllegalStateException e) {
            return e.getMessage();
        }
    }
}