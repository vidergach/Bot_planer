package org.example;

import java.io.File;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Основной класс для обработки сообщений пользователя и управления задачами.
 * Обеспечивает взаимодействие между пользовательским интерфейсом и системой хранения данных.

 * @see DatabaseService
 * @see FileWork
 */
public class MessageHandler {
    private final DatabaseService databaseService;
    private final Map<String, AuthState> authStates = new ConcurrentHashMap<>();
    private final Map<String, Operation> operationStates = new ConcurrentHashMap<>();
    private final FileWork fileWork = new FileWork();

    /**
     * Конструктор по умолчанию, инициализирует сервис базы данных.
     */
    public MessageHandler() {
        this.databaseService = new DatabaseService();
    }

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
     * Основной метод обработки пользовательского ввода.
     *
     * @param userInput ввод пользователя
     * @param userId идентификатор пользователя
     * @param platformType тип платформы
     * @return ответ бота
     */
    public BotResponse processUserInput(String userInput, String userId, String platformType) {
        System.out.println("сообщение: " + userInput + " от: " + userId + " платформа: " + platformType);
        try {
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
                if (command.equals("/registration") || command.equals("/integration")) {
                    return startAuth(command.substring(1), userId, platformType);
                }
                return new BotResponse(WELCOME_MESSAGE);
            }
            return processCommand(command, parameter,userId, platformType);
        } catch (Exception e) {
            e.printStackTrace();
            return new BotResponse("Произошла ошибка: " + e.getMessage());
        }
    }

    /**
     * Обрабатывает операцию, требующую дополнительного ввода от пользователя.
     *
     * @param operation тип операции
     * @param parameter параметр операции
     * @param userId идентификатор пользователя
     * @param prompt сообщение для пользователя
     * @return ответ бота
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
     * Обрабатывает операции после получения ввода.
     *
     * @param userId идентификатор пользователя
     * @param userInput ввод пользователя
     * @return ответ бота
     */
    private BotResponse handleOperationStep(String userId, String userInput) {
        Operation state = operationStates.get(userId);
        operationStates.remove(userId);
        return executeOperation(state.type, userInput.trim(), userId);
    }

    /**
     * Выполняет указанную операцию.
     *
     * @param operation тип операции
     * @param input ввод пользователя
     * @param userId идентификатор пользователя
     * @return ответ бота
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
                    yield new BotResponse("\uD83D\uDDD1\uFE0F Задача \"" + input + "\" удалена!");
                }
                case "done" -> {
                    databaseService.markTaskDone(internalUserId, input);
                    yield new BotResponse("✅ Задача \"" + input + "\" выполнена!");
                }
                case "export" -> {
                    DatabaseService.TaskData taskData = databaseService.exportTasks(internalUserId);
                    File exportFile = fileWork.export(taskData.getCurrentTasks(), taskData.getCompletedTasks(), input);
                    yield new BotResponse("Ваши задачи экспортированы в файл: " + exportFile.getName(), exportFile, exportFile.getName());
                }
                default -> new BotResponse("Неизвестная операция");
            };
        } catch (Exception e) {
            e.printStackTrace();
            return new BotResponse("Ошибка " + getOperationError(operation) + ": " + e.getMessage());
        }
    }

    /**
     * Возвращает ошибки для операции.
     *
     * @param operation тип операции
     * @return описание ошибки
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
     * Обрабатывает команду пользователя.
     *
     * @param command команда
     * @param parameter параметр команды
     * @param userId идентификатор пользователя
     * @param platformType тип платформы
     * @return ответ бота
     */
    private BotResponse processCommand(String command, String parameter, String userId, String platformType) {
        try {
            String internalUserId = databaseService.getUserIdByPlatform(userId);
            if (internalUserId == null) {
                return new BotResponse("Ошибка: пользователь не авторизован. Пожалуйста, войдите снова.");
            }

            return switch (command) {
                case "/start" -> new BotResponse(START_MESSAGE);
                case "/help" -> new BotResponse(HELP_MESSAGE);
                case "/add" -> handleOperation("add", parameter, userId, "Введите задачу для добавления:\nНапример: Купить молоко");
                case "/tasks" -> handleShowTasks(internalUserId);
                case "/done" -> handleOperation("done", parameter, userId, "Введите название задачи для отметки выполнения:\nНапример: Купить молоко");
                case "/dTask" -> handleShowCompletedTasks(internalUserId);
                case "/delete" -> handleOperation("delete", parameter, userId, "Введите название задачи для удаления:\nНапример: Купить молоко");
                case "/registration" -> handleRegistration(userId, platformType);
                case "/integration" -> handleIntegration(userId, platformType);
                case "/export" -> handleOperation("export", parameter, userId, "Напишите имя файла для экспорта\nНапример: 'list'");
                case "/import" -> new BotResponse("Для импорта отправьте JSON файл с задачами");
                default -> new BotResponse("Неизвестная команда.\nВведите /help для просмотра доступных команд.");
            };
        } catch (Exception e) {
            e.printStackTrace();
            return new BotResponse("Ошибка при выполнении команды: " + e.getMessage());
        }
    }

    /**
     * Обрабатывает импорт задач из файла.
     *
     * @param inputStream поток ввода с файлом задач
     * @param userId идентификатор пользователя
     * @return ответ бота с результатом импорта
     */
    public BotResponse processImport(InputStream inputStream, String userId) {
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

    /**
     * Обрабатывает текущие задачи пользователя.
     *
     * @param internalUserId идентификатор пользователя
     * @return ответ бота
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
            }
            return new BotResponse(sb.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            return new BotResponse("Ошибка при получении задач: " + e.getMessage());
        }
    }

    /**
     * Обрабатывает выполненные задачи пользователя.
     *
     * @param internalUserId идентификатор пользователя
     * @return ответ бота
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
     * Начинает процесс аутентификации пользователя.
     *
     * @param type тип аутентификации
     * @param userId идентификатор пользователя
     * @param platformType тип платформы
     * @return ответ бота
     */
    private BotResponse startAuth(String type, String userId, String platformType) {
        authStates.put(userId, new AuthState(type, platformType));
        return new BotResponse(type.equals("registration") ?
                "📝 Регистрация нового пользователя\nВведите логин:" :
                "Вход в аккаунт\nВведите логин:");
    }

    /**
     * Обрабатывает команду регистрации.
     *
     * @param userId идентификатор пользователя
     * @param platformType тип платформы
     * @return ответ бота
     */
    private BotResponse handleRegistration(String userId, String platformType) {
        authStates.put(userId, new AuthState("registration", platformType));
        return new BotResponse("""
                📝 Регистрация нового пользователя
                Введите логин:
                """);
    }

    /**
     * Обрабатывает команду входа в аккаунт.
     *
     * @param userId идентификатор пользователя
     * @param platformType тип платформы
     * @return ответ бота
     */
    private BotResponse handleIntegration(String userId, String platformType) {
        authStates.put(userId, new AuthState("integration", platformType));
        return new BotResponse("""
                🔑 Вход в аккаунт
                Введите логин:
                """);
    }

    /**
     * Обрабатывает шаг аутентификации.
     *
     * @param userId идентификатор пользователя
     * @param userInput ввод пользователя
     * @return ответ бота
     */
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

    /**
     * Обрабатывает шаг ввода логина при аутентификации.
     *
     * @param state состояние аутентификации
     * @param userInput ввод пользователя
     * @param userId идентификатор пользователя
     * @return ответ бота
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

    /**
     * Обрабатывает шаг ввода пароля при аутентификации.
     *
     * @param state состояние аутентификации
     * @param userInput ввод пользователя
     * @param userId идентификатор пользователя
     * @return ответ бота с результатом аутентификации
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

    /**
     * Проверяет, аутентифицирован ли пользователь.
     *
     * @param userId идентификатор пользователя
     * @param platformType тип платформы
     * @return true если пользователь аутентифицирован, false в противном случае
     */
    private boolean isUserAuthenticated(String userId, String platformType) {
        try {
            return databaseService.getUsername(platformType, userId) != null;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}