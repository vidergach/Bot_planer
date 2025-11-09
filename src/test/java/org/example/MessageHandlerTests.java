package org.example;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.List;

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
        messageHandler.processUserInput("testpass", userId); // пароль
    }

    /**
     * Тест добавления новой задачи.
     */
    @Test
    void testAddTask() {
        MessageHandler.BotResponse response = messageHandler.processUserInput("/add Полить цветы", "user123");
        Assertions.assertEquals("Задача \"Полить цветы\" добавлена!", response.getMessage());
    }

    /**
     * Тест добавления уже существующей задачи.
     */
    @Test
    void testAddExistingTask() {
        messageHandler.processUserInput("/add Полить цветы", "user123");
        MessageHandler.BotResponse response = messageHandler.processUserInput("/add Полить цветы", "user123");
        Assertions.assertEquals("Задача \"Полить цветы\" уже есть в списке!", response.getMessage());
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
        MessageHandler.BotResponse response = messageHandler.processUserInput("/tasks", "user123");
        // Используем contains вместо точного сравнения из-за возможных различий в форматировании
        Assertions.assertTrue(response.getMessage().contains("📝 Ваши задачи:"));
        Assertions.assertTrue(response.getMessage().contains("1. Задача 1"));
        Assertions.assertTrue(response.getMessage().contains("2. Задача 2"));
    }

    /**
     * Тест удаления задачи.
     */
    @Test
    void testDeleteTask() {
        messageHandler.processUserInput("/add Удаляемая задача", "user123");
        MessageHandler.BotResponse response = messageHandler.processUserInput("/delete Удаляемая задача", "user123");
        Assertions.assertEquals("🗑️ Задача \"Удаляемая задача\" удалена!", response.getMessage());
    }

    /**
     * Тест отметки задачи как выполненной.
     */
    @Test
    void testMarkTaskDone() {
        messageHandler.processUserInput("/add Полить цветы", "user123");
        MessageHandler.BotResponse response = messageHandler.processUserInput("/done Полить цветы", "user123");
        Assertions.assertEquals("✅ Задача \"Полить цветы\" выполнена!", response.getMessage());
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
        MessageHandler.BotResponse response = messageHandler.processUserInput("/dTask", "user123");
        Assertions.assertTrue(response.getMessage().contains("✅ Выполненные задачи:"));
        Assertions.assertTrue(response.getMessage().contains("1. Полить цветы"));
    }


    /**
     * Тест экспорта без имени файла.
     */
    @Test
    void testExportWithoutFilename() {
        MessageHandler.BotResponse response = messageHandler.processUserInput("/export", "user123");
        Assertions.assertEquals("""
            Напишите имя файла для экспорта
            Например: 'list'""", response.getMessage());
    }

    /**
     * Тест экспорта файла с задачами.
     */
    @Test
    void testExportWithTasks() {
        String userId = "testUserExport";

        messageHandler.processUserInput("/add Задача 1", userId);
        messageHandler.processUserInput("/add Задача 2", userId);
        messageHandler.processUserInput("/done Задача 1", userId);

        MessageHandler.BotResponse response = messageHandler.processUserInput("/export test_export.json", userId);

        Assertions.assertTrue(response.hasFile(), "Ответ должен содержать файл");
        Assertions.assertNotNull(response.getFile(), "Файл не должен быть null");
        Assertions.assertTrue(response.getFile().exists(), "Файл должен существовать");
        Assertions.assertTrue(response.getFile().length() > 0, "Файл не должен быть пустым");
        Assertions.assertEquals("test_export.json", response.getFileName(), "Имя файла должно совпадать");

        if (response.getFile().exists()) {
            response.getFile().delete();
        }
    }

    /**
     * Тест прямой работы с FileWork (отдельно от MessageHandler)
     */
    @Test
    void testFileWorkDirectly() throws Exception {
        FileWork fileWork = new FileWork();
        UserData userData = new UserData();

        userData.addTask("Задача 1");
        userData.addTask("Задача 2");
        userData.markTaskDone("Задача 1");

        File exportFile = fileWork.export("testUser",
                userData.getTasks(),
                userData.getCompletedTasks(),
                "test_export_direct.json");

        Assertions.assertNotNull(exportFile, "Файл не должен быть null");
        Assertions.assertTrue(exportFile.exists(), "Файл должен существовать");
        Assertions.assertTrue(exportFile.length() > 0, "Файл не должен быть пустым");
        Assertions.assertEquals("test_export_direct.json", exportFile.getName(), "Имя файла должно совпадать");

        if (exportFile.exists()) {
            exportFile.delete();
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
    void testImportCommand_WithValidFile() throws Exception {
        File testFile = File.createTempFile("test_import", ".json");
        String jsonContent = """
        {
            "current_tasks": ["Задача 1", "Задача 2"],
            "completed_tasks": ["Выполненная задача"]
        }
        """;
        Files.write(testFile.toPath(), jsonContent.getBytes());

        try (FileInputStream inputStream = new FileInputStream(testFile)) {
            MessageHandler.BotResponse response = messageHandler.processImport(inputStream, "user123");
            Assertions.assertEquals("""
                    Задачи успешно добавлены,
                    можете проверить списки с помощью команд /tasks и /dTask
                    """, response.getMessage());
        }

        MessageHandler.BotResponse tasksResponse = messageHandler.processUserInput("/tasks", "user123");
        Assertions.assertTrue(tasksResponse.getMessage().contains("Задача 1"));
        Assertions.assertTrue(tasksResponse.getMessage().contains("Задача 2"));

        MessageHandler.BotResponse completedTasksResponse = messageHandler.processUserInput("/dTask", "user123");
        Assertions.assertTrue(completedTasksResponse.getMessage().contains("Выполненная задача"));

        testFile.delete();
    }

    
    /**
     * Тест неизвестной команды
     */
    @Test
    void testUnknownCommand() {
        MessageHandler.BotResponse response = messageHandler.processUserInput("/unknown", "user123");
        Assertions.assertNotNull(response.getMessage());
        Assertions.assertTrue(response.getMessage().contains("Неизвестная команда"));
        Assertions.assertTrue(response.getMessage().contains("/help"));
    }

    /**
     * Тест процесса регистрации
     */
    @Test
    void testRegistrationProcess() {
        String newUserId = "newUser";

        MessageHandler.BotResponse step1 = messageHandler.processUserInput("/registration", newUserId);
        Assertions.assertTrue(step1.getMessage().contains("Регистрация"));
        Assertions.assertTrue(step1.getMessage().contains("логин"));

        MessageHandler.BotResponse step2 = messageHandler.processUserInput("new_test_user", newUserId);
        Assertions.assertTrue(step2.getMessage().contains("пароль"));

        MessageHandler.BotResponse step3 = messageHandler.processUserInput("password123", newUserId);
        Assertions.assertTrue(step3.getMessage().contains("✅ Регистрация прошла успешно"));

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
        Assertions.assertTrue(response.getMessage().contains("Добро пожаловать"));
        Assertions.assertTrue(response.getMessage().contains("авторизоваться"));
    }
}
