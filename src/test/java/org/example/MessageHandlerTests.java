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
    private static int userCounter = 0;

    @BeforeEach
    void setUp() {
        messageHandler = new MessageHandler();
    }

    /**
     * Метод для регистрации тестового пользователя
     */
    private String registerTestUser(String platformType) {
        userCounter++;
        String userId = "testUser" + userCounter + "_" + System.currentTimeMillis();

        MessageHandler.BotResponse step1 = messageHandler.processUserInput("/registration", userId, platformType);
        MessageHandler.BotResponse step2 = messageHandler.processUserInput("testuser_" + userId, userId, platformType);
        MessageHandler.BotResponse step3 = messageHandler.processUserInput("testpass", userId, platformType);

        if (!step3.getMessage().contains("успешно") && !step3.getMessage().contains("✅")) {
            throw new RuntimeException("Не удалось зарегистрировать тестового пользователя: " + step3.getMessage());
        }

        return userId;
    }

    /**
     * Метод для очистки задач пользователя
     */
    private void clearUserTasks(String userId, String platformType) {
        try {
            // Получаем текущие задачи и удаляем их по одной
            MessageHandler.BotResponse tasksResponse = messageHandler.processUserInput("/tasks", userId, platformType);
            if (!tasksResponse.getMessage().contains("пуст")) {
                // Извлекаем задачи из сообщения и удаляем их
                String message = tasksResponse.getMessage();
                String[] lines = message.split("\n");
                for (String line : lines) {
                    if (line.matches("\\d+\\.\\s+.+")) {
                        String task = line.substring(line.indexOf(". ") + 2);
                        messageHandler.processUserInput("/delete " + task, userId, platformType);
                    }
                }
            }

            MessageHandler.BotResponse completedResponse = messageHandler.processUserInput("/dTask", userId, platformType);
            if (!completedResponse.getMessage().contains("пуст")) {
                String message = completedResponse.getMessage();
                String[] lines = message.split("\n");
                for (String line : lines) {
                    if (line.matches("\\d+\\.\\s+.+")) {
                        String task = line.substring(line.indexOf(". ") + 2);
                    }
                }
            }
        } catch (Exception e) {

        }
    }

    /**
     * Тест добавления новой задачи.
     */
    @Test
    void testAddTask() {
        String userId = registerTestUser("telegram");
        clearUserTasks(userId, "telegram");

        MessageHandler.BotResponse response = messageHandler.processUserInput("/add Полить цветы", userId, "telegram");
        Assertions.assertEquals("Задача \"Полить цветы\" добавлена!", response.getMessage());
    }

    /**
     * Тест добавления уже существующей задачи.
     */
    @Test
    void testAddExistingTask() {
        String userId = registerTestUser("telegram");
        clearUserTasks(userId, "telegram");

        messageHandler.processUserInput("/add Полить цветы", userId, "telegram");
        MessageHandler.BotResponse response = messageHandler.processUserInput("/add Полить цветы", userId, "telegram");

        Assertions.assertTrue(response.getMessage().contains("уже есть в списке") ||
                response.getMessage().contains("уже существует"));
    }

    /**
     * Тест отображения пустого списка задач.
     */
    @Test
    void testShowEmptyTasks() {
        String userId = registerTestUser("telegram");
        clearUserTasks(userId, "telegram");

        MessageHandler.BotResponse response = messageHandler.processUserInput("/tasks", userId, "telegram");
        Assertions.assertTrue(response.getMessage().contains("пуст"));
    }

    /**
     * Тест отображения списка задач.
     */
    @Test
    void testShowTasks() {
        String userId = registerTestUser("telegram");
        clearUserTasks(userId, "telegram");

        messageHandler.processUserInput("/add Задача 1", userId, "telegram");
        messageHandler.processUserInput("/add Задача 2", userId, "telegram");
        MessageHandler.BotResponse response = messageHandler.processUserInput("/tasks", userId, "telegram");

        Assertions.assertTrue(response.getMessage().contains("Задача 1"));
        Assertions.assertTrue(response.getMessage().contains("Задача 2"));
        Assertions.assertFalse(response.getMessage().contains("пуст"));
    }

    /**
     * Тест удаления задачи.
     */
    @Test
    void testDeleteTask() {
        String userId = registerTestUser("telegram");
        clearUserTasks(userId, "telegram");

        messageHandler.processUserInput("/add Удаляемая задача", userId, "telegram");
        MessageHandler.BotResponse response = messageHandler.processUserInput("/delete Удаляемая задача", userId, "telegram");
        Assertions.assertTrue(response.getMessage().contains("удалена"));
    }

    /**
     * Тест отметки задачи как выполненной.
     */
    @Test
    void testMarkTaskDone() {
        String userId = registerTestUser("telegram");

        MessageHandler.BotResponse addResponse = messageHandler.processUserInput("/add Полить цветы", userId, "telegram");
        Assertions.assertTrue(addResponse.getMessage().contains("добавлена"),
                "Задача должна быть добавлена: " + addResponse.getMessage());

        MessageHandler.BotResponse doneResponse = messageHandler.processUserInput("/done Полить цветы", userId, "telegram");
        Assertions.assertTrue(doneResponse.getMessage().contains("выполнена"),
                "Задача должна быть отмечена выполненной: " + doneResponse.getMessage());

        MessageHandler.BotResponse completedResponse = messageHandler.processUserInput("/dTask", userId, "telegram");
        Assertions.assertTrue(completedResponse.getMessage().contains("Полить цветы"),
                "Задача должна быть в списке выполненных: " + completedResponse.getMessage());
    }
    /**
     * Тест отображения пустого списка выполненных задач.
     */
    @Test
    void testShowEmptyCompletedTasks() {
        String userId = registerTestUser("telegram");

        MessageHandler.BotResponse response = messageHandler.processUserInput("/dTask", userId, "telegram");
        System.out.println("Response for empty completed tasks: " + response.getMessage());

        Assertions.assertTrue(response.getMessage().contains("пуст"),
                "Список выполненных задач должен быть пуст: " + response.getMessage());
    }

    /**
     * Тест отображения списка выполненных задач.
     */
    @Test
    void testShowCompletedTasks() {
        String userId = registerTestUser("telegram");
        clearUserTasks(userId, "telegram");

        messageHandler.processUserInput("/add Полить цветы", userId, "telegram");
        messageHandler.processUserInput("/done Полить цветы", userId, "telegram");

        MessageHandler.BotResponse response = messageHandler.processUserInput("/dTask", userId, "telegram");
        Assertions.assertTrue(response.getMessage().contains("Полить цветы"));
    }

    /**
     * Тест экспорта без имени файла.
     */
    @Test
    void testExportWithoutFilename() {
        String userId = registerTestUser("telegram");
        clearUserTasks(userId, "telegram");

        MessageHandler.BotResponse response = messageHandler.processUserInput("/export", userId, "telegram");
        Assertions.assertTrue(response.getMessage().contains("имя файла"));
    }

    /**
     * Тест экспорта файла с задачами.
     */
    @Test
    void testExportWithTasks() {
        String userId = registerTestUser("telegram");
        clearUserTasks(userId, "telegram");

        messageHandler.processUserInput("/add Задача 1", userId, "telegram");
        messageHandler.processUserInput("/add Задача 2", userId, "telegram");
        messageHandler.processUserInput("/done Задача 1", userId, "telegram");

        MessageHandler.BotResponse response = messageHandler.processUserInput("/export test_export.json", userId, "telegram");

        Assertions.assertTrue(response.hasFile(), "Ответ должен содержать файл");
        Assertions.assertNotNull(response.getFile(), "Файл не должен быть null");

        if (response.getFile() != null && response.getFile().exists()) {
            Assertions.assertTrue(response.getFile().length() > 0, "Файл должен быть не пустым");

            try {
                response.getFile().delete();
            } catch (Exception e) {

            }
        }
    }

    /**
     * Тест прямой работы с FileWork (отдельно от MessageHandler)
     */
    @Test
    void testFileWorkDirectly() throws Exception {
        FileWork fileWork = new FileWork();

        List<String> currentTasks = List.of("Задача 1", "Задача 2");
        List<String> completedTasks = List.of("Выполненная задача 1");

        File exportFile = fileWork.export(currentTasks, completedTasks, "test_export_direct.json");

        Assertions.assertNotNull(exportFile, "Файл не должен быть null");
        Assertions.assertTrue(exportFile.exists(), "Файл должен существовать");
        Assertions.assertTrue(exportFile.length() > 0, "Файл не должен быть пустым");

        try {
            exportFile.delete();
        } catch (Exception e) {

        }
    }

    /**
     * Тест проверки сообщения запрашивающего файл
     */
    @Test
    void testImportCommand_FileRequest() {
        String userId = registerTestUser("telegram");

        MessageHandler.BotResponse response = messageHandler.processUserInput("/import", userId, "telegram");
        Assertions.assertTrue(response.getMessage().contains("импорт") ||
                response.getMessage().contains("JSON") ||
                response.getMessage().contains("файл"));
    }

    /**
     * Тест импорта файла с задачами.
     */
    @Test
    void testImportCommand_WithValidFile() throws Exception {
        String userId = registerTestUser("telegram");
        clearUserTasks(userId, "telegram");

        File testFile = File.createTempFile("test_import", ".json");
        String jsonContent = """
                {
                    "current_tasks": ["Импортированная задача 1", "Импортированная задача 2"],
                    "completed_tasks": ["Выполненная импортированная задача"]
                }
                """;
        Files.write(testFile.toPath(), jsonContent.getBytes());

        try (FileInputStream inputStream = new FileInputStream(testFile)) {
            MessageHandler.BotResponse response = messageHandler.processImport(inputStream, userId);
            Assertions.assertTrue(response.getMessage().contains("успешно") ||
                    response.getMessage().contains("Импорт завершен"));
        }

        MessageHandler.BotResponse tasksResponse = messageHandler.processUserInput("/tasks", userId, "telegram");
        MessageHandler.BotResponse completedTasksResponse = messageHandler.processUserInput("/dTask", userId, "telegram");

        boolean hasImportedTasks = tasksResponse.getMessage().contains("Импортированная") ||
                completedTasksResponse.getMessage().contains("импортированная");

        Assertions.assertTrue(hasImportedTasks, "Должны быть импортированные задачи");
        testFile.delete();
    }

    /**
     * Тест неизвестной команды
     */
    @Test
    void testUnknownCommand() {
        String userId = registerTestUser("telegram");

        MessageHandler.BotResponse response = messageHandler.processUserInput("/unknown", userId, "telegram");
        Assertions.assertTrue(response.getMessage().contains("Неизвестная команда"));
    }

    /**
     * Тест процесса регистрации
     */
    @Test
    void testRegistrationProcess() {
        String newUserId = "newUser_" + System.currentTimeMillis();
        String platformType = "telegram";

        MessageHandler.BotResponse step1 = messageHandler.processUserInput("/registration", newUserId, platformType);
        Assertions.assertTrue(step1.getMessage().contains("логин"));

        MessageHandler.BotResponse step2 = messageHandler.processUserInput("new_test_user_" + System.currentTimeMillis(), newUserId, platformType);
        Assertions.assertTrue(step2.getMessage().contains("пароль"));

        MessageHandler.BotResponse step3 = messageHandler.processUserInput("password123", newUserId, platformType);
        Assertions.assertTrue(step3.getMessage().contains("успешно") ||
                step3.getMessage().contains("✅"));

        MessageHandler.BotResponse response = messageHandler.processUserInput("/add Новая задача", newUserId, platformType);
        Assertions.assertTrue(response.getMessage().contains("добавлена"));
    }

    /**
     * Тест для неавторизованного пользователя
     */
    @Test
    void testUnauthenticatedUser() {
        String newUserId = "unauthenticatedUser_" + System.currentTimeMillis();
        String platformType = "telegram";

        MessageHandler.BotResponse response = messageHandler.processUserInput("/add Задача", newUserId, platformType);
        Assertions.assertTrue(response.getMessage().contains("авторизоваться") ||
                response.getMessage().contains("Добро пожаловать"));
    }

    /**
     * Тест операции с ожиданием ввода
     */
    @Test
    void testOperationWaitingForInput() {
        String userId = registerTestUser("telegram");
        clearUserTasks(userId, "telegram");

        MessageHandler.BotResponse response = messageHandler.processUserInput("/add", userId, "telegram");
        Assertions.assertTrue(response.getMessage().contains("Введите задачу"));

        MessageHandler.BotResponse response2 = messageHandler.processUserInput("Новая задача", userId, "telegram");
        Assertions.assertTrue(response2.getMessage().contains("добавлена"));
    }
}