package org.example;

import java.util.ArrayList;
import java.util.List;

/**
 * Умный менеджер данных пользователя.
 * Инкапсулирует логику работы с задачами
 *
 * @author Vika
 * @version 1.0
 */
public class UserData {
    private final List<String> tasks = new ArrayList<>();
    private final List<String> completedTasks = new ArrayList<>();

    /**
     * Возвращает копию списка текущих задач для предотвращения "гонки данных"
     * Мы не даем доступ к оригиналу, а для каждого пользователя формируем свой
     * список с помощью копирования
     */
    public List<String> getTasks() {
        return new ArrayList<>(tasks);
    }

    /**
     * Возвращает копию списка выполненных задач для предотвращения "гонки данных"
     */
    public List<String> getCompletedTasks() {
        return new ArrayList<>(completedTasks);
    }

    /**
     * Добавляет новую задачу.
     *
     * @param task текст задачи
     */
    public String addTask(String task) {
        String trimmedTask = task.trim();
        if (tasks.contains(trimmedTask)) {
            return "Задача \"" + trimmedTask + "\" уже есть в списке!";
        }
        tasks.add(trimmedTask);
        return "Задача \"" + trimmedTask + "\" добавлена!";
    }

    /**
     * Отмечает задачу как выполненную.
     * @param task текст задачи
     * @throws IllegalArgumentException если задача пустая
     * @throws IllegalStateException если задача не найдена
     */
    public String markTaskDone(String task) {
        String trimmedTask = task.trim();
        if (!tasks.contains(trimmedTask)) {
            return "Задача \"" + trimmedTask + "\" не найдена в списке!";
        }
        tasks.remove(trimmedTask);
        completedTasks.add(trimmedTask);
        return "Задача \"" + trimmedTask + "\" отмечена выполненной!";
    }

    /**
     * Удаляет задачу.
     *
     * @param task текст задачи
     */
    public String deleteTask(String task) {
        String trimmedTask = task.trim();
        if (!tasks.contains(trimmedTask)) {
            return "Задача \"" + trimmedTask + "\" не найдена в списке!";
        }
        tasks.remove(trimmedTask);
        return "🗑️ Задача \"" + trimmedTask + "\" удалена из списка задач!";
    }

    /**
     * Проверяет, есть ли текущие задачи.
     */
    public boolean hasTasks() {
        return !tasks.isEmpty();
    }

    /**
     * Проверяет, есть ли выполненные задачи.
     */
    public boolean hasCompletedTasks() {
        return !completedTasks.isEmpty();
    }
}