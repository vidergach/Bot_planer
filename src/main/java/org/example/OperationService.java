package org.example;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для управления операциями с задачами
 */
public class OperationService {
    private final DatabaseService databaseService;
    private final Map<String, Operation> operationStates = new ConcurrentHashMap<>();
    private final FileWork fileWork = new FileWork();
    private final AuthService authService;

    public final String HELP_MESSAGE = """
            Справка по работе:
            Я планировщик задач😊 📝
            Используйте кнопки:
            \uD83D\uDCDD Регистрация
            Войти в аккаунт
            \u2795 Добавить задачу
            \uD83D\uDCDD Показать список задач
            \u2714 Выполнено
            \u2705 Список выполненных задач
            \u2718 Удалить
            Экспорт - предоставить список задач пользователя в файле
            Импорт - загрузить список задач из файла
            Выйти из аккаунта
            Помощь

            Например:
            \u2795 Добавить задачу
            - Полить цветы
            - Задача "Полить цветы" добавлена!

            \u2795 Добавить задачу
            - Накормить кота
            - Задача "Накормить кота" добавлена!

            \u2795 Добавить задачу
            - Полить цветы
            - Задача "Полить цветы" уже есть в списке!

            \uD83D\uDCDD Показать список задач
            - Вот список ваших задач:
              1. Полить цветы
              2. Накормить кота

            \u2714 Выполнено
            - Полить цветы
            - Задача "Полить цветы" отмечена выполненной!

            \u2705 Список выполненных задач
            - ✅ Вот список выполненных задач:
              1. Полить цветы ✔

            \u2718 Удалить
            - Накормить кота
            - 🗑️ Задача "Накормить кота" удалена из списка задач!

            Экспорт
            - Напишите имя файла для экспорта
            - 'tasks_list.json'
            - Ваш список задач в виде документа (отправка "tasks_list.json")

            Импорт
            - Отправьте JSON файл со списком задач
            - (отправка "tasks_list.json")
            - Задачи успешно добавлены, можете проверить списки с помощью команд /tasks и /dTask
            """;

    /**
     * Внутренний класс для отслеживания состояния операции.
     */
    private class Operation {
        String type;
        Operation(String type) {
            this.type = type;
        }
    }

    /**
     * Конструктор сервиса операций.
     *
     * @param databaseService сервис для работы с базой данных
     * @param authService сервис для управления аутентификацией
     */
    public OperationService(DatabaseService databaseService, AuthService authService) {
        this.databaseService = databaseService;
        this.authService = authService;
    }

    /**
     * Проверяет есть ли у пользователя незавершенная операция.
     *
     * @param userId идентификатор пользователя
     * @return true если есть незавершенная операция, false в противном случае
     */
    public boolean hasOperationState(String userId) {
        return operationStates.containsKey(userId);
    }

    /**
     * Обрабатывает следующий шаг операции с дополнительным вводом.
     *
     * @param userId идентификатор пользователя
     * @param userInput ввод пользователя
     * @return ответ бота с результатом выполнения операции
     */
    public BotResponse handleOperationStep(String userId, String userInput) throws SQLException, IOException, UserAuthorized, UnknownOperation, TaskNotFoundException, TaskAlreadyExistsException {
        Operation state = operationStates.get(userId);
        operationStates.remove(userId);
        return executeOperation(state.type, userInput.trim(), userId);
    }

    /**
     * Обрабатывает команду пользователя.
     *
     * @param command команда
     * @param parameter параметр
     * @param userId идентификатор пользователя
     * @param platformType тип платформы
     * @return ответ бота с результатом выполнения команды
     */
    BotResponse processCommand(String command, String parameter, String userId, String platformType) {
        try {
            String internalUserId = databaseService.getUserIdByPlatform(userId);
            if (internalUserId == null && !command.equals("/start") && !command.equals("/help") &&
                    !command.equals("/registration") && !command.equals("/login")) {
                throw new UserAuthorized("Ошибка: пользователь не авторизован. Пожалуйста, войдите с помощью /login.");
            }

            return switch (command) {
                case "/start" -> new BotResponse(authService.getStartMessage());
                case "/help" -> new BotResponse(HELP_MESSAGE);
                case "/add" -> handleOperation("add", parameter, userId,
                        """
                                Введите задачу для добавления:
                                Например: Купить молоко""");
                case "/tasks" -> handleShowTasks(internalUserId);
                case "/done" -> handleOperation("done", parameter, userId,
                        """
                                Введите название задачи для отметки выполнения:
                                Например: Купить молоко""");
                case "/dTask" -> handleShowCompletedTasks(internalUserId);
                case "/delete" -> handleOperation("delete", parameter, userId,
                        """
                                Введите название задачи для удаления:
                                Например: Купить молоко""");
                case "/registration" -> authService.handleRegistration(userId, platformType);
                case "/login" -> authService.handleLogin(userId, platformType);
                case "/exit" -> authService.handleExit(userId, platformType);
                case "/export" -> handleOperation("export", parameter, userId,
                        """
                                Напишите имя файла для экспорта
                                Например: 'list'""");
                case "/import" -> new BotResponse("Для импорта отправьте JSON файл с задачами");
                default -> new BotResponse("""
                                        Неизвестная команда.
                                        Введите /help для просмотра доступных команд.
                                        """);
            };
        } catch (Exception e) {
            e.printStackTrace();
            return new BotResponse("Ошибка при выполнении команды: " + e.getMessage());
        }
    }

