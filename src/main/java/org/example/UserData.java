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
        return tasks;
    }

    /**
     * Возвращает копию списка выполненных задач для предотвращения случайных изменений
     */
    public List<String> getCompletedTasks() {
        return completedTasks;
    }

    /**
     * Добавляет новую задачу.
     *
     * @param task текст задачи
     */
    public void addTask(String task) {
        String trimmedTask = task.trim();
        if (tasks.contains(trimmedTask)) {
            throw new IllegalStateException
                    ("Задача \"" + trimmedTask + "\" уже есть в списке!");
        }
        tasks.add(trimmedTask);
    }

    /**
     * Отмечает задачу как выполненную.
     * @param task текст задачи
     * @throws IllegalArgumentException если задача пустая
     * @throws IllegalStateException если задача не найдена
     */
    public void markTaskDone(String task) {
        String trimmedTask = task.trim();
        if (!tasks.contains(trimmedTask)) {
            throw new IllegalStateException
                    ("Задача \"" + trimmedTask + "\" не найдена в списке!");
        }
        tasks.remove(trimmedTask);
        completedTasks.add(trimmedTask);
    }

    /**
     * Удаляет задачу.
     *
     * @param task текст задачи
     */
    public void deleteTask(String task) {
        String trimmedTask = task.trim();
        if (!tasks.contains(trimmedTask)) {
            throw new IllegalStateException
                    ("Задача \"" + trimmedTask + "\" не найдена в списке!");
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