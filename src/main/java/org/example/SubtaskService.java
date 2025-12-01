package org.example;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для управления подзадачами.
 */
public class SubtaskService {
    private final DatabaseService databaseService;
    private final Map<String, SubtaskState> expandStates = new ConcurrentHashMap<>();
    private final OpenRouterClient gptClient;

    private final String SUBTASK_MESSAGE = """
            Отлично! Выберите действие, которое хотите сделать:
            /add_subtasks_with_gpt - добавить подзадачи с помощью чата GPT
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
        List<String> generatedSubtasks; // Для хранения сгенерированных GPT подзадач

        SubtaskState(Integer taskId, String taskText) {
            this.taskId = taskId;
            this.taskText = taskText;
            this.step = null;
            this.generatedSubtasks = new ArrayList<>();
        }
    }

    /**
     * Конструктор сервиса подзадач.
     */
    public SubtaskService(DatabaseService databaseService) {
        this.databaseService = databaseService;
        // Получаем API ключ из переменных окружения
        String apiKey = System.getenv("OPENROUTER_API_KEY");
        this.gptClient = new OpenRouterClient(apiKey != null ? apiKey : "");
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
                case "gpt_details" -> handleGptDetails(userId, userInput, state);
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
            case "/add_subtasks_with_gpt", "GPT добавление подзадач" -> {
                state.step = "gpt_details";
                yield new BotResponse("""
                        Напишите детали и пожелания по задаче, для более точного добавления новых подзадач.
                        Например: "Рисунок красками и кисточками, хочу рисовать природу"
                        """);
            }
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
            case "/save_subtasks_from_gpt", "Сохранить" -> handleSaveGptSubtasks(userId, state);
            case "/delete_subtasks_from_gpt", "Удалить" -> handleDeleteGptSubtasks(userId, state);
            default -> new BotResponse("Используйте кнопки для работы с подзадачами или введите /finish_expand для выхода.");
        };
    }

    /**
     * Обрабатывает детали для GPT
     */
    private BotResponse handleGptDetails(String userId, String userInput, SubtaskState state) {
        if (userInput.trim().isEmpty()) {
            return new BotResponse("Пожалуйста, напишите детали и пожелания по задаче:");
        }

        try {
            // Формируем промпт для GPT
            String prompt = String.format("""
                    Разбей задачу "%s" на подзадачи. Детали от пользователя: %s
                    Верни только список подзадач, по одной на каждой строке, без номеров и маркеров.
                    """, state.taskText, userInput);

            // Отправляем запрос к GPT
            String gptResponse = gptClient.sendRequest(prompt);

            // Парсим ответ на отдельные подзадачи
            List<String> subtasks = parseGptResponse(gptResponse);
            state.generatedSubtasks = subtasks;
            state.step = "gpt_review";

            // Формируем сообщение с предложением сохранить или удалить
            StringBuilder sb = new StringBuilder();
            sb.append("🤖 Подзадачи, сгенерированные ИИ:\n\n");
            for (int i = 0; i < subtasks.size(); i++) {
                sb.append(i + 1).append(". ").append(subtasks.get(i)).append("\n");
            }
            sb.append("\nПосмотрите список подзадач. Если все верно, нажмите [Сохранить], в противном случае [Удалить]");

            return new BotResponse(sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
            state.step = null;
            return new BotResponse("❌ Ошибка при генерации подзадач: " + e.getMessage());
        }
    }

    /**
     * Парсит ответ GPT на отдельные подзадачи
     */
    private List<String> parseGptResponse(String gptResponse) {
        List<String> subtasks = new ArrayList<>();
        String[] lines = gptResponse.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();
            // Убираем номера, маркеры и лишние символы
            if (!trimmed.isEmpty()) {
                // Убираем начальные цифры с точкой, дефисы, звездочки
                String cleanLine = trimmed.replaceAll("^[\\d\\s]*[-•*.]\\s*", "").trim();
                if (!cleanLine.isEmpty()) {
                    subtasks.add(cleanLine);
                }
            }
        }

        return subtasks;
    }

    /**
     * Сохраняет подзадачи, сгенерированные GPT
     */
    private BotResponse handleSaveGptSubtasks(String userId, SubtaskState state) {
        try {
            for (String subtask : state.generatedSubtasks) {
                databaseService.addSubtask(state.taskId, subtask);
            }

            state.generatedSubtasks.clear();
            state.step = null;

            return new BotResponse("✅ Подзадачи сохранены! Можете посмотреть общий список задач.");

        } catch (SQLException e) {
            e.printStackTrace();
            state.step = null;
            return new BotResponse("❌ Ошибка при сохранении подзадач: " + e.getMessage());
        }
    }

    /**
     * Удаляет сгенерированные GPT подзадачи
     */
    private BotResponse handleDeleteGptSubtasks(String userId, SubtaskState state) {
        state.generatedSubtasks.clear();
        state.step = null;
        return new BotResponse("🗑️ Подзадачи удалены. При повторном процессе сделайте запрос более точным, чтобы получить желаемые подзадачи.");
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