package org.example;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для управления подзадачами.
 */
public class SubtaskService {
    private final DatabaseService databaseService;
    private final Map<String, SubtaskState> expandStates = new ConcurrentHashMap<>();

    private final String SUBTASK_MESSAGE = """
            Отлично! Выберите действие, которое хотите сделать:
            /add_subtask - добавить подзадачу
            /delete_subtask - удалить подзадачу
            /edit_subtask - изменить подзадачу
            /finish_expand - окончить расширение задачи
            """;

    /**
     * Класс для отслеживания состояния работы с подзадачами.
     */
    private class SubtaskState {
        Integer taskId;
        String taskText;
        String step;
        String selectSubtask;

        SubtaskState(Integer taskId, String taskText) {
            this.taskId = taskId;
            this.taskText = taskText;
            this.step = null;
        }
    }

    /**
     * Конструктор сервиса подзадач.
     */
    public SubtaskService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    /**
     * Начинает режим работы с подзадачами для пользователя.
     */
    public void startSubtaskMode(String userId, Integer taskId, String taskText) {
        expandStates.put(userId, new SubtaskState(taskId, taskText));
    }

    /**
     * Проверяет, находится ли пользователь в режиме работы с подзадачами.
     */
    public boolean isUserInSubtaskMode(String userId) {
        return expandStates.containsKey(userId);
    }

    /**
     * Возвращает сообщение для работы с подзадачами.
     */
    public String getSubtaskMessage() {
        return SUBTASK_MESSAGE;
    }

