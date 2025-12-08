package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Тесты для тестирования логики с использованием постепенного ввода
 * и кнопок
 */
public class MessageHandlerTestsKeyboard {
    private MessageHandler messageHandler;
    private final String PLATFORM_TYPE = "test";
    private final String TEST_DB_URL = "jdbc:sqlite:test_tasks.db";

    @BeforeEach
    void setUp() {
        messageHandler = new MessageHandler(new TestDatabaseService());
        clearDatabase();
    }

    @AfterEach
    void tearDown() {
        clearDatabase();
    }

    private class TestDatabaseService extends DatabaseService {
        public TestDatabaseService() {
            super(TEST_DB_URL);
        }
    }

    /**
     * Очищает базу данных для изоляции тестов
     */
    private void clearDatabase() {
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL);
             Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = OFF");
            stmt.execute("DELETE FROM user_sessions");
            stmt.execute("DELETE FROM completed_tasks");
            stmt.execute("DELETE FROM user_tasks");
            stmt.execute("DELETE FROM users");
            stmt.execute("PRAGMA foreign_keys = ON");

        } catch (Exception e) {
            System.err.println("Ошибка при очистке базы данных: " + e.getMessage());
        }
    }

    /**
     * Регистрация пользователя через кнопки
     */
    private void registerTestUser(String userId) {
        messageHandler.processUserInput("📝 Регистрация", userId, PLATFORM_TYPE);
        messageHandler.processUserInput("test_user_" + userId, userId, PLATFORM_TYPE);
        messageHandler.processUserInput("test_password", userId, PLATFORM_TYPE);
    }

    /**
     * Тестирует добавление задачи через постепенный ввод
     * "➕ Добавить задачу".
     */
    @Test
    void testAddTask() {
        String userId = "user1";
        registerTestUser(userId);

        BotResponse step1 = messageHandler.processUserInput("➕ Добавить задачу", userId, PLATFORM_TYPE);
        Assertions.assertEquals("""
        Введите задачу для добавления:
        Например: Купить молоко""", step1.getMessage());

        BotResponse step2 = messageHandler.processUserInput("Полить цветы", userId, PLATFORM_TYPE);
        Assertions.assertEquals("Задача \"Полить цветы\" добавлена!", step2.getMessage());

        BotResponse step3 = messageHandler.processUserInput("📝 Показать список задач", userId, PLATFORM_TYPE);
        Assertions.assertEquals("""
        📝 Ваши задачи:
        1. Полить цветы
        """, step3.getMessage());
    }

    /**
     * Тестирует отметку задачи как выполненной через постепенный ввод
     * "✔ Выполнено"
     */
    @Test
    void testMarkTaskDone() {
        String userId = "user2";
        registerTestUser(userId);

        messageHandler.processUserInput("➕ Добавить задачу", userId, PLATFORM_TYPE);
        messageHandler.processUserInput("Полить цветы", userId, PLATFORM_TYPE);

        BotResponse step1 = messageHandler.processUserInput("✔ Выполнено", userId, PLATFORM_TYPE);
        Assertions.assertEquals("""
        Введите название задачи для отметки выполнения:
        Например: Купить молоко""", step1.getMessage());

        BotResponse step2 = messageHandler.processUserInput("Полить цветы", userId, PLATFORM_TYPE);
        Assertions.assertEquals("✅ Задача \"Полить цветы\" выполнена!", step2.getMessage());

        BotResponse step3 = messageHandler.processUserInput("✅ Список выполненных задач", userId, PLATFORM_TYPE);
        Assertions.assertEquals("""
                ✅ Выполненные задачи:
                1. Полить цветы
                """, step3.getMessage());
    }

    /**
     * Тестирует удаление задачи через постепенный ввод
     * "❌ Удалить"
     */
    @Test
    void testDeleteTask() {
        String userId = "user3";
        registerTestUser(userId);

        messageHandler.processUserInput("➕ Добавить задачу", userId, PLATFORM_TYPE);
        messageHandler.processUserInput("Полить цветы", userId, PLATFORM_TYPE);

        BotResponse step1 = messageHandler.processUserInput("❌ Удалить", userId, PLATFORM_TYPE);
        Assertions.assertEquals("""
        Введите название задачи для удаления:
        Например: Купить молоко""", step1.getMessage());

        BotResponse step2 = messageHandler.processUserInput("Полить цветы", userId, PLATFORM_TYPE);
        Assertions.assertEquals("🗑️ Задача \"Полить цветы\" удалена!", step2.getMessage());

        BotResponse step3 = messageHandler.processUserInput("📝 Показать список задач", userId, PLATFORM_TYPE);
        Assertions.assertEquals("📝 Список задач пуст!", step3.getMessage());
    }

    /**
     * Тестирует вход в аккаунт через кнопку "Войти в аккаунт"
     */
    @Test
    void testLoginProcess() {
        String regUserId = "user4_reg";
        String loginUserId = "user4_login";

        registerTestUser(regUserId);
        BotResponse step1 = messageHandler.processUserInput("Войти в аккаунт", loginUserId, PLATFORM_TYPE);
        Assertions.assertEquals("""
                🔑 Вход в аккаунт
                Введите логин:
                """, step1.getMessage());

        BotResponse step2 = messageHandler.processUserInput("test_user_" + regUserId, loginUserId, PLATFORM_TYPE);
        Assertions.assertEquals("✅Отлично! Теперь введите пароль:", step2.getMessage());

        BotResponse step3 = messageHandler.processUserInput("test_password", loginUserId, PLATFORM_TYPE);
        String expectedStep3 = """
                ✅ Вход выполнен успешно!
                Добро пожаловать обратно, test_user_user4_reg
                
                Добро пожаловать в планировщик задач! 🐱 📝
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
                /exit - выйти из аккаунта
                /help - помощь
                """;
        Assertions.assertEquals(expectedStep3, step3.getMessage());
    }

    /**
     * Тестирует выход из аккаунта через кнопку "Выйти из аккаунта"
     */
    @Test
    void testLogout() {
        String userId = "user5";
        registerTestUser(userId);

        BotResponse step1 = messageHandler.processUserInput("Выйти из аккаунта", userId, PLATFORM_TYPE);
        Assertions.assertEquals("""
                ✅ Вы успешно вышли из аккаунта.
                
                Для продолжения работы:
                /registration - зарегистрироваться
                /login - войти в существующий аккаунт
                """, step1.getMessage());

        BotResponse step2 = messageHandler.processUserInput("➕ Добавить задачу", userId, PLATFORM_TYPE);
        String expectedMessage = """
            Добро пожаловать в планировщик задач! 🐱 📝

            Для начала работы необходимо авторизоваться:
            /registration - Регистрация
            /login - Войти в аккаунт
            /exit - Выйти из аккаунта

            После авторизации вы сможете использовать все функции планировщика!
            """;
        Assertions.assertEquals(expectedMessage, step2.getMessage());
    }

    /**
     * Тестирует попытку использования бота без авторизации
     */
    @Test
    void testUnauthenticatedAccess() {
        String userId = "user8";

        BotResponse response = messageHandler.processUserInput("➕ Добавить задачу", userId, PLATFORM_TYPE);
        String expectedMessage = """
            Добро пожаловать в планировщик задач! 🐱 📝

            Для начала работы необходимо авторизоваться:
            /registration - Регистрация
            /login - Войти в аккаунт
            /exit - Выйти из аккаунта

            После авторизации вы сможете использовать все функции планировщика!
            """;
        Assertions.assertEquals(expectedMessage, response.getMessage());
    }

    /**
     * Тестирует добавление пустой задачи через кнопку
     */
    @Test
    void testAddEmptyTask() {
        String userId = "user9";
        registerTestUser(userId);

        BotResponse step1 = messageHandler.processUserInput("➕ Добавить задачу", userId, PLATFORM_TYPE);
        Assertions.assertEquals("""
        Введите задачу для добавления:
        Например: Купить молоко""", step1.getMessage());

        BotResponse step2 = messageHandler.processUserInput("", userId, PLATFORM_TYPE);
        Assertions.assertEquals("""
        Введите задачу для добавления:
        Например: Купить молоко""", step2.getMessage());
    }

    /**
     * Тестирует добавление дублирующихся задач через кнопки
     */
    @Test
    void testAddExistingTaskStepByStep() {
        String userId = "user10";
        registerTestUser(userId);

        messageHandler.processUserInput("➕ Добавить задачу", userId, PLATFORM_TYPE);
        messageHandler.processUserInput("Полить цветы", userId, PLATFORM_TYPE);

        messageHandler.processUserInput("➕ Добавить задачу", userId, PLATFORM_TYPE);
        BotResponse response = messageHandler.processUserInput("Полить цветы", userId, PLATFORM_TYPE);
        Assertions.assertEquals("Задача \"" + "Полить цветы" + "\" уже существует!", response.getMessage());

        BotResponse tasksResponse = messageHandler.processUserInput("📝 Показать список задач", userId, PLATFORM_TYPE);
        String tasksMessage = tasksResponse.getMessage();
        int count = 0;
        String[] lines = tasksMessage.split("\n");
        for (String line: lines){
            if (line.contains("Полить цветы")){
                count++;
            }
        }
        Assertions.assertEquals(1, count);
    }

    /**
     * Тестирует пустой список задач через кнопки
     */
    @Test
    void testShowEmptyTask() {
        String userId = "user11";
        registerTestUser(userId);

        BotResponse response = messageHandler.processUserInput("📝 Показать список задач", userId, PLATFORM_TYPE);
        Assertions.assertEquals("📝 Список задач пуст!", response.getMessage());
    }

    /**
     * Тестирует пустой список выполненных задач через кнопки
     */
    @Test
    void testShowEmptyCompletedTask() {
        String userId = "user12";
        registerTestUser(userId);

        BotResponse response = messageHandler.processUserInput("✅ Список выполненных задач", userId, PLATFORM_TYPE);
        Assertions.assertEquals("✅ Список выполненных задач пуст!", response.getMessage());
    }

    /**
     * Тестирует неизвестную команду
     */
    @Test
    void testUnknownCommandStepByStep() {
        String userId = "user15";
        registerTestUser(userId);

        BotResponse response = messageHandler.processUserInput("/unknown", userId, PLATFORM_TYPE);
        Assertions.assertEquals("""
                        Неизвестная команда.
                        Введите /help для просмотра доступных команд.
                        """, response.getMessage());
    }

    /**
     * Тестирует вход с неверным паролем через кнопки
     */
    @Test
    void testLoginWithWrongPasswordStepByStep() {
        String regUserId = "user16_reg";
        String loginUserId = "user16_login";
        registerTestUser(regUserId);

        messageHandler.processUserInput("Войти в аккаунт", loginUserId, PLATFORM_TYPE);
        messageHandler.processUserInput("test_user_" + regUserId, loginUserId, PLATFORM_TYPE);
        BotResponse response = messageHandler.processUserInput("wrong_password", loginUserId, PLATFORM_TYPE);

        Assertions.assertEquals("Неверный пароль. Попробуйте снова.", response.getMessage());
    }

    /**
     * Тестирует попытку входа с несуществующим логином через кнопки
     */
    @Test
    void testLoginWithNonExistentUserStepByStep() {
        String userId = "user17";
        messageHandler.processUserInput("Войти в аккаунт", userId, PLATFORM_TYPE);
        BotResponse response = messageHandler.processUserInput("nonexistent_user", userId, PLATFORM_TYPE);

        Assertions.assertEquals("""
                    Пользователь 'nonexistent_user' не найден.
                    Проверьте логин или зарегистрируйтесь с помощью /registration.
                    """, response.getMessage());
    }

    /**
     * Тестирует дублирующую регистрацию через кнопки
     */
    @Test
    void testDuplicateRegistrationStepByStep() {
        String firstUserId = "user18_first";
        String secondUserId = "user18_second";
        registerTestUser(firstUserId);
        messageHandler.processUserInput("📝 Регистрация", secondUserId, PLATFORM_TYPE);
        BotResponse response = messageHandler.processUserInput("test_user_" + firstUserId, secondUserId, PLATFORM_TYPE);

        Assertions.assertEquals("""
                    Пользователь с таким логином уже существует.
                    Используйте другой логин или войдите с помощью /integration.""", response.getMessage());
    }
}