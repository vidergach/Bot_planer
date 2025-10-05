package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals("Задача \"Полить цветы\" добавлена!", result);
    }

    /**
     * Тест добавления уже существующей задачи.
     */
    @Test
    void testAddExistingTask() {
        messageHandler.processUserInput("/add Полить цветы", "user123");
        String result = messageHandler.processUserInput("/add Полить цветы", "user123");
        assertEquals("Задача \"Полить цветы\" уже есть в списке!", result);
    }

    /**
     * Тест отображения пустого списка задач.
     */
    @Test
    void testShowEmptyTasks() {
        String result = messageHandler.processUserInput("/tasks", "user123");
        assertEquals("Список задач пуст!", result);
    }

    /**
     * Тест отображения списка задач.
     */
    @Test
    void testShowTasks() {
        messageHandler.processUserInput("/add Задача 1", "user123");
        messageHandler.processUserInput("/add Задача 2", "user123");

        String result = messageHandler.processUserInput("/tasks", "user123");
        assertTrue(result.contains("Вот список ваших задач"));
        assertTrue(result.contains("Задача 1"));
        assertTrue(result.contains("Задача 2"));
    }

    /**
     * Тест удаления задачи.
     */
    @Test
    void testDeleteTask() {
        messageHandler.processUserInput("/add Удаляемая задача", "user123");
        String result = messageHandler.processUserInput("/delete Удаляемая задача", "user123");
        assertTrue(result.contains("🗑️ Задача \"Удаляемая задача\" удалена"));
    }

    /**
     * Тест отметки задачи как выполненной.
     */
    @Test
    void testMarkTaskDone() {
        messageHandler.processUserInput("/add Полить цветы", "user123");
        String result = messageHandler.processUserInput("/done Полить цветы", "user123");
        assertEquals("Задача \"Полить цветы\" отмечена выполненной!", result);
    }

    /**
     * Тест отображения пустого списка выполненных задач.
     */
    @Test
    void testShowEmptyCompletedTasks() {
        String result = messageHandler.processUserInput("/dTask", "user123");
        assertEquals("Список выполненных задач пуст!", result);
    }

    /**
     * Тест отображения списка выполненных задач.
     */
    @Test
    void testShowCompletedTasks() {
        messageHandler.processUserInput("/add Полить цветы", "user123");
        messageHandler.processUserInput("/done Полить цветы", "user123");

        String result = messageHandler.processUserInput("/dTask", "user123");
        assertTrue(result.contains("✅ Вот список выполненных задач"));
        assertTrue(result.contains("Полить цветы"));
    }
}