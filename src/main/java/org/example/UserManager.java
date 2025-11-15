package org.example;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Менеджер пользователей для аутентификации и синхронизации между платформами
 */
public class UserManager {
    private final Map<String, String> userPasswords = new ConcurrentHashMap<>();
    private final Map<String, String> platformToUsername = new ConcurrentHashMap<>();

    /**
     * Регистрирует нового пользователя
     *
     * @param username имя пользователя для регистрации
     * @param password пароль пользователя
     * @return true если регистрация прошла успешно, false если пользователь уже существует
     */
    public synchronized boolean registerUser(String username, String password) {
        if (userPasswords.containsKey(username)) {
            return false;
        }
        userPasswords.put(username, password);
        return true;
    }

    /**
     * Аутентифицирует пользователя
     *
     * @param username имя пользователя для аутентификации
     * @param password пароль для проверки
     * @param platformId уникальный идентификатор платформы
     * @return true если аутентификация прошла успешно, false в противном случае
     */
    public boolean authenticateUser(String username, String password, String platformId) {
        String storedPassword = userPasswords.get(username);
        if (storedPassword != null && storedPassword.equals(password)) {
            platformToUsername.put(platformId, username);
            return true;
        }
        return false;
    }

    /**
     * Выход пользователя из системы
     *
     * @param platformId идентификатор платформы
     * @return true если пользователь был авторизован, false в противном случае
     */
    public boolean outUser(String platformId) {
        return platformToUsername.remove(platformId) != null;
    }

    /**
     * Получает имя пользователя по platformId
     *
     * @param platformId идентификатор платформы
     * @return имя пользователя или null если привязка не найдена
     */
    public String getUsername(String platformId) {
        return platformToUsername.get(platformId);
    }

    /**
     * Проверяет, зарегистрирован ли пользователь
     *
     * @param username имя пользователя для проверки
     * @return true если пользователь зарегистрирован, false в противном случае
     */
    public boolean isUserRegistered(String username) {
        return userPasswords.containsKey(username);
    }
}