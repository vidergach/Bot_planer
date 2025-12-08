package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Тесты для класса MessageHandler.
 */
public class MessageHandlerTests {
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
     * Очищает базу данных для изоляции тестов.
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
     * Регистрирует тестового пользователя в системе.
     *
     * @param userId идентификатор пользователя для регистрации
     */
    private void registerTestUser(String userId) {
        messageHandler.processUserInput("/registration", userId, PLATFORM_TYPE);
        messageHandler.processUserInput("test_user_" + userId, userId, PLATFORM_TYPE);
        messageHandler.processUserInput("test_password", userId, PLATFORM_TYPE);
    }

    /**
     * Тестирует добавление новой задачи.
     * Проверяет корректность добавления и отображения задачи в списке.
     */
    @Test
    void testAddTask() {
        String userId = "user1";
        registerTestUser(userId);

        BotResponse response = messageHandler.processUserInput("/add Полить цветы", userId, PLATFORM_TYPE);
        Assertions.assertEquals("Задача \"Полить цветы\" добавлена!", response.getMessage());

        BotResponse tasksResponse = messageHandler.processUserInput("/tasks", userId, PLATFORM_TYPE);
        Assertions.assertEquals("""
        📝 Ваши задачи:
        1. Полить цветы
        """, tasksResponse.getMessage());
    }

    /**
     * Тестирует добавление задачи без названия.
     * Проверяет сообщение от бота.
     */
    @Test
    void testAddEmptyTask() {
        String userId = "user2";
        registerTestUser(userId);

        BotResponse response = messageHandler.processUserInput("/add", userId, PLATFORM_TYPE);
        Assertions.assertEquals("""
                                Введите задачу для добавления:
                                Например: Купить молоко""", response.getMessage());
    }

    /**
     * Тестирует удаление задачи без названия.
     * Проверяет сообщение от бота.
     */
    @Test
    void testDeleteEmptyTask() {
        String userId = "user3";
        registerTestUser(userId);

        BotResponse response = messageHandler.processUserInput("/delete", userId, PLATFORM_TYPE);
        Assertions.assertEquals("""
        Введите название задачи для удаления:
        Например: Купить молоко""", response.getMessage());
    }

    /**
     * Тестирует команду выполнения без указания задачи.
     * Проверяет сообщение от бота.
     */
    @Test
    void testMarkEmptyTaskDone() {
        String userId = "user4";
        registerTestUser(userId);

        BotResponse response = messageHandler.processUserInput("/done", userId, PLATFORM_TYPE);
        Assertions.assertEquals("""
        Введите название задачи для отметки выполнения:
        Например: Купить молоко""", response.getMessage());
    }

