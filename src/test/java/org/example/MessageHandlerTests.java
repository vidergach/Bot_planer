package org.example;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;

/**
 * Тесты для класса MessageHandler.
 * Проверяет функциональность обработки команд бота.
 *
 * @see MessageHandler
 */
public class MessageHandlerTests {
    private MessageHandler messageHandler;

    /**
     * Инициализация тестового окружения перед каждым тестом.
     */
    @BeforeEach
    void setUp() {
        messageHandler = new MessageHandler();
        registerTestUser("user123");
        registerTestUser("testUserExport");
    }

    /**
     * Вспомогательный метод для регистрации тестового пользователя
     */
    private void registerTestUser(String userId) {
        messageHandler.processUserInput("/registration", userId);
        messageHandler.processUserInput("testuser_" + userId, userId);
        messageHandler.processUserInput("testpass", userId);
    }

    /**
     * Тест добавления новой задачи.
     */
    @Test
    void testAddTask() {
        MessageHandler.BotResponse response = messageHandler.processUserInput("/add Полить цветы", "user123");
        Assertions.assertEquals("Задача \"Полить цветы\" добавлена!", response.getMessage());
        MessageHandler.BotResponse tasks_response = messageHandler.processUserInput("/tasks", "user123");
        Assertions.assertEquals("""
        📝 Ваши задачи:
        1. Полить цветы
        """, tasks_response.getMessage());
    }

    /**
     * Тест добавления пустой задачи
     */
    @Test
    void testAddEmptyTask() {
        MessageHandler.BotResponse response = messageHandler.processUserInput("/add", "user123");
        Assertions.assertEquals("Укажите задачу после /add\nНапример: /add Купить молоко", response.getMessage());
    }

    /**
     * Тест удаления пустой задачи
     */
    @Test
    void testDeleteEmptyTask() {
        MessageHandler.BotResponse response = messageHandler.processUserInput("/delete", "user123");
        Assertions.assertEquals("Укажите задачу после /delete\nНапример: /delete Купить молоко", response.getMessage());
    }

    /**
     * Тест отметки как выполнена пустой задачи
     */
    @Test
    void testMarkEmptyTaskDone() {
        MessageHandler.BotResponse response = messageHandler.processUserInput("/done", "user123");
        Assertions.assertEquals("Укажите задачу после /done\nНапример: /done Купить молоко", response.getMessage());
    }

