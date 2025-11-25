package org.example;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Основной класс для обработки сообщений пользователя.
 * Координирует работу сервисов аутентификации и операций.
 */
public class MessageHandler {
    private final DatabaseService databaseService;
    private final AuthService authService;
    private final OperationService operationService;
    private final FileWork fileWork;

    public MessageHandler() {
        this.databaseService = new DatabaseService();
        this.authService = new AuthService(databaseService);
        this.operationService = new OperationService(databaseService, authService);
        this.fileWork = new FileWork();
    }

    public MessageHandler(DatabaseService databaseService) {
        this.databaseService = databaseService;
        this.authService = new AuthService(databaseService);
        this.operationService = new OperationService(databaseService, authService);
        this.fileWork = new FileWork();
    }

    public BotResponse processUserInput(String userInput, String userId, String platformType) {
        System.out.println("сообщение: " + userInput + " от: " + userId + " платформа: " + platformType);
        try {
            if (operationService.hasOperationState(userId)) {
                return operationService.handleOperationStep(userId, userInput);
            }

            if (authService.hasAuthState(userId)) {
                return authService.handleAuthStep(userId, userInput);
            }

            String[] parts = userInput.trim().split("\\s+", 2);
            String command = parts[0];
            String parameter = parts.length > 1 ? parts[1].trim() : "";

            if (!authService.isUserAuthenticated(userId, platformType)) {
                if (command.equals("/registration") || command.equals("/login")) {
                    if (command.equals("/registration")) {
                        return authService.handleRegistration(userId, platformType);
                    } else if (command.equals("/login")){
                        return authService.handleLogin(userId, platformType);
                    } else {
                        return authService.handleExit(userId, platformType);
                    }
                }
                return new BotResponse(authService.getWelcomeMessage());
            }
            return operationService.processCommand(command, parameter, userId, platformType);
        } catch (Exception e) {
            e.printStackTrace();
            return new BotResponse("Произошла ошибка: " + e.getMessage());
        }
    }

    public BotResponse processImport(InputStream inputStream, String userId) {
        try {
            String internalUserId = databaseService.getUserIdByPlatform(userId);
            if (internalUserId == null) {
                return new BotResponse("Ошибка: пользователь не авторизован. Пожалуйста, войдите снова.");
            }
            FileWork.FileData importedData = fileWork.importData(inputStream);
            for (String task : importedData.current_tasks()) {
                try {
                    databaseService.addTask(internalUserId, task);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            for (String task : importedData.completed_tasks()) {
                try {
                    databaseService.markTaskDone(internalUserId, task);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return new BotResponse("""
                    Импорт завершен успешно!
                    Можете проверить списки с помощью команд /tasks и /dTask
                    """);
        } catch (Exception e) {
            e.printStackTrace();
            return new BotResponse("Ошибка при импорте: " + e.getMessage());
        }
    }
}