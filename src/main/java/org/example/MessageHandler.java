package org.example;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Основной обработчик сообщений для бота планировщика задач.
 * Класс обрабатывает команды и управляет данными пользователей.
 * Поддерживает авторизацию, регистрацию и все операции с задачами
 */
public class MessageHandler {
    private final Map<String, UserData> userDataMap = new ConcurrentHashMap<>();
    private final Map<String, AuthState> authStates = new ConcurrentHashMap<>();
    private final Map<String, String> operations = new ConcurrentHashMap<>();
    private final FileWork fileWork = new FileWork();
    private final UserManager userManager = new UserManager();

    /**
     * Структура для возврата ответа бота
     * Содержит текстовое сообщение для отправки пользователю.
     */
    public class BotResponse {
        private final String message;
        private final File file;
        private final String fileName;

        /**
         * Создает текстовый ответ
         *
         * @param message текстовое сообщение
         */
        public BotResponse(String message) {
            this.message = message;
            this.file = null;
            this.fileName = null;
        }

        /**
         * Создает ответ с файлом
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

        public String getMessage() { return message; }
        public File getFile() { return file; }
        public String getFileName() { return fileName; }
        public boolean hasFile() { return file != null; }
    }

    /**
     * Класс для отслеживания аутентификации пользователя.
     * Хранит информацию о регистрации.
     */
    private class AuthState {
        String type;
        String username;
        String step;
        String exportFilename;

        AuthState(String type) {
            this.type = type;
            this.step = "username";
        }

        AuthState(String type, String step){
            this.type = type;
            this.step = step;
        }
    }


    /**
     * Вспомогательный класс, разделяет ввод пользователя на команду и параметры.
     */
    private class CommandParts {
        private final String command;
        private final String parameter;


        /**
         * Создает части команды.
         *
         * @param command основная команда
         * @param parameter параметры команды
         */
        public CommandParts(String command, String parameter) {
            this.command = command;
            this.parameter = parameter;
        }
        public String getCommand() { return command; }
        public String getParameter() { return parameter; }
    }