    /**
     * Тест добавления уже существующей задачи.
     */
    @Test
    void testAddExistingTask() {
        messageHandler.processUserInput("/add Полить цветы", "user123");
        MessageHandler.BotResponse response = messageHandler.processUserInput("/add Полить цветы", "user123");
        Assertions.assertEquals("Задача \"Полить цветы\" уже есть в списке!", response.getMessage());

        MessageHandler.BotResponse tasks_response = messageHandler.processUserInput("/tasks", "user123");
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
     * Тест отображения пустого списка задач.
     */
    @Test
    void testShowEmptyTasks() {
        MessageHandler.BotResponse response = messageHandler.processUserInput("/tasks", "user123");
        Assertions.assertEquals("📝 Список задач пуст!", response.getMessage());
    }

    /**
     * Тест отображения списка задач.
     */
    @Test
    void testShowTasks() {
        messageHandler.processUserInput("/add Задача 1", "user123");
        messageHandler.processUserInput("/add Задача 2", "user123");
        MessageHandler.BotResponse tasks_response = messageHandler.processUserInput("/tasks", "user123");

        Assertions.assertEquals("""
        📝 Ваши задачи:
        1. Задача 1
        2. Задача 2
        """, tasks_response.getMessage());
    }

    /**
     * Тест удаления задачи.
     */
    @Test
    void testDeleteTask() {
        messageHandler.processUserInput("/add Удаляемая задача", "user123");
        MessageHandler.BotResponse response = messageHandler.processUserInput("/delete Удаляемая задача", "user123");
        Assertions.assertEquals("🗑️ Задача \"Удаляемая задача\" удалена!", response.getMessage());

        MessageHandler.BotResponse tasks_response = messageHandler.processUserInput("/tasks", "user123");
        Assertions.assertEquals("📝 Список задач пуст!", tasks_response.getMessage());
    }

    /**
     * Тест отметки задачи как выполненной.
     */
    @Test
    void testMarkTaskDone() {
        messageHandler.processUserInput("/add Полить цветы", "user123");
        MessageHandler.BotResponse response = messageHandler.processUserInput("/done Полить цветы", "user123");
        Assertions.assertEquals("✅ Задача \"Полить цветы\" выполнена!", response.getMessage());

        MessageHandler.BotResponse dTask_response = messageHandler.processUserInput("/dTask", "user123");
        Assertions.assertEquals("""
            ✅ Выполненные задачи:
            1. Полить цветы
            """, dTask_response.getMessage());
    }

    /**
     * Тест отображения пустого списка выполненных задач.
     */
    @Test
    void testShowEmptyCompletedTasks() {
        MessageHandler.BotResponse response = messageHandler.processUserInput("/dTask", "user123");
        Assertions.assertEquals("✅ Список выполненных задач пуст!", response.getMessage());
    }

    /**
     * Тест отображения списка выполненных задач.
     */
    @Test
    void testShowCompletedTasks() {
        messageHandler.processUserInput("/add Полить цветы", "user123");
        messageHandler.processUserInput("/done Полить цветы", "user123");
        MessageHandler.BotResponse dTask_response = messageHandler.processUserInput("/dTask", "user123");
        Assertions.assertEquals("""
            ✅ Выполненные задачи:
            1. Полить цветы
            """, dTask_response.getMessage());
    }

    /**
     * Тест экспорта без имени файла.
     */
    @Test
    void testExportWithoutFilename() {
        MessageHandler.BotResponse export_response = messageHandler.processUserInput("/export", "user123");
        Assertions.assertEquals("Напишите имя файла после /export", export_response.getMessage());
    }

    /**
     * Тест экспорта файла с задачами.
     */
    @Test
    void testExportWithTasks() {
        String exportUserId = "testUserExport";
        String importUserId = "testUserImport";

        messageHandler.processUserInput("/registration", exportUserId);
        messageHandler.processUserInput("testUserExport", exportUserId);
        messageHandler.processUserInput("password", exportUserId);

        messageHandler.processUserInput("/add Задача 1", exportUserId);
        messageHandler.processUserInput("/add Задача 2", exportUserId);
        messageHandler.processUserInput("/done Задача 1", exportUserId);

        MessageHandler.BotResponse exportResponse = messageHandler.processUserInput("/export test_export.json", exportUserId);

        Assertions.assertNotNull(exportResponse.getFile(), "Файл не должен быть null");
        Assertions.assertEquals("test_export.json", exportResponse.getFileName());
        File exportedFile = exportResponse.getFile();

        try {
            messageHandler.processUserInput("/registration", importUserId);
            messageHandler.processUserInput("testUserImport", importUserId);
            messageHandler.processUserInput("password", importUserId);
            MessageHandler.BotResponse importResponse = messageHandler.processImport(new FileInputStream(exportedFile), importUserId);
            MessageHandler.BotResponse tasksResponse = messageHandler.processUserInput("/tasks", importUserId);
            MessageHandler.BotResponse dTaskResponse = messageHandler.processUserInput("/dTask", importUserId);

            Assertions.assertEquals("""
            📝 Ваши задачи:
            1. Задача 2
            """, tasksResponse.getMessage());

            Assertions.assertEquals("""
            ✅ Выполненные задачи:
            1. Задача 1
            """, dTaskResponse.getMessage());
        } catch (Exception e) {
            Assertions.fail("Ошибка при проверке экспортированного файла: " + e.getMessage());
        } finally {
            if (exportedFile.exists()) {
                exportedFile.delete();
            }
        }
    }

    /**
     * Тест проверки сообщения запрашивающего файл
     */
    @Test
    void testImportCommand_FileRequest() {
        MessageHandler.BotResponse response = messageHandler.processUserInput("/import", "user123");
        Assertions.assertEquals("Для импорта отправьте JSON файл с задачами", response.getMessage());
    }

    /**
     * Тест импорта файла с задачами.
     */
    @Test
    void testImportCommand_WithFile() throws Exception {
        File testFile = File.createTempFile("test_import", ".json");
        String jsonContent = """
        {
            "current_tasks": ["Задача 1", "Задача 2"],
            "completed_tasks": ["Выполненная задача"]
        }
        """;
        Files.write(testFile.toPath(), jsonContent.getBytes());
        try (FileInputStream inputStream = new FileInputStream(testFile)) {
            MessageHandler.BotResponse import_response = messageHandler.processImport(inputStream, "user123");
            Assertions.assertEquals("""
                    Задачи успешно добавлены,
                    можете проверить списки с помощью команд /tasks и /dTask
                    """, import_response.getMessage());
        }

        MessageHandler.BotResponse tasks_response = messageHandler.processUserInput("/tasks", "user123");
        Assertions.assertEquals("""
            📝 Ваши задачи:
            1. Задача 1
            2. Задача 2
            """, tasks_response.getMessage());

        MessageHandler.BotResponse dTask_response = messageHandler.processUserInput("/dTask", "user123");
        Assertions.assertEquals("""
            ✅ Выполненные задачи:
            1. Выполненная задача
            """, dTask_response.getMessage());
    }
    
    /**
     * Тест неизвестной команды
     */
    @Test
    void testUnknownCommand() {
        MessageHandler.BotResponse response = messageHandler.processUserInput("/unknown", "user123");
        Assertions.assertNotNull(response.getMessage());
        Assertions.assertEquals(("""
                        Неизвестная команда.
                        Введите /help для просмотра доступных команд."""), response.getMessage());
    }

    /**
     * Тест процесса регистрации
     */
    @Test
    void testRegistrationProcess() {
        String newUserId = "newUser";

        MessageHandler.BotResponse step1 = messageHandler.processUserInput("/registration", newUserId);
        Assertions.assertEquals(("""
                📝 Регистрация нового пользователя
                Введите логин:
                """),step1.getMessage());

        MessageHandler.BotResponse step2 = messageHandler.processUserInput("new_test_user", newUserId);
        Assertions.assertEquals("✅Отлично! Теперь введите пароль:",step2.getMessage());

        MessageHandler.BotResponse step3 = messageHandler.processUserInput("password123", newUserId);
        Assertions.assertEquals(("""
                    ✅ Регистрация прошла успешно!
                    """),step3.getMessage());

        MessageHandler.BotResponse response = messageHandler.processUserInput("/add Новая задача", newUserId);
        Assertions.assertEquals("Задача \"Новая задача\" добавлена!", response.getMessage());
    }

    /**
     * Тест для неавторизованного пользователя
     */
    @Test
    void testUnauthenticatedUser() {
        String newUserId = "unauthenticatedUser";
        MessageHandler.BotResponse response = messageHandler.processUserInput("/add Задача", newUserId);
        Assertions.assertEquals(""" 
            Добро пожаловать в планировщик задач! \uD83D\uDC31 📝
            
            ⚠️ Для начала работы необходимо авторизоваться:
            /registration - зарегистрироваться
            /integration - войти в существующий аккаунт
            
            После авторизации вы сможете использовать все функции планировщика!
            """, response.getMessage());
    }

    /**
     * Тест попытка регистрации пользователя с уже существующим логином
     */
    @Test
    void testDuplicateRegistration() {
        String firstUserId = "firstUser";
        String secondUserId = "secondUser";

        messageHandler.processUserInput("/registration", firstUserId);
        messageHandler.processUserInput("user", firstUserId);
        messageHandler.processUserInput("password123", firstUserId);

        messageHandler.processUserInput("/registration", secondUserId);
        MessageHandler.BotResponse response = messageHandler.processUserInput("user", secondUserId);

        Assertions.assertEquals( """
                    Пользователь с таким логином уже существует.
                    Используйте другой логин или войдите с помощью /integration.""", response.getMessage());
    }

    /**
     * Тест проверки входа с несуществующим логином
     */
    @Test
    void testIntegrationWithWrongUsername() {
        String userId = "wrongUser";
        MessageHandler.BotResponse step1 = messageHandler.processUserInput("/integration", userId);
        MessageHandler.BotResponse step2 = messageHandler.processUserInput("nonexistent_user", userId);

        Assertions.assertEquals("""
                    Пользователь 'nonexistent_user' не найден.
                    Проверьте логин или зарегистрируйтесь с помощью /registration.""", step2.getMessage());
    }

    /**
     * Тест проверки вход с неверным паролем
     */
    @Test
    void testIntegrationWithWrongPassword() {
        String regUserId = "regUser";
        String loginUserId = "loginUser";

        messageHandler.processUserInput("/registration", regUserId);
        messageHandler.processUserInput("test_login_user", regUserId);
        messageHandler.processUserInput("correct_password", regUserId);

        messageHandler.processUserInput("/integration", loginUserId);
        messageHandler.processUserInput("test_login_user", loginUserId);
        MessageHandler.BotResponse response = messageHandler.processUserInput("wrong_password", loginUserId);

        Assertions.assertEquals("Неверный пароль. Попробуйте снова.", response.getMessage());
    }
}
