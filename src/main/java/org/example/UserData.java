package org.example;

import java.util.ArrayList;
import java.util.List;

/**
 * Инкапсулирует логику работы с задачами
 * Класс управляет двумя списками задач: текущими и выполненными.
 * Предоставляет методы для работы с задачами.
 */
public class UserData {
    private final List<String> tasks = new ArrayList<>();
    private final List<String> completedTasks = new ArrayList<>();

    /**
     * Возвращает копию списка текущих задач
     *
     * @return новый список, содержащий все текущие задачи
     */
    public List<String> getTasks() {
        return new ArrayList<>(tasks);
    }

    /**
     * Возвращает копию списка выполненных задач
     *
     * @return новый список, содержащий все выполненные задачи
     */
    public List<String> getCompletedTasks() {
        return new ArrayList<>(completedTasks);
    }

    /**
     * Добавляет новую задачу.
     *
     * @param task описание задачи для добавления
     * @throws IllegalStateException если задача уже существует в списке
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
     *
     * @param task описание задачи для отметки как выполненной
     * @throws IllegalStateException если задача не найдена в списке текущих задач
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
     *
     * @param task описание задачи для удаления
     * @throws IllegalStateException если задача не найдена в списке текущих задач
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
     *
     * @return true если есть хотя бы одна текущая задача, false в противном случае
     */
    public boolean hasTasks() {
        return !tasks.isEmpty();
    }

    /**
     * Проверяет, есть ли выполненные задачи.
     *
     * @return true если есть хотя бы одна выполненная задача, false в противном случае
     */
    public boolean hasCompletedTasks() {
        return !completedTasks.isEmpty();
    }
}