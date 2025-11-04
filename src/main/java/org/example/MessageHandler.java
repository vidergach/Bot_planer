package org.example;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Основной обработчик сообщений для бота планировщика задач.
 * Теперь возвращает структурированный ответ с поддержкой файлов
 */
public class MessageHandler {
    private final Map<String, UserData> userDataMap = new ConcurrentHashMap<>();
    private final Map<String, AuthState> authStates = new ConcurrentHashMap<>();
    private final FileWork fileWork = new FileWork();
    private final UserManager userManager = new UserManager();

    /**
     * Структура для возврата ответа бота
     */
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

        public String getMessage() { return message; }
        public File getFile() { return file; }
        public String getFileName() { return fileName; }
        public boolean hasFile() { return file != null; }
    }

    private class AuthState {
        String type;
        String username;
        String step;

        AuthState(String type) {
            this.type = type;
            this.step = "username";
        }
    }

    private class CommandParts {
        private final String command;
        private final String parameter;

        public CommandParts(String command, String parameter) {
            this.command = command;
            this.parameter = parameter;
        }
        public String getCommand() { return command; }
        public String getParameter() { return parameter; }
    }

    private static final String START_MESSAGE = """ 
            Добро пожаловать в планировщик задач! \uD83D\uDC31 📝
            Я могу организовывать ваши задачи.
            Команды:
            /add - добавить задачу
            /tasks - показать список задач
            /done - отметить выполненной
            /dTask - список выполненных задач
            /delete - удалить задачу
            /export - предоставить список задач пользователя в файле
            /import - загрузить список задач из файла
            /help - помощь
            Синхронизация:
            /registration - зарегистрироваться
            /integration - синхронизировать аккаунт
            """;

    private static final String HELP_MESSAGE = """ 
            Справка по работе:
            Я планировщик задач😊 📝
            Мои команды:
            /add - добавить задачу
            /tasks - показать список задач
            /done - отметить выполненной
            /dTask - список выполненных задач
            /delete - удалить задачу
            /export - предоставить список задач пользователя в файле
            /import - загрузить список задач из файла
            /help - помощь
            /registration - зарегистрироваться
            /integration - синхронизировать аккаунт
            
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
            
            /export 'tasks_list.json'
            - Ваш список задач в виде документа (отправка "tasks_list.json")
            
            /import
            - Отправьте JSON файл со списком задач
            - (отправка "tasks_list.json")
            - Задачи успешно добавлены, можете проверить списки с помощью команд /tasks и /dTask
            """;

    /**
     * Основной метод обработки пользовательского ввода.
     * Теперь возвращает структурированный BotResponse
     */
    public BotResponse processUserInput(String userInput, String userId) {
        System.out.println("сообщение: " + userInput + " от: " + userId);

        if (authStates.containsKey(userId)) {
            return handleAuthStep(userId, userInput);
        }
        UserData userData = getUserDataForUserId(userId);
        CommandParts parts = parseCommand(userInput);
        String command = parts.getCommand();
        String parameter = parts.getParameter();
        return processCommand(command, parameter, userId, userData);
    }

    /**
     * Обрабатывает импорт файла
     */
    public BotResponse processImport(InputStream inputStream, String userId) {
        try {
            UserData userData = getUserData(userId);
            FileWork.FileData result = fileWork.importData(inputStream);

            int addedTasks = 0;
            int addedCompleted = 0;
            for (String task : result.current_tasks()) {
                if (!userData.getTasks().contains(task) && !userData.getCompletedTasks().contains(task)) {
                    userData.addTask(task);
                    addedTasks++;
                }
            }
            for (String task : result.completed_tasks()) {
                if (!userData.getCompletedTasks().contains(task)) {
                    if (userData.getTasks().contains(task)) {
                        userData.markTaskDone(task);
                        addedCompleted++;
                    } else if (!userData.getCompletedTasks().contains(task)) {
                        userData.addTask(task);
                        userData.markTaskDone(task);
                        addedCompleted++;
                    }
                }
            }

            return new BotResponse("""
                    Задачи успешно добавлены,
                    можете проверить списки с помощью команд /tasks и /dTask
                    """);
        } catch (Exception e) {
            return new BotResponse("Ошибка при импорте: " + e.getMessage());
        }
    }

    private UserData getUserDataForUserId(String userId) {
        if (!userDataMap.containsKey(userId)) {
            userDataMap.put(userId, new UserData());
        }
        return userDataMap.get(userId);
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

    private BotResponse processCommand(String command, String parameter, String userId, UserData userData) {
        return switch (command) {
            case "/start" -> new BotResponse(START_MESSAGE);
            case "/help" -> new BotResponse(HELP_MESSAGE);
            case "/add" -> new BotResponse(addTask(parameter, userId));
            case "/tasks" -> new BotResponse(showTasks(userId));
            case "/done" -> new BotResponse(markTaskDone(parameter, userId));
            case "/dTask" -> new BotResponse(showCompletedTasks(userId));
            case "/delete" -> new BotResponse(deleteTask(parameter, userId));
            case "/registration" -> new BotResponse(startRegistration(userId));
            case "/integration" -> new BotResponse(startIntegration(userId));
            case "/export" -> handleExport(parameter, userId);
            case "/import" -> new BotResponse("Для импорта отправьте JSON файл с задачами");
            default -> new BotResponse("""
                    Неизвестная команда.
                    Введите /help для просмотра доступных команд.""");
        };
    }

    private BotResponse handleExport(String parameter, String userId) {
        if (parameter.isEmpty()) {
            return new BotResponse("Напишите имя файла после /export");
        }
        try {
            UserData userData = getUserData(userId);
            File exportFile = fileWork.export(userId, userData.getTasks(), userData.getCompletedTasks(), parameter.trim());
            return new BotResponse("Ваши задачи экспортированы в файл: "
                    + exportFile.getName(), exportFile, exportFile.getName());
        } catch (Exception e) {
            return new BotResponse("Ошибка экспорта: " + e.getMessage());
        }
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

    private BotResponse processUsernameStep(AuthState state, String userInput, String userId) {
        if (userInput.trim().isEmpty()) {
            return new BotResponse("""
                    Упс, кажется вы забыли ввести логин.
                    Введите логин:
                    """);
        }
        String username = userInput.trim();
        if ("registration".equals(state.type) && userManager.isUserRegistered(username)) {
            authStates.remove(userId);
            return new BotResponse("""
                    Пользователь с таким логином уже существует.
                    Используйте другой логин.""");
        }
        if ("integration".equals(state.type) && !userManager.isUserRegistered(username)) {
            authStates.remove(userId);
            return new BotResponse("Пользователь '" + username
                    + "' не найден. Проверьте логин.");
        }
        state.username = username;
        state.step = "password";
        return new BotResponse("✅ Отлично! Теперь введите пароль:");
    }

    private BotResponse processPasswordStep(AuthState state, String userInput, String userId) {
        String password = userInput.trim();
        authStates.remove(userId);
        if ("registration".equals(state.type)) {
            return handleRegistration(state, password, userId);
        } else {
            return handleIntegration(state, password, userId);
        }
    }

    private BotResponse handleRegistration(AuthState state, String password, String userId) {
        if (userManager.registerUser(state.username, password)) {
            userManager.authenticateUser(state.username, password, userId);
            return new BotResponse("Регистрация прошла успешно!");
        }
        return new BotResponse("Ошибка регистрации. Попробуйте снова.");
    }

    private BotResponse handleIntegration(AuthState state, String password, String userId) {
        if (userManager.authenticateUser(state.username, password, userId)) {
            synchronizeUserData(userId, state.username);
            return new BotResponse("""
                    Интеграция прошла успешно!
                    Данные синхронизированы.""");
        }
        return new BotResponse("Неверный пароль. Попробуйте снова.");
    }

    private String startRegistration(String userId) {
        authStates.put(userId, new AuthState("registration"));
        return """
                Регистрация
                Введите логин:
                """;
    }

    private String startIntegration(String userId) {
        authStates.put(userId, new AuthState("integration"));
        return """
                Интеграция
                Введите логин:
                """;
    }

    private UserData getUserData(String userId) {
        String username = userManager.getUsername(userId);
        String dataKey = username != null ? username : userId;

        if (!userDataMap.containsKey(dataKey)) {
            userDataMap.put(dataKey, new UserData());
        }
        return userDataMap.get(dataKey);
    }

    private void synchronizeUserData(String oldUserId, String newUsername) {
        UserData oldData = userDataMap.get(oldUserId);
        UserData newData = getUserData(newUsername);

        if (oldData == null || newData == null) return;

        for (String task : oldData.getTasks()) {
            if (!newData.getTasks().contains(task) && !newData.getCompletedTasks().contains(task)) {
                try {
                    newData.addTask(task);
                } catch (IllegalStateException ignored) {}
            }
        }

        for (String task : oldData.getCompletedTasks()) {
            if (!newData.getCompletedTasks().contains(task)) {
                try {
                    if (newData.getTasks().contains(task)) {
                        newData.markTaskDone(task);
                    } else {
                        newData.addTask(task);
                        newData.markTaskDone(task);
                    }
                } catch (Exception ignored) {}
            }
        }

        userDataMap.remove(oldUserId);
    }

    private String addTask(String parameter, String userId) {
        if (parameter.isEmpty()) {
            return """
                    Укажите задачу после /add
                    Например: /add Купить молоко""";
        }
        try {
            getUserData(userId).addTask(parameter);
            return "Задача \"" + parameter + "\" добавлена!";
        } catch (IllegalStateException e) {
            return e.getMessage();
        }
    }

    private String showTasks(String userId) {
        UserData userData = getUserData(userId);
        if (!userData.hasTasks()) {
            return "📝 Список задач пуст!";
        }
        List<String> tasks = userData.getTasks();
        StringBuilder sb = new StringBuilder("📝 Ваши задачи:\n");
        for (int i = 0; i < tasks.size(); i++) {
            sb.append(i + 1).append(". ").append(tasks.get(i)).append("\n");
        }
        return sb.toString();
    }

    private String markTaskDone(String parameter, String userId) {
        if (parameter.isEmpty()) {
            return """
                    Укажите задачу после /done
                    Например: /done Купить молоко""";
        }
        try {
            getUserData(userId).markTaskDone(parameter);
            return "✅ Задача \"" + parameter + "\" выполнена!";
        } catch (IllegalStateException e) {
            return e.getMessage();
        }
    }

    private String showCompletedTasks(String userId) {
        UserData userData = getUserData(userId);
        if (!userData.hasCompletedTasks()) {
            return "✅ Список выполненных задач пуст!";
        }
        List<String> completedTasks = userData.getCompletedTasks();
        StringBuilder sb = new StringBuilder("✅ Выполненные задачи:\n");
        for (int i = 0; i < completedTasks.size(); i++) {
            sb.append(i + 1).append(". ").append(completedTasks.get(i)).append("\n");
        }
        return sb.toString();
    }

    private String deleteTask(String parameter, String userId) {
        if (parameter.isEmpty()) {
            return """
                    Укажите задачу после /delete
                    Например: /delete Купить молоко""";
        }
        try {
            getUserData(userId).deleteTask(parameter);
            return "🗑️ Задача \"" + parameter + "\" удалена!";
        } catch (IllegalStateException e) {
            return e.getMessage();
        }
    }
}