    private static final String WELCOME_MESSAGE = """ 
            Добро пожаловать в планировщик задач! \uD83D\uDC31 📝
            
            ⚠️ Для начала работы необходимо авторизоваться:
            /registration - Регистрация
            /integration - Войти в аккаунт
            Используйте кнопки ниже)
            
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
     * Основной метод обработки пользовательского ввода.да.
     * Теперь возвращает структурированный BotResponse
     *
     *  @param userInput текст сообщения от пользователяя
     *  @param userId идентификатор пользователя
     *  @return ответ бота
     */
    public BotResponse processUserInput(String userInput, String userId) {
        System.out.println("сообщение: " + userInput + " от: " + userId);

        try {
            if (operations.containsKey(userId)){
                return handleOperation(userId, userInput);
            }
            if (authStates.containsKey(userId)){
                return handleAuthStep(userId, userInput);
            }
            if (!isUserAuthenticated(userId)) {
                if (!authStates.containsKey(userId)) {
                    CommandParts parts = parseCommand(userInput);
                    String command = parts.getCommand();

                    if (command.equals("/registration") ||
                            command.equals("/integration")) {
                        return processCommand(command, parts.getParameter(), userId, null);
                    } else {
                        return new BotResponse(WELCOME_MESSAGE);
                    }
                } else {
                    return handleAuthStep(userId, userInput);
                }
            }

            UserData userData = getUserDataForUserId(userId);
            CommandParts parts = parseCommand(userInput);
            String command = parts.getCommand();
            String parameter = parts.getParameter();
            return processCommand(command, parameter, userId, userData);
        } catch (Exception e) {
            e.printStackTrace();
            return new BotResponse("Произошла ошибка: " + e.getMessage());
        }
    }

    /**
     * Обрабатывает операцию с задачей после получения названия от пользователя.
     *
     * @param userId идентификатор пользователя
     * @param userInput название задачи
     * @return ответ с результатом выполнения
     * @throws IllegalStateException если операция не может быть выполнена
     */
    private BotResponse handleOperation(String userId, String userInput){
        String operation = operations.remove(userId);
        String taskName = userInput.trim();
        if (taskName.isEmpty()){
            return new BotResponse("Имя файла не может отсутствовать");
        }

        try {
            UserData userData = getUserData(userId);
            switch (operation) {
                case "add":
                    userData.addTask(taskName);
                    return new BotResponse("Задача \"" + taskName + "\" добавлена!");
                case "done":
                    userData.markTaskDone(taskName);
                    return new BotResponse("✅ Задача \"" + taskName + "\" выполнена!");
                case "delete":
                    userData.deleteTask(taskName);
                    return new BotResponse("🗑️ Задача \"" + taskName + "\" удалена!");
                default:
                    return new BotResponse("Неизвестная команда");
            }
        } catch (IllegalStateException e) {
            return new BotResponse(e.getMessage());
        } catch (Exception e){
            e.printStackTrace();
            return new BotResponse("Произошла ошибка" + e.getMessage());
        }
    }
    /**
     * Проверяет, авторизован ли пользователь
     *
     * @param userId идентификатор пользователя
     * @return true если пользователь авторизован, false в противном случае
     */
    private boolean isUserAuthenticated(String userId) {
        String username = userManager.getUsername(userId);
        return username != null && !username.trim().isEmpty();
    }

    /**
     * Обрабатывает импорт задач из файла.
     * Читает задачи из входного потока (JSON файла) и добавляет их в список
     * задач пользователя.
     *
     * @param inputStream поток данных из загруженного файла
     * @param userId идентификатор пользователя
     * @return ответ с результатом импорта
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
            e.printStackTrace();
            return new BotResponse("Ошибка при импорте: " + e.getMessage());
        }
    }

    /**
     * Получает или создает данные пользователя по идентификатору.
     *
     * @param userId идентификатор пользователя
     * @return объект UserData пользователя
     */
    private UserData getUserDataForUserId(String userId) {
        if (!userDataMap.containsKey(userId)) {
            userDataMap.put(userId, new UserData());
        }
        return userDataMap.get(userId);
    }

    /**
     * Разбирает пользовательский ввод на команду и параметры.
     *
     * @param userInput исходный ввод пользователя
     * @return объект CommandParts
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
     * Выполняет соответствующую операцию в зависимости от команды и возвращает результат.
     *
     * @param command команда для выполнения
     * @param parameter параметры команды
     * @param userId идентификатор пользователя
     * @param userData данные пользователя
     * @return ответ с результатом выполнения команды
     */
    private BotResponse processCommand(String command, String parameter, String userId, UserData userData) {
        try {
            return switch (command) {
                case "/start" -> new BotResponse(isUserAuthenticated(userId) ? START_MESSAGE : WELCOME_MESSAGE);
                case "/help" -> new BotResponse(HELP_MESSAGE);
                case "/add" -> handleAddTask(userId);
                case "/tasks" -> handleShowTasks(userId);
                case "/done" -> handleMarkTaskDone(userId);
                case "/dTask" -> handleShowCompletedTasks(userId);
                case "/delete" -> handleDeleteTask(userId);
                case "/registration" -> handleRegistration(userId);
                case "/integration" -> handleIntegration(userId);
                case "/export" -> handleExport(parameter, userId);
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

    /**
     * Обрабатывает добавление новой задачи.
     *
     * @param userId идентификатор пользователя
     * @return ответ с результатом операции
     */
    private BotResponse handleAddTask(String userId) {
        operations.put(userId, "add");
        return new BotResponse("""
                Введите название задачи для добавления:
                Например: Купить молоко""");
    }

    /**
     * Обрабатывает отображение списка текущих задач.
     *
     * @param userId идентификатор пользователя
     * @return ответ со списком задач
     */
    private BotResponse handleShowTasks(String userId) {
        UserData userData = getUserData(userId);
        if (!userData.hasTasks()) {
            return new BotResponse("📝 Список задач пуст!");
        }
        List<String> tasks = userData.getTasks();
        StringBuilder sb = new StringBuilder("📝 Ваши задачи:\n");
        for (int i = 0; i < tasks.size(); i++) {
            sb.append(i + 1).append(". ").append(tasks.get(i)).append("\n");
        }
        return new BotResponse(sb.toString());
    }

    /**
     * Обрабатывает отметку задачи как выполненной.
     *
     * @param userId идентификатор пользователя
     * @return ответ с результатом операции
     */
    private BotResponse handleMarkTaskDone(String userId) {
        operations.put(userId, "done");
        return new BotResponse("""
                Введите название задачи для отметки выполнения:
                Например: Купить молоко""");
    }

    /**
     * Обрабатывает отображение списка выполненных задач.
     *
     * @param userId идентификатор пользователя
     * @return ответ со списком выполненных задач
     */
    private BotResponse handleShowCompletedTasks(String userId) {
        UserData userData = getUserData(userId);
        if (!userData.hasCompletedTasks()) {
            return new BotResponse("✅ Список выполненных задач пуст!");
        }
        List<String> completedTasks = userData.getCompletedTasks();
        StringBuilder sb = new StringBuilder("✅ Выполненные задачи:\n");
        for (int i = 0; i < completedTasks.size(); i++) {
            sb.append(i + 1).append(". ").append(completedTasks.get(i)).append("\n");
        }
        return new BotResponse(sb.toString());
    }

    /**
     * Обрабатывает удаление задачи из списка.
     *
     * @param userId идентификатор пользователя
     * @return ответ с результатом операции
     */
    private BotResponse handleDeleteTask(String userId) {
        operations.put(userId, "delete");
        return new BotResponse("""
                Введите название задачи для удаления:
                Например: Купить молоко""");
    }


    /**
     * Обрабатывает экспорт задач в файл.
     *
     * @param parameter имя файла для экспорта
     * @param userId идентификатор пользователя
     * @return ответ с результатом операции и файлом для отправки
     */
    private BotResponse handleExport(String parameter, String userId) {
        if (parameter.isEmpty()) {
            authStates.put(userId, new AuthState("export", "filename"));
            return new BotResponse("""
                    Напишите имя файла для экспорта
                    Например: 'list'""");
        }
        return handleExportFilename(userId, parameter.trim());
    }

    /**
     * Процесс регистрации нового пользователя.
     *
     * @param userId идентификатор пользователя
     * @return ответ с запросом логина
     */
    private BotResponse handleRegistration(String userId) {
        authStates.put(userId, new AuthState("registration"));
        return new BotResponse("""
                📝 Регистрация нового пользователя
                Введите логин:
                """);
    }

    /**
     * Процесс входа в существующий аккаунт.
     *
     * @param userId идентификатор пользователя
     * @return ответ с запросом логина
     */
    private BotResponse handleIntegration(String userId) {
        authStates.put(userId, new AuthState("integration"));
        return new BotResponse("""
                🔑 Вход в аккаунт
                Введите логин:
                """);
    }

    /**
     * Обрабатывает шаг процесса аутентификации.
     *
     * @param userId идентификатор пользователя
     * @param userInput ввод пользователя (логин или пароль)
     * @return ответ с запросом следующего шага или результатом аутентификации
     */
    private BotResponse handleAuthStep(String userId, String userInput) {
        AuthState state = authStates.get(userId);
        if ("export".equals(state.type) && "filename".equals(state.step)){
            return handleExportFilename(userId, userInput);
        }
        if ("username".equals(state.step)) {
            return processUsernameStep(state, userInput, userId);
        } else if ("password".equals(state.step)) {
            return processPasswordStep(state, userInput, userId);
        }
        authStates.remove(userId);
        return new BotResponse("Ошибка аутентификации. Попробуйте снова.");
    }

    /**
     * Обрабатывает экспорт задач пользователя в файл с указанным именем.
     *
     * @param userId идентификатор пользователя
     * @param filename имя файла для экспорта задач
     * @return BotResponse с результатом операции
     */
    private BotResponse handleExportFilename(String userId, String filename){
        authStates.remove(userId);
            if (filename.trim().isEmpty()){
                return new BotResponse("Имя файла не может отсутствовать");
            }
            try{
                UserData userData = getUserData(userId);
                File exportFile = fileWork.export(userId, userData.getTasks(), userData.getCompletedTasks(), filename.trim());
                return new BotResponse("Ваши задачи экспортированы в файл: "
                            + exportFile.getName(), exportFile, exportFile.getName());
            } catch (Exception e) {
                e.printStackTrace();
                return new BotResponse("Ошибка экспорта: " + e.getMessage());
            }
    }
    /**
     * Обрабатывает ввод логина в процессе аутентификации.
     *
     * @param state текущее состояние аутентификации
     * @param userInput введенный логин
     * @param userId идентификатор пользователя
     * @return ответ с запросом пароля или сообщением об ошибке
     */
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
                    Используйте другой логин или войдите с помощью /integration.""");
        }
        if ("integration".equals(state.type) && !userManager.isUserRegistered(username)) {
            authStates.remove(userId);
            return new BotResponse("""
                    Пользователь '%s' не найден.
                    Проверьте логин или зарегистрируйтесь с помощью /registration."""
                    .formatted(username));
        }
        state.username = username;
        state.step = "password";
        return new BotResponse("✅Отлично! Теперь введите пароль:");
    }

    /**
     * Обрабатывает ввод пароля в процессе аутентификации.
     *
     * @param state текущее состояние аутентификации
     * @param userInput введенный пароль
     * @param userId идентификатор пользователя
     * @return ответ с результатом аутентификации
     */
    private BotResponse processPasswordStep(AuthState state, String userInput, String userId) {
        String password = userInput.trim();
        authStates.remove(userId);
        try {
            if ("registration".equals(state.type)) {
                return handleRegistration(state, password, userId);
            } else {
                return handleIntegration(state, password, userId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new BotResponse("Ошибка при авторизации: " + e.getMessage());
        }
    }

    /**
     * Завершает процесс регистрации нового пользователя.
     *
     * @param state состояние аутентификации
     * @param password введенный пароль
     * @param userId идентификатор пользователя
     * @return ответ с результатом регистрации
     */
    private BotResponse handleRegistration(AuthState state, String password, String userId) {
        if (userManager.registerUser(state.username, password)) {
            userManager.authenticateUser(state.username, password, userId);
            synchronizeUserData(userId, state.username);
            return new BotResponse("""
                    ✅ Регистрация прошла успешно!
                    """);
        }
        return new BotResponse("Ошибка регистрации. Попробуйте снова.");
    }

    /**
     * Завершает процесс входа в аккаунт.
     *
     * @param state состояние аутентификации
     * @param password введенный пароль
     * @param userId идентификатор пользователя
     * @return ответ с результатом входа
     */
    private BotResponse handleIntegration(AuthState state, String password, String userId) {
        if (userManager.authenticateUser(state.username, password, userId)) {
            synchronizeUserData(userId, state.username);
            return new BotResponse("""
                    ✅ Вход выполнен успешно!
                    Данные синхронизированы.
                    
                    Добро пожаловать обратно!""");
        }
        return new BotResponse("Неверный пароль. Попробуйте снова.");
    }

    /**
     * Получает данные пользователя с учетом его авторизации.
     *
     * @param userId идентификатор пользователя
     * @return объект UserData пользователя
     */
    private UserData getUserData(String userId) {
        String username = userManager.getUsername(userId);
        String dataKey = username != null ? username : userId;

        if (!userDataMap.containsKey(dataKey)) {
            userDataMap.put(dataKey, new UserData());
        }
        return userDataMap.get(dataKey);
    }

    /**
     * Синхронизирует данные пользователя после успешной аутентификации.
     *
     * @param oldUserId старый идентификатор пользователя
     * @param newUsername новое имя пользователя
     */
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
}