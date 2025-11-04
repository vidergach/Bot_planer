package org.example;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions.*;
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
        String expected = """
                📝 Ваши задачи:
                1. Задача 1
                2. Задача 2
                """;
        Assertions.assertEquals(expected, response.getMessage());
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
        String expected = """
                ✅ Выполненные задачи:
                1. Полить цветы
                """;
        Assertions.assertEquals(expected, response.getMessage());
    }


    /**
     * Тест экспорта без имени файла.
     */
    @Test
    void testExportWithoutFilename() {
        MessageHandler.BotResponse response = messageHandler.processUserInput("/export", "user123");
        Assertions.assertEquals("Напишите имя файла после /export", response.getMessage());
    }

    /**
     * Тест экспорта файла с задачами.
     */
    @Test
    void exportLogic_WithTasks() throws Exception {
        MessageHandler messageHandler = new MessageHandler();
        String userId = "testUserExport";

        messageHandler.processUserInput("/add Задача 1", userId);
        messageHandler.processUserInput("/add Задача 2", userId);
        messageHandler.processUserInput("/done Задача 1", userId);

        // Прямая работа с файлами через FileWork
        FileWork fileWork = new FileWork();
        UserData userData = new UserData();
        userData.addTask("Задача 1");
        userData.addTask("Задача 2");
        userData.markTaskDone("Задача 1");

        File exportFile = fileWork.export(userId, userData.getTasks(), userData.getCompletedTasks(), "test_export.json");
        Assertions.assertNotNull(exportFile);
        Assertions.assertTrue(exportFile.exists());
        Assertions.assertTrue(exportFile.length() > 0);
        Assertions.assertEquals("test_export.json", exportFile.getName());

        // Очистка
        fileWork.delete(exportFile);
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
    void importCommand_WithValidFile() throws Exception {
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
        String expectedTasks = """
                📝 Ваши задачи:
                1. Задача 1
                2. Задача 2
                """;
        Assertions.assertEquals(expectedTasks, tasksResponse.getMessage());

        MessageHandler.BotResponse completedTasksResponse = messageHandler.processUserInput("/dTask", "user123");
        String expectedCompleted = """
                ✅ Выполненные задачи:
                1. Выполненная задача
                """;
        Assertions.assertEquals(expectedCompleted, completedTasksResponse.getMessage());
    }
}
