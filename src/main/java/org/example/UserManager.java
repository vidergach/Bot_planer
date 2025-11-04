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
     * Получает имя пользователя по platformId
     */
    public String getUsername(String platformId) {
        return platformToUsername.get(platformId);
    }

    /**
     * Проверяет, зарегистрирован ли пользователь
     */
    public boolean isUserRegistered(String username) {
        return userPasswords.containsKey(username);
    }
}