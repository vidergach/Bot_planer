package org.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService {

    private final String databaseUrl;

    public DatabaseService(String dbUrl) {
        this.databaseUrl = dbUrl;
        initializeDatabase();
    }

    public DatabaseService() {
        this("jdbc:sqlite:tasks.db");
    }

    public class TaskData {
        private final List<String> currentTasks;
        private final List<String> completedTasks;

        public TaskData(List<String> currentTasks, List<String> completedTasks) {
            if (currentTasks != null) {
                this.currentTasks = new ArrayList<>(currentTasks);
            } else {
                this.currentTasks = new ArrayList<>();
            }

            if (completedTasks != null) {
                this.completedTasks = new ArrayList<>(completedTasks);
            } else {
                this.completedTasks = new ArrayList<>();
            }
        }

        public List<String> getCurrentTasks() {
            return new ArrayList<>(currentTasks);
        }

        public List<String> getCompletedTasks() {
            return new ArrayList<>(completedTasks);
        }
    }

    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(databaseUrl)) {
            conn.setAutoCommit(false);

            String createUsers = """
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT UNIQUE NOT NULL,
                        password TEXT NOT NULL,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                    );
                    """;

            String createCurrentTasks = """
                    CREATE TABLE IF NOT EXISTS user_tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL,
                        task_text TEXT NOT NULL,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        UNIQUE(user_id, task_text)
                    );
                    """;

            String createCompletedTasks = """
                    CREATE TABLE IF NOT EXISTS completed_tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL,
                        task_text TEXT NOT NULL,
                        completed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        UNIQUE(user_id, task_text)
                    );
                    """;

            String createUserSessions = """
                    CREATE TABLE IF NOT EXISTS user_sessions (
                        platform_type TEXT NOT NULL,
                        platform_id TEXT NOT NULL,
                        user_id INTEGER NOT NULL,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        UNIQUE(platform_type, platform_id)
                    );
                    """;

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createUsers);
                stmt.execute(createCurrentTasks);
                stmt.execute(createCompletedTasks);
                stmt.execute(createUserSessions);
            }
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка - не удалось инициализировать базу данных", e);
        }
    }

    public void addTask(String userId, String taskText) throws SQLException {
        String sql = "INSERT INTO user_tasks (user_id, task_text) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(databaseUrl);
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, userId);
            preparedStatement.setString(2, taskText.trim());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 19) {
                throw new IllegalStateException("Задача \"" + taskText + "\" уже есть в списке!");
            }
            throw e;
        }
    }

    public void markTaskDone(String userId, String taskText) throws SQLException {
        String deleteSql = "DELETE FROM user_tasks WHERE user_id = ? AND task_text = ?";
        String insertSql = "INSERT INTO completed_tasks (user_id, task_text) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(databaseUrl)) {
            conn.setAutoCommit(false);

            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
                 PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

                deleteStmt.setString(1, userId);
                deleteStmt.setString(2, taskText.trim());
                int deleted = deleteStmt.executeUpdate();

                if (deleted == 0) {
                    throw new IllegalStateException("Задача \"" + taskText + "\" не найдена в списке!");
                }

                insertStmt.setString(1, userId);
                insertStmt.setString(2, taskText.trim());
                insertStmt.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void deleteTask(String userId, String taskText) throws SQLException {
        String sql = "DELETE FROM user_tasks WHERE user_id = ? AND task_text = ?";
        try (Connection conn = DriverManager.getConnection(databaseUrl);
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, userId);
            preparedStatement.setString(2, taskText.trim());
            int deleted = preparedStatement.executeUpdate();

            if (deleted == 0) {
                throw new IllegalStateException("Задача \"" + taskText + "\" не найдена в списке!");
            }
        }
    }

    public List<String> getCurrentTasks(String userId) throws SQLException {
        return getTasks(userId, "user_tasks");
    }

    public List<String> getCompletedTasks(String userId) throws SQLException {
        return getTasks(userId, "completed_tasks");
    }

    private List<String> getTasks(String userId, String tableName) throws SQLException {
        List<String> tasks = new ArrayList<>();
        String sql = "SELECT task_text FROM " + tableName + " WHERE user_id = ? ORDER BY id";

        try (Connection conn = DriverManager.getConnection(databaseUrl);
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, userId);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                tasks.add(rs.getString("task_text"));
            }
        }
        return tasks;
    }

    public boolean registerUser(String username, String password) throws SQLException {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(databaseUrl);
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, username.trim());
            preparedStatement.setString(2, password);
            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 19) {
                return false;
            }
            throw e;
        }
    }

    public boolean authenticateUser(String username, String password, String platformType, String platformId) throws SQLException {
        String userSql = "SELECT id FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DriverManager.getConnection(databaseUrl)) {
            conn.setAutoCommit(false);

            String userId = null;
            try (PreparedStatement userStmt = conn.prepareStatement(userSql)) {
                userStmt.setString(1, username.trim());
                userStmt.setString(2, password);
                ResultSet rs = userStmt.executeQuery();
                if (rs.next()) {
                    userId = rs.getString("id");
                } else {
                    return false;
                }
            }

            String sessionSql = "INSERT OR REPLACE INTO user_sessions (platform_type, platform_id, user_id) VALUES (?, ?, ?)";
            try (PreparedStatement sessionStmt = conn.prepareStatement(sessionSql)) {
                sessionStmt.setString(1, platformType);
                sessionStmt.setString(2, platformId);
                sessionStmt.setString(3, userId);
                sessionStmt.executeUpdate();
            }
            conn.commit();
            return true;
        }
    }

    public String getUsername(String platformType, String platformId) throws SQLException {
        String sql = """
                SELECT u.username 
                FROM users u 
                JOIN user_sessions us ON u.id = us.user_id 
                WHERE us.platform_type = ? AND us.platform_id = ?
                """;
        try (Connection conn = DriverManager.getConnection(databaseUrl);
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, platformType);
            preparedStatement.setString(2, platformId);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                return rs.getString("username");
            }
            return null;
        }
    }

    public String getUserIdByPlatform(String platformId) throws SQLException {
        String sql = "SELECT user_id FROM user_sessions WHERE platform_id = ?";
        try (Connection conn = DriverManager.getConnection(databaseUrl);
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, platformId);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                return rs.getString("user_id");
            }
            return null;
        }
    }

    public TaskData exportTasks(String userId) throws SQLException {
        List<String> currentTasks = getCurrentTasks(userId);
        List<String> completedTasks = getCompletedTasks(userId);
        return new TaskData(currentTasks, completedTasks);
    }

    public boolean userExists(String username) throws SQLException {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(databaseUrl);
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, username.trim());
            ResultSet rs = preparedStatement.executeQuery();
            return rs.next();
        }
    }

    public boolean isUserLinkedToOtherPlatform(String userId, String currentPlatformType) throws SQLException {
        String sql = "SELECT COUNT(*) FROM user_sessions WHERE user_id = ? AND platform_type != ?";
        try (Connection conn = DriverManager.getConnection(databaseUrl);
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, userId);
            preparedStatement.setString(2, currentPlatformType);
            ResultSet rs = preparedStatement.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }
}