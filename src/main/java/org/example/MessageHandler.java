package org.example;

import java.io.File;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class MessageHandler {
    private final DatabaseService databaseService;
    private final Map<String, AuthState> authStates = new ConcurrentHashMap<>();
    private final FileWork fileWork = new FileWork();

    public MessageHandler() {
        this.databaseService = new DatabaseService();
    }

    public class BotResponse {
        private final String message;
        private final File file;
        private final String fileName;

        public BotResponse(String message) {
            this.message = message;
            this.file = null;
            this.fileName = null;
        }

        public BotResponse(String message, File file, String fileName) {
            this.message = message;
            this.file = file;
            this.fileName = fileName;
        }

        public String getMessage() {
            return message;
        }

        public File getFile() {
            return file;
        }

        public String getFileName() {
            return fileName;
        }

        public boolean hasFile() {
            return file != null;
        }
    }

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

    private class CommandParts {
        private final String command;
        private final String parameter;

        public CommandParts(String command, String parameter) {
            this.command = command;
            this.parameter = parameter;
        }

        public String getCommand() {
            return command;
        }

        public String getParameter() {
            return parameter;
        }
    }

    private static final String WELCOME_MESSAGE = """
            Добро пожаловать в планировщик задач! \uD83D\uDC31 📝

            Для начала работы необходимо авторизоваться:
            /registration - Регистрация
            /integration - Войти в аккаунт

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
            /export - предоставить список задач пользователя в файле
            /import - загрузить список задач из файла
            /help - помощь
            """;

    private static final String HELP_MESSAGE = """
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

    public BotResponse processUserInput(String userInput, String userId, String platformType) {
        System.out.println("сообщение: " + userInput + " от: " + userId + " платформа: " + platformType);
        try {
            CommandParts parts = parseCommand(userInput);

            if (isUserAuthenticated(userId, platformType)) {
                return processCommand(parts.getCommand(), parts.getParameter(), userId, platformType);
            }

            if (authStates.containsKey(userId)) {
                return handleAuthStep(userId, userInput);
            }

            String command = parts.getCommand();
            if (command.equals("/registration") || command.equals("/integration")) {
                return processAuthCommand(command, userId, platformType);
            }
            return new BotResponse(WELCOME_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            return new BotResponse("Произошла ошибка: " + e.getMessage());
        }
    }

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

    private BotResponse processCommand(String command, String parameter, String userId, String platformType) {
        try {
            String internalUserId = databaseService.getUserIdByPlatform(userId);
            if (internalUserId == null) {
                return new BotResponse("Ошибка: пользователь не авторизован. Пожалуйста, войдите снова.");
            }

            return switch (command) {
                case "/start" -> new BotResponse(START_MESSAGE);
                case "/help" -> new BotResponse(HELP_MESSAGE);
                case "/add" -> handleAddTask(parameter, internalUserId);
                case "/tasks" -> handleShowTasks(internalUserId);
                case "/done" -> handleMarkTaskDone(parameter, internalUserId);
                case "/dTask" -> handleShowCompletedTasks(internalUserId);
                case "/delete" -> handleDeleteTask(parameter, internalUserId);
                case "/registration" -> handleRegistration(userId, platformType);
                case "/integration" -> handleIntegration(userId, platformType);
                case "/export" -> handleExport(parameter, internalUserId);
                case "/import" -> new BotResponse("Для импорта отправьте JSON файл с задачами");
                default -> new BotResponse("""
                        Неизвестная команда.
                        Введите /help для просмотра доступных команд.""");
            };
        } catch (Exception e) {
            e.printStackTrace();
            return new BotResponse("Ошибка при выполнении команды: " + e.getMessage());
        }
    }

    public BotResponse processImport(InputStream inputStream, String userId, String platformType) {
        try {
            String internalUserId = databaseService.getUserIdByPlatform(userId);
            if (internalUserId == null) {
                return new BotResponse("Ошибка: пользователь не авторизован. Пожалуйста, войдите снова.");
            }
            FileWork.FileData importedData = fileWork.importData(inputStream);
            int addedCurrentTasks = 0;
            int addedCompletedTasks = 0;

            for (String task : importedData.current_tasks()) {
                try {
                    databaseService.addTask(internalUserId, task);
                    addedCurrentTasks++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            for (String task : importedData.completed_tasks()) {
                try {
                    databaseService.markTaskDone(internalUserId, task);
                    addedCompletedTasks++;
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

    private BotResponse handleAddTask(String parameter, String internalUserId) {
        if (parameter.isEmpty()) {
            return new BotResponse("""
                    Укажите задачу после /add
                    Например: /add Купить молоко""");
        } else {
            String taskText = parameter.trim();
            try {
                databaseService.addTask(internalUserId, taskText);
                return new BotResponse("Задача \"" + taskText + "\" добавлена!");
            } catch (Exception e) {
                e.printStackTrace();
                return new BotResponse("Ошибка добавления задачи: " + e.getMessage());
            }
        }
    }

    private BotResponse handleShowTasks(String internalUserId) {
        try {
            List<String> tasks = databaseService.getCurrentTasks(internalUserId);
            if (tasks.isEmpty()) {
                return new BotResponse("📝 Список задач пуст!");
            }
            StringBuilder sb = new StringBuilder("📝 Ваши задачи:\n");
            for (int i = 0; i < tasks.size(); i++) {
                sb.append(i + 1).append(". ").append(tasks.get(i)).append("\n");
            }
            return new BotResponse(sb.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            return new BotResponse("Ошибка при получении задач: " + e.getMessage());
        }
    }

    private BotResponse handleMarkTaskDone(String parameter, String internalUserId) {
        if (parameter.isEmpty()) {
            return new BotResponse("""
                    Введите название задачи для отметки выполнения:
                    Например: Купить молоко""");
        } else {
            String taskText = parameter.trim();
            try {
                databaseService.markTaskDone(internalUserId, taskText);
                return new BotResponse("✅ Задача \"" + taskText + "\" выполнена!");
            } catch (Exception e) {
                e.printStackTrace();
                return new BotResponse("Ошибка в выполнении задачи: " + e.getMessage());
            }
        }
    }

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

    private BotResponse handleDeleteTask(String parameter, String internalUserId) {
        if (parameter.isEmpty()) {
            return new BotResponse("""
                    Введите название задачи для удаления:
                    Например: Купить молоко""");
        } else {
            String taskText = parameter.trim();
            try {
                databaseService.deleteTask(internalUserId, taskText);
                return new BotResponse("\uD83D\uDDD1\uFE0F Задача \"" + taskText + "\" удалена!");
            } catch (Exception e) {
                e.printStackTrace();
                return new BotResponse("Ошибка в удалении задачи: " + e.getMessage());
            }
        }
    }

    private BotResponse handleRegistration(String userId, String platformType) {
        authStates.put(userId, new AuthState("registration", platformType));
        return new BotResponse("""
                📝 Регистрация нового пользователя
                Введите логин:
                """);
    }

    private BotResponse handleIntegration(String userId, String platformType) {
        authStates.put(userId, new AuthState("integration", platformType));
        return new BotResponse("""
                🔑 Вход в аккаунт
                Введите логин:
                """);
    }

    private BotResponse processAuthCommand(String command,String userId, String platformType) {
        return switch (command) {
            case "/registration" -> handleRegistration(userId, platformType);
            case "/integration" -> handleIntegration(userId, platformType);
            default -> new BotResponse(WELCOME_MESSAGE);
        };
    }

    private BotResponse handleAuthStep(String userId, String userInput) {
        AuthState state = authStates.get(userId);

        if ("username".equals(state.step)) {
            return processUsernameStep(state, userInput, userId);
        } else if ("password".equals(state.step)) {
            return processPasswordStep(state, userInput, userId);
        }

        authStates.remove(userId);
        return new BotResponse("Ошибка аутентификации. Попробуйте снова.");
    }

    private BotResponse handleExport(String filename, String internalUserId) {
        if (filename.isEmpty()) {
            return new BotResponse("""
                    Напишите имя файла для экспорта
                    Например: 'list'""");
        }
        try {
            DatabaseService.TaskData taskData = databaseService.exportTasks(internalUserId);
            File exportFile = fileWork.export(taskData.getCurrentTasks(),
                    taskData.getCompletedTasks(), filename.trim());
            return new BotResponse("Ваши задачи экспортированы в файл: "
                    + exportFile.getName(), exportFile, exportFile.getName());
        } catch (Exception e) {
            e.printStackTrace();
            return new BotResponse("Ошибка экспорта: " + e.getMessage());
        }
    }

    private BotResponse processUsernameStep(AuthState state, String userInput, String userId) {
        if (userInput.trim().isEmpty()) {
            return new BotResponse("""
                    Упс, кажется вы забыли ввести логин.
                    Введите логин:
                    """);
        }
        String username = userInput.trim();
        try {
            if ("registration".equals(state.type)) {
                if (databaseService.userExists(username)) {
                    authStates.remove(userId);
                    return new BotResponse("""
                            Пользователь с таким логином уже существует.
                            Используйте другой логин или войдите с помощью /integration.""");
                }
            } else if ("integration".equals(state.type)) {
                if (!databaseService.userExists(username)) {
                    authStates.remove(userId);
                    return new BotResponse("""
                            Пользователь '%s' не найден.
                            Проверьте логин или зарегистрируйтесь с помощью /registration.
                            """.formatted(username));
                }
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
                    return new BotResponse("""
                                ✅ Регистрация завершена успешно!
                                Добро пожаловать, %s!
                                %s""".formatted(state.username, START_MESSAGE));
                } else {
                    authStates.remove(userId);
                    return new BotResponse("""
                            Ошибка регистрации.
                            Попробуйте снова: /registration""");
                }
            } else {
                if (databaseService.authenticateUser(state.username, password, state.platformType, userId)) {
                    authStates.remove(userId);
                    return new BotResponse("""
                                ✅ Вход выполнен успешно!
                                Добро пожаловать обратно, %s

                                %s""".formatted(state.username, START_MESSAGE));
                } else {
                    authStates.remove(userId);
                    return new BotResponse("""
                            Неверный пароль.
                            Попробуйте снова: /integration""");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            authStates.remove(userId);
            return new BotResponse("Ошибка при авторизации: " + e.getMessage());
        }
    }

    private boolean isUserAuthenticated(String userId, String platformType) {
        try {
            return databaseService.getUsername(platformType, userId) != null;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}