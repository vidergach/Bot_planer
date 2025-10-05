package org.example;

import java.util.concurrent.ConcurrentHashMap;

public class MessageHandler {

    private final ConcurrentHashMap<String, UserData> userDataMap = new ConcurrentHashMap<>();

    public String processUserInput(String userInput, String userId) {
        System.out.println("сообщение: " + userInput + " от: " + userId);
        UserData userData = userDataMap.computeIfAbsent(userId, k -> new UserData());
        String outputText = processCommand(userInput, userData);
        System.out.println("Ответ: " + outputText);
        return outputText;
    }

    private String processCommand(String userInput, UserData userData) {
        if ("/start".equals(userInput)) {
            return getStartMessage();
        } else if ("/help".equals(userInput)) {
            return getHelpMessage();
        } else if (userInput.startsWith("/add")) {
            return addTask(userInput, userData);
        } else if ("/tasks".equals(userInput)) {
            return showTasks(userData);
        } else if (userInput.startsWith("/done")) {
            return markTaskDone(userInput, userData);
        } else if ("/dTask".equals(userInput)) {
            return showCompletedTasks(userData);
        } else if (userInput.startsWith("/delete")) {
            return deleteTask(userInput, userData);
        } else {
            return "Неизвестная команда.\nВведите /help для просмотра доступных команд.";
        }
    }

    private String addTask(String userInput, UserData userData) {
        if (userInput.length() <= 5) {
            return "Упс\uD83D\uDE05, похоже вы забыли указать задачу после команды /add\nНапример: /add Полить цветы";
        }

        String task = userInput.substring(5).trim();
        if (task.isEmpty()) {
            return "Задача не может быть пустой!";
        }

        if (userData.getTasks().contains(task)) {
            return "Задача \"" + task + "\" уже есть в списке!";
        }

        userData.getTasks().add(task);
        return "Задача \"" + task + "\" добавлена!";
    }

    private String showTasks(UserData userData) {
        if (userData.getTasks().isEmpty()) {
            return "Список задач пуст!";
        }

        StringBuilder sb = new StringBuilder("Вот список ваших задач:\n");
        for (int i = 0; i < userData.getTasks().size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(userData.getTasks().get(i)).append("\n");
        }
        return sb.toString();
    }

    private String markTaskDone(String userInput, UserData userData) {
        if (userInput.length() <= 6) {
            return "Упс\uD83D\uDE05, похоже вы забыли указать задачу после команды /done\nНапример: /done Полить цветы";
        }

        String task = userInput.substring(6).trim();
        if (!userData.getTasks().contains(task)) {
            return "Задача \"" + task + "\" не найдена в списке!";
        }

        userData.getTasks().remove(task);
        userData.getCompletedTasks().add(task);
        return "Задача \"" + task + "\" отмечена выполненной!";
    }

    private String showCompletedTasks(UserData userData) {
        if (userData.getCompletedTasks().isEmpty()) {
            return "Список выполненных задач пуст!";
        }

        StringBuilder sb = new StringBuilder("✅ Вот список выполненных задач:\n");
        for (int i = 0; i < userData.getCompletedTasks().size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(userData.getCompletedTasks().get(i)).append(" ✔\n");
        }
        return sb.toString();
    }
    private String deleteTask(String userInput, UserData userData) {
        if (userInput.length() <= 8) {
            return "Упс\uD83D\uDE05, похоже вы забыли указать задачу после команды /delete.\nНапример: /delete Полить цветы";
        }

        String task = userInput.substring(8).trim();
        if (!userData.getTasks().contains(task)) {
            return "Задача \"" + task + "\" не найдена в списке!";
        }

        userData.getTasks().remove(task);
        return "🗑️ Задача \"" + task + "\" удалена из списка задач!";
    }

    private String getStartMessage() {
        return "Добро пожаловать в планировщик задач! \uD83D\uDC31 📝 \n" +
                "Я могу организовывать ваши задачи.\n" +
                "Команды: \n" +
                "/add - добавить задачу\n" +
                "/tasks - показать список задач\n" +
                "/done - отметить выполненной\n" +
                "/dTask - список выполненных задач\n" +
                "/delete - удалить задачу\n" +
                "/help - помощь\n";
    }

    private String getHelpMessage() {
        return "Справка по работе:\n" +
                "Я планировщик задач😊 📝\n" +
                "Мои команды: \n" +
                "/add - добавить задачу\n" +
                "/tasks - показать список задач\n" +
                "/done - отметить выполненной\n" +
                "/dTask - список выполненных задач\n" +
                "/delete - удалить задачу\n" +
                "/help - помощь\n" +
                "\n" +
                "Например: \n" +
                "/add Полить цветы\n" +
                "- Задача \"Полить цветы\" добавлена!\n\n" +
                "/add Накормить кота\n" +
                "- Задача \"Накормить кота\" добавлена!\n\n" +
                "/add Полить цветы\n" +
                "- Задача \"Полить цветы\" уже есть в списке!\n\n" +
                "/tasks\n" +
                "- Вот список ваших задач:\n" +
                "  1. Полить цветы\n" +
                "  2. Накормить кота\n\n" +
                "/done Полить цветы\n" +
                "- Задача \"Полить цветы\" отмечена выполненной!\n\n" +
                "/dTask\n" +
                "- ✅ Вот список выполненных задач:\n" +
                "  1. Полить цветы ✔\n\n" +
                "/delete Накормить кота\n" +
                "- 🗑️ Задача \"Накормить кота\" удалена из списка задач!";
    }
}