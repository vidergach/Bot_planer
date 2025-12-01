package org.example;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

/**
 * Класс для обработки сообщений пользователя и управления задачами.
 */
public class MessageHandler {
    private final DatabaseService databaseService;
    private final AuthService authService;
    private final OperationService operationService;
    private final SubtaskService subtaskService;
    private final Keyboard keyboard;
    private final FileWork fileWork = new FileWork();

    private final String START_MESSAGE = """
            Добро пожаловать в планировщик задач! \uD83D\uDC31 📝
            Я могу организовывать ваши задачи.
            Можете воспользоваться кнопками для удобства)

            Команды:
            /add - добавить задачу
            /tasks - показать список задач
            /done - отметить выполненной
            /dTask - список выполненных задач
            /delete - удалить задачу
            /expand - расширить задачу
            /export - предоставить список задач пользователя в файле
            /import - загрузить список задач из файла
            /exit - выйти из аккаунта
            /help - помощь
            
            Команды для подзадач:
            /add_subtasks_with_gpt - добавить подзадачи с помощью чата GPT
            /add_subtask - добавить подзадачу
            /delete_subtask - удалить подзадачу
            /edit_subtask - изменить подзадачу
            /finish_subtask - окончить расширение задачи
            """;

    private final String HELP_MESSAGE = """
            Справка по работе:
            Я планировщик задач😊 📝
            Используйте кнопки для удобства.
            
            Например:
            \u2795 Добавить задачу
            - Полить цветы
            - Задача "Полить цветы" добавлена!

            \uD83D\uDCDD Показать список задач
            - Вот список ваших задач:
              1. Полить цветы
              2. Накормить кота

            \u2714 Выполнено
            - Полить цветы
            - Задача "Полить цветы" отмечена выполненной!
            """;

    /**
     * Конструктор по умолчанию, инициализирует сервис базы данных.
     */
    public MessageHandler() {
        this.databaseService = new DatabaseService();
        this.authService = new AuthService(databaseService);
        this.operationService = new OperationService(databaseService);
        this.keyboard = new Keyboard();
        this.subtaskService = new SubtaskService(databaseService, keyboard);
    }

    /**
     * Конструктор для тестирования
     */
    public MessageHandler(DatabaseService databaseService) {
        this.databaseService = databaseService;
        this.authService = new AuthService(databaseService);
        this.operationService = new OperationService(databaseService);
        this.keyboard = new Keyboard();
        this.subtaskService = new SubtaskService(databaseService, keyboard);
    }

    /**
     * Метод обработки пользовательского ввода.
     */
    public BotResponse processUserInput(String userInput, String userId, String platformType) {
        System.out.println("сообщение: " + userInput + " от: " + userId + " платформа: " + platformType);
        try {
            if (subtaskService.isUserInSubtaskMode(userId)) {
                return subtaskService.processSubtaskInput(userId, userInput);
            }

            if (operationService.isUserInOperationProcess(userId)) {
                return operationService.handleOperationStep(userId, userInput);
            }

            if (authService.isUserInAuthProcess(userId)) {
                return authService.handleAuthStep(userId, userInput);
            }

            String[] parts = userInput.trim().split("\\s+", 2);
            String command = parts[0];
            String parameter = parts.length > 1 ? parts[1].trim() : "";

            if (!authService.isUserAuthenticated(userId, platformType)) {
                return handleUnauthorizedUser(command, userId, platformType);
            }

            return processCommand(command, parameter, userId, platformType);
        } catch (Exception e) {
            e.printStackTrace();
            return new BotResponse("Произошла ошибка: " + e.getMessage());
        }
    }

    /**
     * Обрабатывает неавторизованного пользователя
     */
    private BotResponse handleUnauthorizedUser(String command, String userId, String platformType) {
        if (command.equals("/registration")) {
            return authService.handleRegistration(userId, platformType);
        } else if (command.equals("/login")) {
            return authService.handleLogin(userId, platformType);
        }
        return new BotResponse(authService.getWelcomeMessage());
    }

