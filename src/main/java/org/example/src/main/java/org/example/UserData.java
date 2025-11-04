package org.example;

import java.util.ArrayList;
import java.util.List;

/**
 * Инкапсулирует логику работы с задачами
 */
public class UserData {
    private final List<String> tasks = new ArrayList<>();
    private final List<String> completedTasks = new ArrayList<>();

    /**
     * Возвращает копию списка текущих задач
     */
    public List<String> getTasks() {
        return new ArrayList<>(tasks);
    }

    /**
     * Возвращает копию списка выполненных задач
     */
    public List<String> getCompletedTasks() {
        return new ArrayList<>(completedTasks);
    }

    /**
     * Добавляет новую задачу.
     */
    public void addTask(String task) {
        String trimmedTask = task.trim();
        if (tasks.contains(trimmedTask)) {
            throw new IllegalStateException("Задача \"" + trimmedTask + "\" уже есть в списке!");
        }
        tasks.add(trimmedTask);
    }

    /**
     * Отмечает задачу как выполненную.
     */
    public void markTaskDone(String task) {
        String trimmedTask = task.trim();
        if (!tasks.contains(trimmedTask)) {
            throw new IllegalStateException("Задача \"" + trimmedTask + "\" не найдена в списке!");
        }
        tasks.remove(trimmedTask);
        completedTasks.add(trimmedTask);
    }

    /**
     * Удаляет задачу.
     */
    public void deleteTask(String task) {
        String trimmedTask = task.trim();
        if (!tasks.contains(trimmedTask)) {
            throw new IllegalStateException("Задача \"" + trimmedTask + "\" не найдена в списке!");
        }
        tasks.remove(trimmedTask);
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