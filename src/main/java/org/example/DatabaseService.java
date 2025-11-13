package org.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для работы с базой данных приложения планировщика задач.
 * Обеспечивает все операции с пользователями и задачами.
 */
public class DatabaseService {

    private final String databaseUrl;

    /**
     * Конструктор с указанием URL базы данных.
     *
     * @param dbUrl URL базы данных SQLite
     */
    public DatabaseService(String dbUrl) {
        this.databaseUrl = dbUrl;
        initializeDatabase();
    }

    /**
     * Конструктор по умолчанию, использующий базу данных "tasks.db".
     */
    public DatabaseService() {
        this("jdbc:sqlite:tasks.db");
    }

    /**
     * Класс для хранения данных о задачах пользователя.
     * Содержит раздельные списки текущих и выполненных задач.
     */
    public class TaskData {
        private final List<String> currentTasks;
        private final List<String> completedTasks;

        /**
         * Конструктор для создания объекта с данными задач.
         *
         * @param currentTasks список текущих задач
         * @param completedTasks список выполненных задач
         */
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

        /**
         * Возвращает копию списка текущих задач.
         *
         * @return список текущих задач
         */
        public List<String> getCurrentTasks() {
            return new ArrayList<>(currentTasks);
        }

        /**
         * Возвращает копию списка выполненных задач.
         *
         * @return список выполненных задач
         */
        public List<String> getCompletedTasks() {
            return new ArrayList<>(completedTasks);
        }
    }

    /**
     * Инициализирует базу данных, создавая необходимые таблицы если они не существуют.
     *
     * @throws RuntimeException если произошла ошибка
     */
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

    /**
     * Добавляет новую задачу для указанного пользователя.
     *
     * @param userId идентификатор пользователя
     * @param taskText текст задачи
     * @throws IllegalStateException если задача с таким текстом уже существует
     */
    public void addTask(String userId, String taskText)  {
        String sql = "INSERT INTO user_tasks (user_id, task_text) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(databaseUrl);
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, userId);
            preparedStatement.setString(2, taskText.trim());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Отмечает задачу как выполненную.
     *
     * @param userId идентификатор пользователя
     * @param taskText текст задачи
     * @throws SQLException если произошла ошибка при работе с базой данных
     * @throws IllegalStateException если задача не найдена в списке текущих задач
     */
    public void markTaskDone(String userId, String taskText) throws SQLException {
        String deleteSql = "DELETE FROM user_tasks WHERE user_id = ? AND task_text = ?";
        String insertSql = "INSERT INTO completed_tasks (user_id, task_text) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(databaseUrl)) {
            conn.setAutoCommit(false);

            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
                 PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

                deleteStmt.setString(1, userId);
                deleteStmt.setString(2, taskText.trim());
                int deletedRows = deleteStmt.executeUpdate();

                if (deletedRows > 0) {
                    insertStmt.setString(1, userId);
                    insertStmt.setString(2, taskText.trim());
                    insertStmt.executeUpdate();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Удаляет задачу из списка текущих задач пользователя.
     *
     * @param userId идентификатор пользователя
     * @param taskText текст задачи
     * @throws SQLException если произошла ошибка при работе с базой данных
     */
    public void deleteTask(String userId, String taskText) throws SQLException {
        String sql = "DELETE FROM user_tasks WHERE user_id = ? AND task_text = ?";
        try (Connection conn = DriverManager.getConnection(databaseUrl);
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, userId);
            preparedStatement.setString(2, taskText.trim());
            preparedStatement.executeUpdate();
        }
    }

    /**
     * Возвращает список текущих задач пользователя.
     *
     * @param userId идентификатор пользователя
     * @return список текущих задач
     * @throws SQLException если произошла ошибка при работе
     */
    public List<String> getCurrentTasks(String userId) throws SQLException {
        return getTasks(userId, "user_tasks");
    }

    /**
     * Возвращает список выполненных задач пользователя.
     *
     * @param userId идентификатор пользователя
     * @return список выполненных задач
     * @throws SQLException если произошла ошибка при работе
     */
    public List<String> getCompletedTasks(String userId) throws SQLException {
        return getTasks(userId, "completed_tasks");
    }

    /**
     * Метод для получения задач из указанной таблицы.
     *
     * @param userId идентификатор пользователя
     * @param tableName имя таблицы
     * @return список задач
     * @throws SQLException если произошла ошибка при работе
     */
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

    /**
     * Регистрирует нового пользователя в системе.
     *
     * @param username имя пользователя
     * @param password пароль
     * @return true если регистрация прошла успешно, false если пользователь с таким именем уже существует
     * @throws SQLException если произошла ошибка при работе
     */
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

    /**
     * Аутентифицирует пользователя и создает сессию для платформы.
     *
     * @param username имя пользователя
     * @param password пароль
     * @param platformType тип платформы
     * @param platformId идентификатор
     * @return true если аутентификация прошла успешно, false в противном случае
     * @throws SQLException если произошла ошибка при работе
     */
    public boolean authenticateUser(String username, String password, String platformType, String platformId) throws SQLException {
        String userSql = "SELECT id FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DriverManager.getConnection(databaseUrl)) {
            conn.setAutoCommit(false);

            String userId;
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

    /**
     * Возвращает имя пользователя по идентификатору платформы.
     *
     * @param platformType тип платформы
     * @param platformId идентификатор
     * @return имя пользователя
     * @throws SQLException если произошла ошибка при работе
     */
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

    /**
     * Возвращает внутренний идентификатор пользователя по идентификатору платформы.
     *
     * @param platformId идентификатор платформы
     * @return внутренний идентификатор пользователя
     * @throws SQLException если произошла ошибка при работе
     */
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

    /**
     * Экспортирует все задачи пользователя в объект TaskData.
     *
     * @param userId идентификатор пользователя
     * @return объект TaskData с текущими и выполненными задачами
     * @throws SQLException если произошла ошибка при работе
     */
    public TaskData exportTasks(String userId) throws SQLException {
        List<String> currentTasks = getCurrentTasks(userId);
        List<String> completedTasks = getCompletedTasks(userId);
        return new TaskData(currentTasks, completedTasks);
    }

    /**
     * Проверяет существование пользователя с указанным именем.
     *
     * @param username имя пользователя для проверки
     * @return true если пользователь существует, false в противном случае
     * @throws SQLException если произошла ошибка при работе
     */
    public boolean userExists(String username) throws SQLException {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(databaseUrl);
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, username.trim());
            ResultSet rs = preparedStatement.executeQuery();
            return rs.next();
        }
    }
}