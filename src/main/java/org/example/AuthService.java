package org.example;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для управления аутентификацией пользователей.
 */
public class AuthService {
    private final DatabaseService databaseService;
    private final Map<String, AuthState> authStates = new ConcurrentHashMap<>();

    private final String WELCOME_MESSAGE = """
            Добро пожаловать в планировщик задач! \uD83D\uDC31 📝

            Для начала работы необходимо авторизоваться:
            /registration - Регистрация
            /login - Войти в аккаунт
            /exit - Выйти из аккаунта

            После авторизации вы сможете использовать все функции планировщика!
            """;

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
            /add_subtask - добавить подзадачу
            /delete_subtask - удалить подзадачу
            /edit_subtask - изменить подзадачу
            /finish_subtask - окончить расширение задачи
            """;

    /**
     * Конструктор сервиса аутентификации.
     */
    public AuthService(DatabaseService databaseService) {
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
     * Начинает процесс регистрации пользователя.
     */
    public BotResponse handleRegistration(String userId, String platformType) {
        authStates.put(userId, new AuthState("registration", platformType));
        return new BotResponse("""
        📝 Регистрация нового пользователя
        Введите логин:""");
    }

    /**
     * Начинает процесс входа пользователя.
     */
    public BotResponse handleLogin(String userId, String platformType) {
        authStates.put(userId, new AuthState("integration", platformType));
        return new BotResponse("""
        🔑 Вход в аккаунт
        Введите логин:""");
    }

    /**
     * Обрабатывает шаг аутентификации.
     */
    public BotResponse handleAuthStep(String userId, String userInput) {
        AuthState state = authStates.get(userId);
        if (state == null) {
            return new BotResponse("Ошибка аутентификации. Попробуйте снова.");
        }

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
     * Обрабатывает шаг ввода логина при аутентификации.
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
            throw new RuntimeException("Ошибка проверки пользователя: " + e.getMessage());
        }
    }

    /**
     * Обрабатывает шаг ввода пароля при аутентификации.
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
            throw new RuntimeException("Ошибка при авторизации: " + e.getMessage());
        }
    }

    /**
     * Проверяет, находится ли пользователь в процессе аутентификации.
     */
    public boolean isUserInAuthProcess(String userId) {
        return authStates.containsKey(userId);
    }

    /**
     * Обрабатывает выход пользователя из аккаунта.
     */
    public BotResponse handleExit(String userId, String platformType) {
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
            throw new RuntimeException("Ошибка при выходе из аккаунта: " + e.getMessage());
        }
    }

    /**
     * Проверяет, аутентифицирован ли пользователь.
     */
    public boolean isUserAuthenticated(String userId, String platformType) {
        try {
            return databaseService.getUsername(platformType, userId) != null;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка проверки авторизации: " + e.getMessage());
        }
    }

    /**
     * Возвращает сообщение для неавторизованных пользователей.
     */
    public String getWelcomeMessage() {
        return WELCOME_MESSAGE;
    }
}