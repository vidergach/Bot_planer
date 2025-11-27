package org.example;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для управления операциями с задачами.
 */
public class OperationService {
    private final DatabaseService databaseService;
    private final FileWork fileWork = new FileWork();
    private final Map<String, Operation> operationStates = new ConcurrentHashMap<>();

    /**
     * Класс для отслеживания состояния операции.
     */
    private class Operation {
        String type;
        Operation(String type) {
            this.type = type;
        }
    }

    /**
     * Конструктор сервиса операций.
     */
    public OperationService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    /**
     * Обрабатывает операцию, требующую дополнительного ввода от пользователя
     */
    public BotResponse handleOperation(String operation, String parameter, String userId, String prompt) {
        if (parameter.isEmpty()) {
            operationStates.put(userId, new Operation(operation));
            return new BotResponse(prompt);
        } else {
            return executeOperation(operation, parameter, userId);
        }
    }

    /**
     * Обрабатывает операции после получения ввода
     */
    public BotResponse handleOperationStep(String userId, String userInput) {
        Operation state = operationStates.get(userId);
        operationStates.remove(userId);
        return executeOperation(state.type, userInput.trim(), userId);
    }

    /**
     * Выполняет указанную операцию
     */
    private BotResponse executeOperation(String operation, String input, String userId) {
        try {
            String internalUserId = databaseService.getUserIdByPlatform(userId);
            if (internalUserId == null) {
                return new BotResponse("Пользователь не авторизован");
            }

            return switch (operation) {
                case "add" -> {
                    databaseService.addTask(internalUserId, input);
                    yield new BotResponse("Задача \"" + input + "\" добавлена!");
                }
                case "delete" -> {
                    databaseService.deleteTask(internalUserId, input);
                    yield new BotResponse("🗑️ Задача \"" + input + "\" удалена!");
                }
                case "done" -> {
                    databaseService.markTaskDone(internalUserId, input);
                    yield new BotResponse("✅ Задача \"" + input + "\" выполнена!");
                }
                case "export" -> {
                    DatabaseService.TaskData taskData = databaseService.exportTasks(internalUserId);
                    File exportFile = fileWork.export(taskData.getCurrentTasks(), taskData.getCompletedTasks(), input);
                    yield new BotResponse("Ваши задачи экспортированы в файл: " + exportFile.getName(),
                            exportFile, exportFile.getName());
                }
                default -> new BotResponse("Неизвестная команда.\nВведите /help для просмотра доступных команд.");
            };
        } catch (Exception e) {
            e.printStackTrace();
            return new BotResponse("Ошибка " + getOperationError(operation) + ": " + e.getMessage());
        }
    }

    /**
     * Возвращает описание ошибки для операции
     */
    private String getOperationError(String operation) {
        return switch (operation) {
            case "add" -> "добавления задачи";
            case "delete" -> "удаления задачи";
            case "done" -> "выполнения задачи";
            case "export" -> "экспорта";
            default -> "операции";
        };
    }

    /**
     * Обрабатывает текущие задачи пользователя
     */
    public BotResponse handleShowTasks(String internalUserId) {
        try {
            List<String> tasks = databaseService.getCurrentTasks(internalUserId);
            if (tasks.isEmpty()) {
                return new BotResponse("📝 Список задач пуст!");
            }

            StringBuilder sb = new StringBuilder("📝 Ваши задачи:\n");
            for (int i = 0; i < tasks.size(); i++) {
                sb.append(i + 1).append(". ").append(tasks.get(i)).append("\n");
                Integer taskId = databaseService.getTaskId(internalUserId, tasks.get(i));
                if (taskId != null) {
                    List<String> subtasks = databaseService.getSubtasks(taskId);
                    for (int j = 0; j < subtasks.size(); j++) {
                        sb.append(" ").append(i + 1).append(".").append(j + 1).append(" ").append(subtasks.get(j)).append("\n");
                    }
                }
            }
            return new BotResponse(sb.toString());
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при получении задач: " + e.getMessage());
        }
    }

    /**
     * Обрабатывает выполненные задачи пользователя
     */
    public BotResponse handleShowCompletedTasks(String internalUserId) {
        try {
            List<String> completedTasks = databaseService.getCompletedTasks(internalUserId);
            if (completedTasks.isEmpty()) {
                return new BotResponse("✅ Список выполненных задач пуст!");
            }

            StringBuilder sb = new StringBuilder("✅ Выполненные задачи:\n");
            for (int i = 0; i < completedTasks.size(); i++) {
                sb.append(i + 1).append(". ").append(completedTasks.get(i)).append("\n");
            }
            return new BotResponse(sb.toString());
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при получении выполненных задач: " + e.getMessage());
        }
    }

    /**
     * Проверяет, находится ли пользователь в процессе операции
     */
    public boolean isUserInOperationProcess(String userId) {
        return operationStates.containsKey(userId);
    }
}