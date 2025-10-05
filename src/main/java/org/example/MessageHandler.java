package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageHandler {
    private final List<String> tasks = new ArrayList<>();//надо сделать
    private final List<String> completedTasks = new ArrayList<>();//сделано задач
    public String processUserInput(String userInput, String userId) {
        System.out.println("сообщение: " + userInput + " от: " + userId);
        String outputText = Response(userInput);
        System.out.println("Ответ: " + outputText);
        return outputText;
    }

    private String Response(String userInput) {
        if ("/start".equals(userInput)) {
            return startMessage();
        } else if ("/help".equals(userInput)) {
            return helpMessage();
        } else if (userInput.startsWith("/add")) {
            return addTask(userInput);
        } else if ("/tasks".equals(userInput)) {
            return showTasks();
        } else if (userInput.startsWith("/delete")) {
            return deleteTask(userInput);
        } else {
            return "Неизвестная команда.\n" +
                    "Введите /help для просмотра доступных команд.";
        }
    }
    private String addTask(String userInput) {
        if (userInput.length() <= 5) {
            return "Упс\uD83D\uDE05, похоже вы " +
                    "забыли указать задачу после команды /add \n" +
                    "Например: /add Полить цветы";
        }
        String task = userInput.substring(5).trim();
        if (task.isEmpty()) {
            return "Задача не может быть пустой!";
        }

        // Проверка на существующую задачу
        if (tasks.contains(task)) {
            return "Задача \"" + task + "\" уже есть в списке!";
        }

        tasks.add(task);
        return "Задача \"" + task + "\" добавлена!";
    }

    private String showTasks() {
        if (tasks.isEmpty())
            return "Список задач пуст!";
        String list_tasks = "Вот список ваших задач:\n";
        for (int i = 0; i < tasks.size(); i++) {
            list_tasks += "  " + (i + 1) + ". " + tasks.get(i) + "\n";
        }
        return list_tasks;
    }

    private String deleteTask(String userInput) {
        if (userInput.length() <= 8) {
            return "Упс\uD83D\uDE05, похоже вы забыли указать задачу после команды /delete.\n" +
                    "Например: /delete Полить цветы";
        }
        String task = userInput.substring(8).trim();
        if (!tasks.contains(task)) {
            return "Задача \"" + task + "\" не найдена в списке!";
        }
        tasks.remove(task);
        return "🗑️ Задача \"" + task + "\" удалена из списка задач!";
    }
    private String startMessage() {
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

    private String helpMessage() {
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
