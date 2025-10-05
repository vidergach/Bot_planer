package org.example;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс для хранения данных пользователя.
 * Содержит списки текущих и выполненных задач.
 * Обеспечивает изоляцию данных между разными пользователями.
 *
 * @see ArrayList
 * @see List
 */
class UserData {
    private List<String> tasks = new ArrayList<>();
    private List<String> completedTasks = new ArrayList<>();

    /**
     * Возвращает список текущих задач пользователя.
     *
     * @return список текущих задач (может быть пустым)
     */
    public List<String> getTasks() { return tasks; }

    /**
     * Возвращает список выполненных задач пользователя.
     *
     * @return список выполненных задач (может быть пустым)
     */
    public List<String> getCompletedTasks() { return completedTasks; }
}