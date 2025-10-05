package org.example;

import java.util.ArrayList;
import java.util.List;

class UserData {
    private List<String> tasks = new ArrayList<>();
    private List<String> completedTasks = new ArrayList<>();

    public List<String> getTasks() { return tasks; }
    public List<String> getCompletedTasks() { return completedTasks; }
}