    /**
     * Тестирует добавление дублирующихся задач.
     * Проверяет, что задача с одинаковым названием добавляется только один раз.
     */
    @Test
    void testAddExistingTask() {
        String userId = "user123";
        registerTestUser(userId);
        messageHandler.processUserInput("/add Полить цветы", "user123", PLATFORM_TYPE);
        messageHandler.processUserInput("/add Полить цветы", "user123", PLATFORM_TYPE);
        BotResponse tasks_response = messageHandler.processUserInput("/tasks", "user123", PLATFORM_TYPE);
        String tasksMessage = tasks_response.getMessage();
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
     * Тестирует пустой списка задач.
     * Проверяет сообщение о пустом списке.
     */
    @Test
    void testShowEmptyTasks() {
        String userId = "user6";
        registerTestUser(userId);

        BotResponse response = messageHandler.processUserInput("/tasks", userId, PLATFORM_TYPE);
        Assertions.assertEquals("📝 Список задач пуст!", response.getMessage());
    }

    /**
     * Тестирует список с несколькими задачами.
     * Проверяет вывод и нумерацию задач.
     */
    @Test
    void testShowTasks() {
        String userId = "user7";
        registerTestUser(userId);

        messageHandler.processUserInput("/add Задача 1", userId, PLATFORM_TYPE);
        messageHandler.processUserInput("/add Задача 2", userId, PLATFORM_TYPE);
        BotResponse tasksResponse = messageHandler.processUserInput("/tasks", userId, PLATFORM_TYPE);

        Assertions.assertEquals("""
        📝 Ваши задачи:
        1. Задача 1
        2. Задача 2
        """, tasksResponse.getMessage());
    }

    /**
     * Тестирует удаление существующей задачи.
     * Проверяет удаление и обновление списка задач.
     */
    @Test
    void testDeleteTask() {
        String userId = "user8";
        registerTestUser(userId);

        messageHandler.processUserInput("/add Удаляемая задача", userId, PLATFORM_TYPE);
        BotResponse response = messageHandler.processUserInput("/delete Удаляемая задача", userId,PLATFORM_TYPE);
        Assertions.assertEquals("🗑️ Задача \"Удаляемая задача\" удалена!", response.getMessage());

        BotResponse tasks_response = messageHandler.processUserInput("/tasks", userId, PLATFORM_TYPE);
        Assertions.assertEquals("📝 Список задач пуст!", tasks_response.getMessage());
    }

    /**
     * Тестирует отметку задачи как выполненной.
     * Проверяет перемещение задачи в список выполненных.
     */
    @Test
    void testMarkTaskDone() {
        String userId = "user9";
        registerTestUser(userId);

        messageHandler.processUserInput("/add Полить цветы", userId, PLATFORM_TYPE);
        BotResponse response = messageHandler.processUserInput("/done Полить цветы", userId, PLATFORM_TYPE);
        Assertions.assertEquals("✅ Задача \"Полить цветы\" выполнена!", response.getMessage());

        BotResponse dTaskResponse = messageHandler.processUserInput("/dTask", userId, PLATFORM_TYPE);
        Assertions.assertEquals("✅ Выполненные задачи:\n1. Полить цветы\n", dTaskResponse.getMessage());
    }

    /**
     * Тестирует пустой список выполненных задач.
     * Проверяет сообщение о пустом списке.
     */
    @Test
    void testShowEmptyCompletedTasks() {
        String userId = "user10";
        registerTestUser(userId);

        BotResponse response = messageHandler.processUserInput("/dTask", userId, PLATFORM_TYPE);
        Assertions.assertEquals("✅ Список выполненных задач пуст!", response.getMessage());
    }

    /**
     * Тестирует список выполненных задач.
     * Проверяет вывод выполненных задач.
     */
    @Test
    void testShowCompletedTasks() {
        String userId = "user11";
        registerTestUser(userId);

        messageHandler.processUserInput("/add Полить цветы", userId, PLATFORM_TYPE);
        messageHandler.processUserInput("/done Полить цветы", userId, PLATFORM_TYPE);
        BotResponse dTaskResponse = messageHandler.processUserInput("/dTask", userId, PLATFORM_TYPE);

        Assertions.assertEquals("✅ Выполненные задачи:\n1. Полить цветы\n", dTaskResponse.getMessage());
    }

    /**
     * Тестирует экспорт без имени файла.
     * Проверяет запрос имени файла от бота.
     */
    @Test
    void testExportWithoutFilename() {
        String userId = "user12";
        registerTestUser(userId);

        BotResponse exportResponse = messageHandler.processUserInput("/export", userId, PLATFORM_TYPE);
        Assertions.assertEquals("Напишите имя файла для экспорта\nНапример: 'list'", exportResponse.getMessage());
    }

    /**
     * Тестирует импорт задач.
     * Проверяет запрос файла от бота.
     */
    @Test
    void testImportCommand_FileRequest() {
        String userId = "user13";
        registerTestUser(userId);

        BotResponse response = messageHandler.processUserInput("/import", userId, PLATFORM_TYPE);
        Assertions.assertEquals("Для импорта отправьте JSON файл с задачами", response.getMessage());
    }

    /**
     * Тестирует неизвестную команду.
     * Проверяет сообщение об ошибке и о помощи.
     */
    @Test
    void testUnknownCommand() {
        String userId = "user14";
        registerTestUser(userId);

        BotResponse response = messageHandler.processUserInput("/unknown", userId, PLATFORM_TYPE);
        Assertions.assertEquals("""
                        Неизвестная команда.
                        Введите /help для просмотра доступных команд.
                        """, response.getMessage());
    }

    /**
     * Тестирует процесс регистрации нового пользователя.
     * Проверяет все шаги регистрации и работу с задачами.
     */
    @Test
    void testRegistrationProcess() {
        String newUserId = "user15";

        BotResponse step1 = messageHandler.processUserInput("/registration", newUserId, PLATFORM_TYPE);
        Assertions.assertEquals("""
                📝 Регистрация нового пользователя
                Введите логин:
                """, step1.getMessage());

        BotResponse step2 = messageHandler.processUserInput("new_test_user", newUserId, PLATFORM_TYPE);
        Assertions.assertEquals("✅Отлично! Теперь введите пароль:", step2.getMessage());

        BotResponse step3 = messageHandler.processUserInput("password123", newUserId, PLATFORM_TYPE);
        String expectedStep3 = """
            ✅ Регистрация завершена успешно!
            Добро пожаловать, new_test_user!
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
            /exit - выйти из аккаунта
            /help - помощь
            """;
        Assertions.assertEquals(expectedStep3, step3.getMessage());

        BotResponse response = messageHandler.processUserInput("/add Новая задача", newUserId, PLATFORM_TYPE);
        Assertions.assertEquals("Задача \"Новая задача\" добавлена!", response.getMessage());
    }

    /**
     * Тестирует неавторизованного пользователя.
     * Проверяет запрос авторизации при попытке использования бота.
     */
    @Test
    void testUnauthenticatedUser() {
        String newUserId = "user16";
        BotResponse response = messageHandler.processUserInput("/add Задача", newUserId, PLATFORM_TYPE);
        String expectedMessage =   """
            Добро пожаловать в планировщик задач! \uD83D\uDC31 📝

            Для начала работы необходимо авторизоваться:
            /registration - Регистрация
            /login - Войти в аккаунт
            /exit - Выйти из аккаунта

            После авторизации вы сможете использовать все функции планировщика!
            """;
            Assertions.assertEquals(expectedMessage, response.getMessage());
        }

        /**
         * Тестирует предотвращение регистрации с дублирующимся логином.
         * Проверяет сообщение об ошибке при повторной регистрации.
        */
        @Test
        void testDuplicateRegistration() {
            String firstUserId = "user17";
            String secondUserId = "user18";

            messageHandler.processUserInput("/registration", firstUserId, PLATFORM_TYPE);
            messageHandler.processUserInput("user", firstUserId, PLATFORM_TYPE);
            messageHandler.processUserInput("password123", firstUserId, PLATFORM_TYPE);

            messageHandler.processUserInput("/registration", secondUserId, PLATFORM_TYPE);
            BotResponse response = messageHandler.processUserInput("user", secondUserId, PLATFORM_TYPE);

            Assertions.assertEquals("""
                    Пользователь с таким логином уже существует.
                    Используйте другой логин или войдите с помощью /integration.""", response.getMessage());
        }

        /**
         * Тестирует вход с несуществующим логином.
         * Проверяет сообщение об ошибке.
         */
        @Test
        void testIntegrationWithWrongUsername() {
            String userId = "user19";
            messageHandler.processUserInput("/login", userId, PLATFORM_TYPE);
            BotResponse step2 = messageHandler.processUserInput("nonexistent_user", userId, PLATFORM_TYPE);

            Assertions.assertEquals("""
                    Пользователь 'nonexistent_user' не найден.
                    Проверьте логин или зарегистрируйтесь с помощью /registration.
                    """, step2.getMessage());
        }

        /**
        * Тестирует вход с неверным паролем.
         * Проверяет сообщение об ошибке аутентификации.
        */
        @Test
        void testIntegrationWithWrongPassword() {
            String regUserId = "user20";
            String loginUserId = "user21";

            messageHandler.processUserInput("/registration", regUserId, PLATFORM_TYPE);
            messageHandler.processUserInput("test_login_user", regUserId, PLATFORM_TYPE);
            messageHandler.processUserInput("correct_password", regUserId, PLATFORM_TYPE);

            messageHandler.processUserInput("/login", loginUserId, PLATFORM_TYPE);
            messageHandler.processUserInput("test_login_user", loginUserId, PLATFORM_TYPE);
            BotResponse response = messageHandler.processUserInput("wrong_password", loginUserId, PLATFORM_TYPE);

            Assertions.assertEquals("Неверный пароль. Попробуйте снова.", response.getMessage());
        }
    }
