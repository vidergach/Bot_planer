package org.example;

import java.util.ArrayList;
import java.util.List;

public class UserTasks {
    private final List<String> tasks = new ArrayList<>(); // задачи
    private final List<String> completedTasks = new ArrayList<>(); // выполненные задачи

    public List<String> getTasks() {
        return tasks;
    }

    public List<String> getCompletedTasks() {
        return completedTasks;
    }
}