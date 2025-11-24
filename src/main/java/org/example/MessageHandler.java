package org.example;

import java.io.File;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Класс для обработки сообщений пользователя и управления задачами.
 */
public class MessageHandler {
    private final DatabaseService databaseService;
    private final Map<String, AuthState> authStates = new ConcurrentHashMap<>();
    private final Map<String, Operation> operationStates = new ConcurrentHashMap<>();
    private final Map<String, SubtaskState> expandStates = new ConcurrentHashMap<>();
    private final FileWork fileWork = new FileWork();

    /**
     * Конструктор по умолчанию, инициализирует сервис базы данных.
     */
    public MessageHandler() {
        this.databaseService = new DatabaseService();
    }

    /**
     * Конструктор для тестирования
     */
    public MessageHandler(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    /**
     * Внутренний класс для отслеживания состояния аутентификации пользователя.
     */
    private class AuthState {
        String type;
        String username;
        String step;
        String platformType;

        AuthState(String type, String platformType) {
            this.type = type;
            this.step = "username";
            this.platformType = platformType;
        }
    }

    /**
     * Класс для отслеживания состояния работы с подзадачами.
     */
    private class SubtaskState {
        Integer taskId;
        String taskText;
        String step;
        String selectSubtask;

        SubtaskState(Integer taskId, String taskText) {
            this.taskId = taskId;
            this.taskText = taskText;
            this.step = null;
        }
    }

    /**
     * Класс для отслеживания состояния операции.
     */
    private class Operation {
        String type;
        Operation(String type) {
            this.type = type;
        }
    }

    private static final String WELCOME_MESSAGE = """
            Добро пожаловать в планировщик задач! \uD83D\uDC31 📝

            Для начала работы необходимо авторизоваться:
            /registration - Регистрация
            /login - Войти в аккаунт
            /exit - Выйти из аккаунта

            После авторизации вы сможете использовать все функции планировщика!
            """;

    private static final String START_MESSAGE = """
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
            /add_subtask - добавить подзадачу
            /delete_subtask - удалить подзадачу
            /edit_subtask - изменить подзадачу
            /finish_subtask - окончить расширение задачи
            """;

    private static final String HELP_MESSAGE = """
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

    private final String SUBTASK_MESSAGE = """
            Отлично! Выберите действие, которое хотите сделать:
            /add_subtask - добавить подзадачу
            /delete_subtask - удалить подзадачу
            /edit_subtask - изменить подзадачу
            /finish_expand - окончить расширение задачи    
            """;

    /**
     * Метод обработки пользовательского ввода.
     */
    public BotResponse processUserInput(String userInput, String userId, String platformType) {
        System.out.println("сообщение: " + userInput + " от: " + userId + " платформа: " + platformType);
        try {
            if (expandStates.containsKey(userId)) {
                SubtaskState state = expandStates.get(userId);
                if (state.step != null) {
                    return handleSubtaskInput(userId, userInput, state);
                }
            }

            if (operationStates.containsKey(userId)) {
                return handleOperationStep(userId, userInput);
            }

            if (authStates.containsKey(userId)) {
                return handleAuthStep(userId, userInput);
            }

            String[] parts = userInput.trim().split("\\s+", 2);
            String command = parts[0];
            String parameter = parts.length > 1 ? parts[1].trim() : "";

            if (!isUserAuthenticated(userId, platformType)) {
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
            return handleRegistration(userId, platformType);
        } else if (command.equals("/login")) {
            return handleLogin(userId, platformType);
        }
        return new BotResponse(WELCOME_MESSAGE);
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
                return handleSubtaskCommand(command, userId);
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
                case "/registration" -> handleRegistration(userId, platformType);
                case "/login" -> handleLogin(userId, platformType);
                case "/exit" -> handleExit(userId, platformType);
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
     * Проверяет, является ли команда командой подзадачи
     */
    private boolean isSubtaskCommand(String command) {
        return command.equals("/add_subtask") || command.equals("/delete_subtask") ||
                command.equals("/edit_subtask") || command.equals("/finish_expand") ||
                command.equals("\u2795 Добавить подзадачу") || command.equals("\u2718 Удалить подзадачу") ||
                command.equals("Изменить подзадачу") || command.equals("Окончить расширение");
    }

    /**
     * Обрабатывает операцию, требующую дополнительного ввода от пользователя
     */
    private BotResponse handleOperation(String operation, String parameter, String userId, String prompt) {
        if (parameter.isEmpty()) {
            operationStates.put(userId, new Operation(operation));
            return new BotResponse(prompt);
        } else {
            return executeOperation(operation, parameter, userId);
        }
    }

    /**
     * Обрабатывает операции после получения ввода
     */
    private BotResponse handleOperationStep(String userId, String userInput) {
        Operation state = operationStates.get(userId);
        operationStates.remove(userId);
        return executeOperation(state.type, userInput.trim(), userId);
    }

    /**
     * Выполняет указанную операцию
     */
    private BotResponse executeOperation(String operation, String input, String userId) {
        try {
            String internalUserId = databaseService.getUserIdByPlatform(userId);
            if (internalUserId == null) {
                return new BotResponse("Пользователь не авторизован");
            }

            return switch (operation) {
                case "add" -> {
                    databaseService.addTask(internalUserId, input);
                    yield new BotResponse("Задача \"" + input + "\" добавлена!");
                }
                case "delete" -> {
                    databaseService.deleteTask(internalUserId, input);
                    yield new BotResponse("🗑️ Задача \"" + input + "\" удалена!");
                }
                case "done" -> {
                    databaseService.markTaskDone(internalUserId, input);
                    yield new BotResponse("✅ Задача \"" + input + "\" выполнена!");
                }
                case "export" -> {
                    DatabaseService.TaskData taskData = databaseService.exportTasks(internalUserId);
                    File exportFile = fileWork.export(taskData.getCurrentTasks(), taskData.getCompletedTasks(), input);
                    yield new BotResponse("Ваши задачи экспортированы в файл: " + exportFile.getName(),
                            exportFile, exportFile.getName());
                }
                default -> new BotResponse("Неизвестная команда.\nВведите /help для просмотра доступных команд.");
            };
        } catch (Exception e) {
            e.printStackTrace();
            return new BotResponse("Ошибка " + getOperationError(operation) + ": " + e.getMessage());
        }
    }

    /**
     * Возвращает описание ошибки для операции
     */
    private String getOperationError(String operation) {
        return switch (operation) {
            case "add" -> "добавления задачи";
            case "delete" -> "удаления задачи";
            case "done" -> "выполнения задачи";
            case "export" -> "экспорта";
            default -> "операции";
        };
    }

    /**
     * Обрабатывает импорт задач из файла
     */
    public BotResponse processImport(InputStream inputStream, String userId) {
        try {
            String internalUserId = databaseService.getUserIdByPlatform(userId);
            if (internalUserId == null) {
                return new BotResponse("Ошибка: пользователь не авторизован. Пожалуйста, войдите снова.");
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
     * Обрабатывает текущие задачи пользователя
     */
    private BotResponse handleShowTasks(String internalUserId) {
        try {
            List<String> tasks = databaseService.getCurrentTasks(internalUserId);
            if (tasks.isEmpty()) {
                return new BotResponse("📝 Список задач пуст!");
            }

            StringBuilder sb = new StringBuilder("📝 Ваши задачи:\n");
            for (int i = 0; i < tasks.size(); i++) {
                sb.append(i + 1).append(". ").append(tasks.get(i)).append("\n");
                Integer taskId = databaseService.getTaskId(internalUserId, tasks.get(i));
                if (taskId != null) {
                    List<String> subtasks = databaseService.getSubtasks(taskId);
                    for (int j = 0; j < subtasks.size(); j++) {
                        sb.append(" ").append(i + 1).append(".").append(j + 1).append(" ").append(subtasks.get(j)).append("\n");
                    }
                }
            }
            return new BotResponse(sb.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            return new BotResponse("Ошибка при получении задач: " + e.getMessage());
        }
    }

    /**
     * Обрабатывает выполненные задачи пользователя
     */
    private BotResponse handleShowCompletedTasks(String internalUserId) {
        try {
            List<String> completedTasks = databaseService.getCompletedTasks(internalUserId);
            if (completedTasks.isEmpty()) {
                return new BotResponse("✅ Список выполненных задач пуст!");
            }

            StringBuilder sb = new StringBuilder("✅ Выполненные задачи:\n");
            for (int i = 0; i < completedTasks.size(); i++) {
                sb.append(i + 1).append(". ").append(completedTasks.get(i)).append("\n");
            }
            return new BotResponse(sb.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            return new BotResponse("Ошибка при получении выполненных задач: " + e.getMessage());
        }
    }

    /**
     * Обрабатывает команду регистрации
     */
    private BotResponse handleRegistration(String userId, String platformType) {
        authStates.put(userId, new AuthState("registration", platformType));
        return new BotResponse("""
        📝 Регистрация нового пользователя
        Введите логин:""");
    }

    /**
     * Обрабатывает команду входа в аккаунт
     */
    private BotResponse handleLogin(String userId, String platformType) {
        authStates.put(userId, new AuthState("integration", platformType));
        return new BotResponse("""
        🔑 Вход в аккаунт
        Введите логин:""");
    }

    /**
     * Обрабатывает выход пользователя из аккаунта
     */
    private BotResponse handleExit(String userId, String platformType) {
        try {
            if (isUserAuthenticated(userId, platformType) && databaseService.logoutUser(userId, platformType)) {
                return new BotResponse("""
                        ✅ Вы успешно вышли из аккаунта.
                       
                        Для продолжения работы:
                        /registration - зарегистрироваться
                        /login - войти в существующий аккаунт
                        """);
            }
            return new BotResponse("Вы не авторизованы.");
        } catch (SQLException e) {
            e.printStackTrace();
            return new BotResponse("Ошибка при выходе из аккаунта: " + e.getMessage());
        }
    }

    /**
     * Обрабатывает шаг аутентификации
     */
    private BotResponse handleAuthStep(String userId, String userInput) {
        AuthState state = authStates.get(userId);
        return switch (state.step) {
            case "username" -> processUsernameStep(state, userInput, userId);
            case "password" -> processPasswordStep(state, userInput, userId);
            default -> {
                authStates.remove(userId);
                yield new BotResponse("Ошибка аутентификации. Попробуйте снова.");
            }
        };
    }

    /**
     * Обрабатывает шаг ввода логина при аутентификации
     */
    private BotResponse processUsernameStep(AuthState state, String userInput, String userId) {
        if (userInput.trim().isEmpty()) {
            return new BotResponse("""
                    Упс, кажется вы забыли ввести логин.
                    Введите логин:
                    """);
        }
        String username = userInput.trim();
        try {
            if ("registration".equals(state.type) && databaseService.userExists(username)) {
                authStates.remove(userId);
                return new BotResponse("""
                        Пользователь с таким логином уже существует.
                        Используйте другой логин или войдите с помощью /integration.""");
            } else if ("integration".equals(state.type) && !databaseService.userExists(username)) {
                authStates.remove(userId);
                return new BotResponse("""
                        Пользователь '%s' не найден.
                        Проверьте логин или зарегистрируйтесь с помощью /registration.
                        """.formatted(username));
            }

            state.username = username;
            state.step = "password";
            return new BotResponse("✅Отлично! Теперь введите пароль:");
        } catch (SQLException e) {
            e.printStackTrace();
            authStates.remove(userId);
            return new BotResponse("Ошибка проверки пользователя: " + e.getMessage());
        }
    }

    /**
     * Обрабатывает шаг ввода пароля при аутентификации
     */
    private BotResponse processPasswordStep(AuthState state, String userInput, String userId) {
        String password = userInput.trim();
        if (password.isEmpty()) {
            return new BotResponse("""
                Пароль не может быть пустым.
                Введите пароль:
                """);
        }

        try {
            if ("registration".equals(state.type)) {
                if (databaseService.registerUser(state.username, password)) {
                    databaseService.authenticateUser(state.username, password, state.platformType, userId);
                    authStates.remove(userId);
                    return new BotResponse(String.format("""
                        ✅ Регистрация завершена успешно!
                        Добро пожаловать, %s!
                        %s""", state.username, START_MESSAGE));
                } else {
                    authStates.remove(userId);
                    return new BotResponse("""
                        Ошибка регистрации.
                        Попробуйте снова: /registration""");
                }
            } else {
                if (databaseService.authenticateUser(state.username, password, state.platformType, userId)) {
                    authStates.remove(userId);
                    return new BotResponse(String.format("""
                        ✅ Вход выполнен успешно!
                        Добро пожаловать обратно, %s
                        
                        %s""", state.username, START_MESSAGE));
                } else {
                    authStates.remove(userId);
                    return new BotResponse("Неверный пароль. Попробуйте снова.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            authStates.remove(userId);
            return new BotResponse("Ошибка при авторизации: " + e.getMessage());
        }
    }

    /**
     * Проверяет, аутентифицирован ли пользователь
     */
    private boolean isUserAuthenticated(String userId, String platformType) {
        try {
            return databaseService.getUsername(platformType, userId) != null;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
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
                    return new BotResponse("Задача не найдена.");
                }
                expandStates.put(userId, new SubtaskState(taskId, selectedTask));
                return new BotResponse(SUBTASK_MESSAGE);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return new BotResponse("Пожалуйста, введите номер задачи:");
            }
        }
        return new BotResponse("Используйте: /expand [номер_задачи] или просто /expand для выбора из списка");
    }

    /**
     * Обрабатывает ввод данных в режиме расширения задачи
     */
    private BotResponse handleSubtaskInput(String userId, String userInput, SubtaskState state) {
        try {
            String internalUserId = databaseService.getUserIdByPlatform(userId);
            if (internalUserId == null) {
                expandStates.remove(userId);
                return new BotResponse("Ошибка, пользователь не авторизован.");
            }

            return switch (state.step) {
                case "add_subtask" -> handleAddSubtask(userId, userInput, state.taskId);
                case "delete_subtask" -> handleDeleteSubtask(userId, userInput, state.taskId);
                case "edit_subtask" -> handleEditSubtask(userInput, state);
                default -> {
                    expandStates.remove(userId);
                    yield new BotResponse("Ошибка режима расширения");
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
            return new BotResponse("Ошибка при работе с подзадачами: " + e.getMessage());
        }
    }

    /**
     * Обрабатывает команды работы с подзадачами
     */
    private BotResponse handleSubtaskCommand(String command, String userId) throws SQLException {
        SubtaskState state = expandStates.get(userId);
        if (state == null) {
            return new BotResponse("Сначала выберите задачу для расширения.");
        }

        if (state.step != null) {
            return handleSubtaskInput(userId, command, state);
        }

        return switch (command) {
            case "/add_subtask", "\u2796 Добавить подзадачу" -> {
                state.step = "add_subtask";
                yield new BotResponse("Отлично! Напишите подзадачу для добавления:");
            }
            case "/delete_subtask", "\u2718 Удалить подзадачу" -> {
                state.step = "delete_subtask";
                yield new BotResponse("Отлично! Напишите подзадачу для удаления:");
            }
            case "/edit_subtask", "Изменить подзадачу" -> {
                state.step = "edit_subtask";
                state.selectSubtask = null;
                yield new BotResponse("Отлично! Напишите подзадачу для изменения:");
            }
            case "/finish_expand", "Окончить расширение" -> handleFinishExpand(userId);
            default -> new BotResponse("Используйте кнопки для работы с подзадачами или введите /finish_expand для выхода.");
        };
    }

    /**
     * Обрабатывает добавление подзадачи
     */
    private BotResponse handleAddSubtask(String userId, String userInput, Integer taskId) throws SQLException {
        if (userInput.trim().isEmpty()) {
            return new BotResponse("Отлично! Напишите подзадачу для добавления.");
        }
        try {
            databaseService.addSubtask(taskId, userInput);
            expandStates.get(userId).step = null;
            return new BotResponse("Подзадача добавлена");
        } catch (SQLException e) {
            expandStates.get(userId).step = null;
            if (e.getErrorCode() == 19) {
                return new BotResponse("Подзадача уже существует.");
            }
            throw e;
        }
    }

    /**
     * Обрабатывает удаление подзадачи
     */
    private BotResponse handleDeleteSubtask(String userId, String userInput, Integer taskId) throws SQLException {
        if (userInput.trim().isEmpty()) {
            List<String> subtasks = databaseService.getSubtasks(taskId);
            if (subtasks.isEmpty()) {
                expandStates.get(userId).step = null;
                return new BotResponse("Нет подзадачи для удаления.");
            }
            StringBuilder sb = new StringBuilder("Отлично! Выберите задачу для удаления.");
            for (int i = 0; i < subtasks.size(); i++) {
                sb.append(i + 1).append(". ").append(subtasks.get(i)).append("\n");
            }
            return new BotResponse(sb.toString());
        }
        String subtaskToDelete = userInput.trim();
        List<String> subtasks = databaseService.getSubtasks(taskId);

        if (!subtasks.contains(subtaskToDelete)) {
            expandStates.get(userId).step = null;
            return new BotResponse("Подзадача не найдена.");
        }

        try {
            databaseService.deleteSubtask(taskId, subtaskToDelete);
            expandStates.get(userId).step = null;
            return new BotResponse("Подзадача удалена.");
        } catch (SQLException e) {
            expandStates.get(userId).step = null;
            e.printStackTrace();
            return new BotResponse("Подзадача не найдена.");
        }
    }

    /**
     * Обрабатывает изменение подзадачи
     */
    private BotResponse handleEditSubtask(String userInput, SubtaskState state) throws SQLException {
        if (state.selectSubtask == null) {
            if (userInput.trim().isEmpty()) {
                List<String> subtasks = databaseService.getSubtasks(state.taskId);
                if (subtasks.isEmpty()) {
                    state.step = null;
                    return new BotResponse("Нет подзадач для изменения.");
                }

                StringBuilder sb = new StringBuilder("Отлично! Напишите подзадачу для изменения.\n");
                for (int i = 0; i < subtasks.size(); i++) {
                    sb.append(i + 1).append(". ").append(subtasks.get(i)).append("\n");
                }
                return new BotResponse(sb.toString());
            }

            String selectedSubtask = userInput.trim();
            List<String> subtasks = databaseService.getSubtasks(state.taskId);

            if (!subtasks.contains(selectedSubtask)) {
                state.step = null;
                return new BotResponse("Подзадача не найдена.");
            }

            state.selectSubtask = selectedSubtask;
            return new BotResponse("Напишите новую формулировку:");
        } else {
            if (userInput.trim().isEmpty()) {
                return new BotResponse("Напишите новую формулировку:");
            }
            try {
                databaseService.editSubtask(state.taskId, state.selectSubtask, userInput.trim());
                state.step = null;
                state.selectSubtask = null;
                return new BotResponse("Подзадача изменена.");
            } catch (SQLException e) {
                state.step = null;
                state.selectSubtask = null;
                e.printStackTrace();
                return new BotResponse("Не удалось изменить подзадачу.");
            }
        }
    }

    /**
     * Завершает режим работы с подзадачами
     */
    private BotResponse handleFinishExpand(String userId) {
        expandStates.remove(userId);
        return new BotResponse("Добавление подзадач завершено! Вы можете посмотреть список задач.");
    }

    /**
     * Проверяет, находится ли пользователь в режиме работы с подзадачами
     */
    public boolean isUserInSubtaskMode(String userId) {
        return expandStates.containsKey(userId);
    }
}