    /**
     * Обрабатывает ввод данных в режиме расширения задачи
     */
    public BotResponse handleSubtaskInput(String userId, String userInput, Object stateObj) {
        SubtaskState state = (SubtaskState) stateObj;
        try {
            String internalUserId = databaseService.getUserIdByPlatform(userId);
            if (internalUserId == null) {
                expandStates.remove(userId);
                return new BotResponse("Ошибка, пользователь не авторизован.");
            }

            return switch (state.step) {
                case "add_subtask" -> handleAddSubtask(userId, userInput, state.taskId);
                case "delete_subtask" -> handleDeleteSubtask(userId, userInput, state.taskId);
                case "edit_subtask" -> handleEditSubtask(userInput, state);
                default -> {
                    expandStates.remove(userId);
                    yield new BotResponse("Ошибка режима расширения");
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
            return new BotResponse("Ошибка при работе с подзадачами: " + e.getMessage());
        }
    }

    /**
     * Обрабатывает команды работы с подзадачами
     */
    public BotResponse handleSubtaskCommand(String command, String userId) throws SQLException {
        SubtaskState state = expandStates.get(userId);
        if (state == null) {
            return new BotResponse("Сначала выберите задачу для расширения.");
        }

        if (state.step != null) {
            return handleSubtaskInput(userId, command, state);
        }

        return switch (command) {
            case "/add_subtask", "\u2795 Добавить подзадачу" -> {
                state.step = "add_subtask";
                yield new BotResponse("Отлично! Напишите подзадачу для добавления:");
            }
            case "/delete_subtask", "\u2718 Удалить подзадачу" -> {
                state.step = "delete_subtask";
                yield new BotResponse("Отлично! Напишите подзадачу для удаления:");
            }
            case "/edit_subtask", "Изменить подзадачу" -> {
                state.step = "edit_subtask";
                state.selectSubtask = null;
                yield new BotResponse("Отлично! Напишите подзадачу для изменения:");
            }
            case "/finish_expand", "Окончить расширение" -> handleFinishExpand(userId);
            default -> new BotResponse("Используйте кнопки для работы с подзадачами или введите /finish_expand для выхода.");
        };
    }

    /**
     * Обрабатывает добавление подзадачи
     */
    private BotResponse handleAddSubtask(String userId, String userInput, Integer taskId) throws SQLException {
        if (userInput.trim().isEmpty()) {
            return new BotResponse("Отлично! Напишите подзадачу для добавления.");
        }
        try {
            databaseService.addSubtask(taskId, userInput);
            expandStates.get(userId).step = null;
            return new BotResponse("Подзадача добавлена");
        } catch (SQLException e) {
            expandStates.get(userId).step = null;
            if (e.getErrorCode() == 19) {
                return new BotResponse("Подзадача уже существует.");
            }
            throw new RuntimeException("Не удалось добавить подзадачу.", e);
        }
    }

    /**
     * Обрабатывает удаление подзадачи
     */
    private BotResponse handleDeleteSubtask(String userId, String userInput, Integer taskId) throws SQLException {
        if (userInput.trim().isEmpty()) {
            List<String> subtasks = databaseService.getSubtasks(taskId);
            if (subtasks.isEmpty()) {
                expandStates.get(userId).step = null;
                return new BotResponse("Нет подзадачи для удаления.");
            }
            StringBuilder sb = new StringBuilder("Отлично! Выберите задачу для удаления.");
            for (int i = 0; i < subtasks.size(); i++) {
                sb.append(i + 1).append(". ").append(subtasks.get(i)).append("\n");
            }
            return new BotResponse(sb.toString());
        }
        String subtaskToDelete = userInput.trim();
        List<String> subtasks = databaseService.getSubtasks(taskId);

        if (!subtasks.contains(subtaskToDelete)) {
            expandStates.get(userId).step = null;
            return new BotResponse("Подзадача не найдена.");
        }

        try {
            databaseService.deleteSubtask(taskId, subtaskToDelete);
            expandStates.get(userId).step = null;
            return new BotResponse("Подзадача удалена.");
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось удалить подзадачу: " + e.getMessage());
        }
    }

    /**
     * Обрабатывает изменение подзадачи
     */
    private BotResponse handleEditSubtask(String userInput, SubtaskState state) throws SQLException {
        if (state.selectSubtask == null) {
            if (userInput.trim().isEmpty()) {
                List<String> subtasks = databaseService.getSubtasks(state.taskId);
                if (subtasks.isEmpty()) {
                    state.step = null;
                    return new BotResponse("Нет подзадач для изменения.");
                }

                StringBuilder sb = new StringBuilder("Отлично! Напишите подзадачу для изменения.\n");
                for (int i = 0; i < subtasks.size(); i++) {
                    sb.append(i + 1).append(". ").append(subtasks.get(i)).append("\n");
                }
                return new BotResponse(sb.toString());
            }

            String selectedSubtask = userInput.trim();
            List<String> subtasks = databaseService.getSubtasks(state.taskId);

            if (!subtasks.contains(selectedSubtask)) {
                state.step = null;
                return new BotResponse("Подзадача не найдена.");
            }

            state.selectSubtask = selectedSubtask;
            return new BotResponse("Напишите новую формулировку:");
        } else {
            if (userInput.trim().isEmpty()) {
                return new BotResponse("Напишите новую формулировку:");
            }
            try {
                databaseService.editSubtask(state.taskId, state.selectSubtask, userInput.trim());
                state.step = null;
                state.selectSubtask = null;
                return new BotResponse("Подзадача изменена.");
            } catch (SQLException e) {
                throw new RuntimeException("Не удалось изменить подзадачу: " + e.getMessage());
            }
        }
    }

    /**
     * Завершает режим работы с подзадачами
     */
    private BotResponse handleFinishExpand(String userId) {
        expandStates.remove(userId);
        return new BotResponse("Добавление подзадач завершено! Вы можете посмотреть список задач.");
    }

    /**
     * Обрабатывает ввод в режиме подзадач
     */
    public BotResponse processSubtaskInput(String userId, String userInput) {
        SubtaskState state = expandStates.get(userId);
        if (state != null && state.step != null) {
            return handleSubtaskInput(userId, userInput, state);
        }
        try {
            return handleSubtaskCommand(userInput, userId);
        } catch (SQLException e) {
            e.printStackTrace();
            return new BotResponse("Ошибка при работе с подзадачами: " + e.getMessage());
        }
    }
}