package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MessageHandlerTests {
    private MessageHandler messageHandler;

    @BeforeEach
    void setUp() {
        messageHandler = new MessageHandler();
    }

    @Test
    void testAddTask() {
        String result = messageHandler.processUserInput("/add Полить цветы", "user123");
        assertEquals("Задача \"Полить цветы\" добавлена!", result);
    }

    @Test
    void testAddEmptyTask() {
        String result = messageHandler.processUserInput("/add", "user123");
        assertTrue(result.contains("забыли указать задачу"));
    }

    @Test
    void testAddExistingTask() {
        messageHandler.processUserInput("/add Полить цветы", "user123");
        String result = messageHandler.processUserInput("/add Полить цветы", "user123");
        assertEquals("Задача \"Полить цветы\" уже есть в списке!", result);
    }

    @Test
    void testShowEmptyTasks() {
        String result = messageHandler.processUserInput("/tasks", "user123");
        assertEquals("Список задач пуст!", result);
    }

    @Test
    void testShowTasks() {
        messageHandler.processUserInput("/add Задача 1", "user123");
        messageHandler.processUserInput("/add Задача 2", "user123");

        String result = messageHandler.processUserInput("/tasks", "user123");
        assertTrue(result.contains("Вот список ваших задач"));
        assertTrue(result.contains("Задача 1"));
        assertTrue(result.contains("Задача 2"));
    }
    @Test
    void testDeleteTask() {
        messageHandler.processUserInput("/add Удаляемая задача", "user123");
        String result = messageHandler.processUserInput("/delete Удаляемая задача", "user123");
        assertTrue(result.contains("🗑️ Задача \"Удаляемая задача\" удалена"));
    }
}