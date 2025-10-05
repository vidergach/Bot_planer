package org.example;

import java.util.ArrayList;
import java.util.List;

class UserTasks {
    private final List<String> tasks = new ArrayList<>();
    private final List<String> completedTasks = new ArrayList<>();
    public List<String> getTasks() { return tasks; }
    public List<String> getCompletedTasks() { return completedTasks; }


}