    /**
     * Обрабатывает команду пользователя
     */
    private BotResponse processCommand(String command, String parameter, String userId, String platformType) {
        try {
            String internalUserId = databaseService.getUserIdByPlatform(userId);
            if (internalUserId == null) {
                return new BotResponse("Ошибка: пользователь не авторизован. Пожалуйста, войдите снова.");
            }

            if (isSubtaskCommand(command)) {
                return subtaskService.handleSubtaskCommand(command, userId);
            }

            if (command.matches("\\d+")) {
                List<String> tasks = databaseService.getCurrentTasks(internalUserId);
                int taskNumber = Integer.parseInt(command);
                if (taskNumber >= 1 && taskNumber <= tasks.size()) {
                    return handleExpandCommand(userId, command, internalUserId);
                }
            }

            if (command.equals("/expand") || command.equals("Расширить задачу")) {
                String full = command + (parameter.isEmpty() ? "" : " " + parameter);
                return handleExpandCommand(userId, full, internalUserId);
            }

            return switch (command) {
                case "/start" -> new BotResponse(START_MESSAGE);
                case "/help" -> new BotResponse(HELP_MESSAGE);
                case "/add" -> operationService.handleOperation("add", parameter, userId,
                        """
                                Введите задачу для добавления:
                                Например: Купить молоко""");
                case "/tasks" -> operationService.handleShowTasks(internalUserId);
                case "/done" -> operationService.handleOperation("done", parameter, userId,
                        """
                                Введите название задачи для отметки выполнения:
                                Например: Купить молоко""");
                case "/dTask" -> operationService.handleShowCompletedTasks(internalUserId);
                case "/delete" -> operationService.handleOperation("delete", parameter, userId,
                        """
                                Введите название задачи для удаления:
                                Например: Купить молоко""");
                case "/registration" -> authService.handleRegistration(userId, platformType);
                case "/login" -> authService.handleLogin(userId, platformType);
                case "/exit" -> authService.handleExit(userId, platformType);
                case "/export" -> operationService.handleOperation("export", parameter, userId,
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
     * Проверяет, является ли команда командой подзадачи
     */
    private boolean isSubtaskCommand(String command) {
        return command.equals("/add_subtask") || command.equals("/delete_subtask") ||
                command.equals("/edit_subtask") || command.equals("/finish_expand") ||
                command.equals("/add_subtasks_with_gpt") || command.equals("/save_subtasks_from_gpt") ||
                command.equals("/delete_subtasks_from_gpt") ||
                command.equals("\u2795 Добавить подзадачу") || command.equals("\u2718 Удалить подзадачу") ||
                command.equals("Изменить подзадачу") || command.equals("Окончить расширение") ||
                command.equals("GPT добавление подзадач") || command.equals("Сохранить") || command.equals("Удалить");
    }

    /**
     * Обрабатывает импорт задач из файла
     */
    public BotResponse processImport(InputStream inputStream, String userId) {
        try {
            String internalUserId = databaseService.getUserIdByPlatform(userId);
            if (internalUserId == null) {
                throw new IllegalArgumentException("Ошибка: пользователь не авторизован. Пожалуйста, войдите снова.");
            }

            FileWork.FileData importedData = fileWork.importData(inputStream);
            for (String task : importedData.current_tasks()) {
                try {
                    databaseService.addTask(internalUserId, task);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            for (String task : importedData.completed_tasks()) {
                try {
                    databaseService.markTaskDone(internalUserId, task);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return new BotResponse("""
                    Импорт завершен успешно!
                    Можете проверить списки с помощью команд /tasks и /dTask
                    """);
        } catch (Exception e) {
            e.printStackTrace();
            return new BotResponse("Ошибка при импорте: " + e.getMessage());
        }
    }

    /**
     * Обрабатывает команду расширения задачи
     */
    private BotResponse handleExpandCommand(String userId, String userInput, String internalUserId) throws SQLException {
        if (userInput.trim().equals("/expand") || userInput.trim().equals("Расширить задачу")) {
            List<String> tasks = databaseService.getCurrentTasks(internalUserId);
            if (tasks.isEmpty()) {
                return new BotResponse("Нет задач для расширения");
            }

            StringBuilder sb = new StringBuilder("Выберите задачу, которую хотите расширить:\n");
            for (int i = 0; i < tasks.size(); i++) {
                sb.append(i + 1).append(". ").append(tasks.get(i)).append("\n");
            }
            sb.append("\nВведите номер задачи:");
            return new BotResponse(sb.toString());
        }
        if (userInput.trim().matches("\\d+")) {
            try {
                List<String> tasks = databaseService.getCurrentTasks(internalUserId);
                int taskNumber = Integer.parseInt(userInput.trim());
                if (taskNumber < 1 || taskNumber > tasks.size()) {
                    return new BotResponse("Выберите номер из списка");
                }
                String selectedTask = tasks.get(taskNumber - 1);
                Integer taskId = databaseService.getTaskId(internalUserId, selectedTask);

                if (taskId == null) {
                    throw new IllegalArgumentException("Задача не найдена.");
                }
                subtaskService.startSubtaskMode(userId, taskId, selectedTask);
                return new BotResponse(subtaskService.getSubtaskMessage());
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return new BotResponse("Пожалуйста, введите номер задачи:");
            }
        }
        return new BotResponse("Используйте: /expand [номер_задачи] или просто /expand для выбора из списка");
    }

    /**
     * Проверяет, находится ли пользователь в режиме работы с подзадачами
     */
    public boolean isUserInSubtaskMode(String userId) {
        return subtaskService.isUserInSubtaskMode(userId);
    }

    /**
     * Проверяет, нужно ли показывать клавиатуру GPT для пользователя
     */
    public boolean shouldShowGptKeyboard(String userId) {
        return subtaskService.shouldGptKeyboard(userId);
    }

    /**
     * Возвращает клавиатуру GPT
     */
    public Object getGptKeyboard() {
        return subtaskService.getGptKeyboard();
    }

}