package org.example;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions.*;
import java.io.File;
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
        String result = messageHandler.processUserInput("/add Полить цветы", "user123");
        Assertions.assertEquals("Задача \"Полить цветы\" добавлена!", result);
    }

    /**
     * Тест добавления уже существующей задачи.
     */
    @Test
    void testAddExistingTask() {
        messageHandler.processUserInput("/add Полить цветы", "user123");
        String result = messageHandler.processUserInput("/add Полить цветы", "user123");
        Assertions.assertEquals("Задача \"Полить цветы\" уже есть в списке!", result);
    }

    /**
     * Тест отображения пустого списка задач.
     */
    @Test
    void testShowEmptyTasks() {
        String result = messageHandler.processUserInput("/tasks", "user123");
        Assertions.assertEquals("Список задач пуст!", result);
    }

    /**
     * Тест отображения списка задач.
     */
    @Test
    void testShowTasks() {
        messageHandler.processUserInput("/add Задача 1", "user123");
        messageHandler.processUserInput("/add Задача 2", "user123");
        String result = messageHandler.processUserInput("/tasks", "user123");
        String expected = """
        Вот список ваших задач:
          1. Задача 1
          2. Задача 2
        """;
        Assertions.assertEquals(expected, result); // Убираем лишние пробелы с обеих сторон
    }

    /**
     * Тест удаления задачи.
     */
    @Test
    void testDeleteTask() {
        messageHandler.processUserInput("/add Удаляемая задача", "user123");
        String expected = "🗑️ Задача \"Удаляемая задача\" удалена из списка задач!";
        String result = messageHandler.processUserInput("/delete Удаляемая задача", "user123");
        Assertions.assertEquals(expected, result);
    }
    /**
     * Тест отметки задачи как выполненной.
     */
    @Test
    void testMarkTaskDone() {
        messageHandler.processUserInput("/add Полить цветы", "user123");
        String result = messageHandler.processUserInput("/done Полить цветы", "user123");
        Assertions.assertEquals("Задача \"Полить цветы\" отмечена выполненной!", result);
    }

    /**
     * Тест отображения пустого списка выполненных задач.
     */
    @Test
    void testShowEmptyCompletedTasks() {
        String result = messageHandler.processUserInput("/dTask", "user123");
        Assertions.assertEquals("Список выполненных задач пуст!", result);
    }

    /**
     * Тест отображения списка выполненных задач.
     */
    @Test
    void testShowCompletedTasks() {
        messageHandler.processUserInput("/add Полить цветы", "user123");
        messageHandler.processUserInput("/done Полить цветы", "user123");
        String expected = """
                ✅ Вот список выполненных задач:
                  1. Полить цветы ✔
                """;
        String result = messageHandler.processUserInput("/dTask", "user123");
        Assertions.assertEquals(expected, result);
    }

    /**
     * Тест экспорта без имени файла.
     */
    @Test
    void testExportWithoutFilename() {
        String result = messageHandler.processUserInput("/export", "user123");
        Assertions.assertEquals("Напишите имя файла после /export", result);
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

        File exportFile = messageHandler.Export_logic(userId, "test_export.json");
        Assertions.assertNotNull(exportFile);
        Assertions.assertTrue(exportFile.exists());
        Assertions.assertTrue(exportFile.length() > 0);
        Assertions.assertEquals("test_export.json", exportFile.getName());
    }

    /**
     * Тест проверки сообщения запрашивающего файл
     */
    @Test
    void testImportCommand_FileRequest() {
        String result = messageHandler.processUserInput("/import", "user123");
        Assertions.assertEquals("Отправьте JSON файл со списком задач", result);
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
        String result = messageHandler.Import_logic(testFile, "user123");
        Assertions.assertEquals("Задачи успешно добавлены, можете проверить списки с помощью команд /tasks и /dTask", result);

        String tasksList = messageHandler.processUserInput("/tasks", "user123");
        Assertions.assertTrue(tasksList.contains("Задача 1"));
        Assertions.assertTrue(tasksList.contains("Задача 2"));

        String completedTasks = messageHandler.processUserInput("/dTask", "user123");
        Assertions.assertTrue(completedTasks.contains("Выполненная задача"));
    }
}