    /**
     * Обрабатывает операцию с дополнительным вводом.
     *
     * @param operation тип операции
     * @param parameter параметр
     * @param userId идентификатор пользователя
     * @param prompt сообщение
     * @return ответ бота
     */
    private BotResponse handleOperation(String operation, String parameter, String userId, String prompt) throws SQLException, IOException, UserAuthorized, UnknownOperation, TaskNotFoundException, TaskAlreadyExistsException {
        if (parameter == null || parameter.isEmpty()) {
            operationStates.put(userId, new Operation(operation));
            return new BotResponse(prompt);
        } else {
            return executeOperation(operation, parameter, userId);
        }
    }

    /**
     * Выполняет операцию.
     *
     * @param operation тип операции
     * @param input ввод
     * @param userId идентификатор пользователя
     * @return ответ бота
     */
    private BotResponse executeOperation(String operation, String input, String userId) throws SQLException, IOException, UserAuthorized, UnknownOperation, TaskNotFoundException, TaskAlreadyExistsException {
        String internalUserId = databaseService.getUserIdByPlatform(userId);
        if (internalUserId == null) {
            throw new UserAuthorized("Пользователь не авторизован");
        }

        return switch (operation) {
            case "add" -> {
                if (input == null || input.trim().isEmpty()) {
                    operationStates.put(userId, new Operation("add"));
                    yield new BotResponse("""
                        Введите задачу для добавления:
                        Например: Купить молоко""");
                }
                List<String> currentTasks = databaseService.getCurrentTasks(internalUserId);
                if (currentTasks.contains(input.trim())) {
                    throw new TaskAlreadyExistsException("Задача \"" + input + "\" уже существует!");
                }
                databaseService.addTask(internalUserId, input);
                yield new BotResponse("Задача \"" + input + "\" добавлена!");
            }
            case "delete" -> {
                List<String> currentTasks = databaseService.getCurrentTasks(internalUserId);
                if (!currentTasks.contains(input.trim())) {
                    throw new TaskNotFoundException("Задача \"" + input + "\" не найдена!");
                }
                databaseService.deleteTask(internalUserId, input);
                yield new BotResponse("🗑️ Задача \"" + input + "\" удалена!");
            }
            case "done" -> {
                List<String> currentTasks = databaseService.getCurrentTasks(internalUserId);
                if (!currentTasks.contains(input.trim())) {
                    throw new TaskNotFoundException("Задача \"" + input + "\" не найдена!");
                }
                databaseService.markTaskDone(internalUserId, input);
                yield new BotResponse("✅ Задача \"" + input + "\" выполнена!");
            }
            case "export" -> {
                DatabaseService.TaskData taskData = databaseService.exportTasks(internalUserId);
                File exportFile = fileWork.export(taskData.getCurrentTasks(), taskData.getCompletedTasks(), input);
                yield new BotResponse("Ваши задачи экспортированы в файл: " + exportFile.getName(),
                        exportFile, exportFile.getName());
            }
            default -> throw new UnknownOperation("Неизвестная операция: " + operation);
        };
    }

    /**
     * Обрабатывает отображение текущих задач.
     */
    private BotResponse handleShowTasks(String internalUserId) throws SQLException {
        if (internalUserId == null) {
            return new BotResponse("Ошибка: пользователь не авторизован");
        }

        List<String> tasks = databaseService.getCurrentTasks(internalUserId);
        if (tasks.isEmpty()) {
            return new BotResponse("📝 Список задач пуст!");
        }
        StringBuilder sb = new StringBuilder("📝 Ваши задачи:\n");
        for (int i = 0; i < tasks.size(); i++) {
            sb.append(i + 1).append(". ").append(tasks.get(i)).append("\n");
        }
        return new BotResponse(sb.toString());
    }

    /**
     * Обрабатывает отображение выполненных задач.
     */
    private BotResponse handleShowCompletedTasks(String internalUserId) throws SQLException {
        if (internalUserId == null) {
            return new BotResponse("Ошибка: пользователь не авторизован");
        }

        List<String> completedTasks = databaseService.getCompletedTasks(internalUserId);
        if (completedTasks.isEmpty()) {
            return new BotResponse("✅ Список выполненных задач пуст!");
        }
        StringBuilder sb = new StringBuilder("✅ Выполненные задачи:\n");
        for (int i = 0; i < completedTasks.size(); i++) {
            sb.append(i + 1).append(". ").append(completedTasks.get(i)).append("\n");
        }
        return new BotResponse(sb.toString());
    }

    /**
     * Исключение для случаев, когда задача не найдена
     */
    public static class TaskNotFoundException extends Exception {
        public TaskNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Исключение для случаев, когда задача уже существует
     */
    public static class TaskAlreadyExistsException extends Exception {
        public TaskAlreadyExistsException(String message) {
            super(message);
        }
    }

    /**
     * Исключение для случаев, когда пользователь не авторизован
     */
    public class UserAuthorized extends Exception {
        public UserAuthorized(String message) {
            super(message);
        }
    }

    /**
     * Исключение для случаев, когда пытаются вызвать неизвестную операцию
     */
    public class UnknownOperation extends Exception {
        public UnknownOperation(String message) {
            super(message);
        }
    }
}