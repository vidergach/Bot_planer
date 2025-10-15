package org.example;

import java.io.*;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс для работы с файлами задач.
 * Обеспечивает экспорт и импорт задач в формате JSON с использованием библиотеки Jackson.
 */
public class FileWork {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Экспортирует списки задач в JSON файл.
     * Создает файл с указанным именем, содержащий текущие и выполненные задачи.
     * Автоматически добавляет расширение .json если оно отсутствует.
     *
     * @param userId идентификатор пользователя
     * @param tasks список текущих задач для экспорта
     * @param completed_tasks список выполненных задач для экспорта
     * @param filename имя файла для экспорта
     * @return File объект созданного файла с экспортированными задачами
     * @throws IOException если произошла ошибка ввода-вывода при создании файла
     */
    public File Export(String userId, List<String> tasks, List<String> completed_tasks, String filename) throws IOException{
        if (!filename.endsWith(".json")){
            filename+=".json";
        }

        File file = new File(filename);

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)){
            StringBuilder jsonBuilder = new StringBuilder();

            jsonBuilder.append("{\n");

            //текущие
            jsonBuilder.append("  \"current_tasks\": [\n");
            for(int i=0; i<tasks.size(); i++){
                jsonBuilder.append("    \"").append(Json(tasks.get(i))).append("\"");
                if (i<tasks.size()-1){
                    jsonBuilder.append(",");
                }
                jsonBuilder.append("\n");
            }
            jsonBuilder.append("  ],\n");

            //выполненные
            jsonBuilder.append("  \"completed_tasks\": [\n");
            for(int i=0; i<completed_tasks.size(); i++){
                jsonBuilder.append("    \"").append(Json(completed_tasks.get(i))).append("\"");
                if(i<completed_tasks.size()-1){
                    jsonBuilder.append(",");
                }
                jsonBuilder.append("\n");
            }
            jsonBuilder.append("  ]\n");
            jsonBuilder.append("}");

            writer.write(jsonBuilder.toString());
        }
        return file;
    }

    /**
     * Импортирует задачи из JSON файла.
     * Использует библиотеку Jackson для парсинга JSON структуры.
     * Извлекает списки текущих и выполненных задач.
     *
     * @param file файл в формате JSON для импорта
     * @return FileData объект, содержащий импортированные списки задач
     * @throws IOException если файл не существует, недоступен для чтения или имеет неверный формат
     */
    public FileData Import(File file) throws IOException {
        List<String> tasks = new ArrayList<>();
        List<String> completed_tasks = new ArrayList<>();

        try {
            JsonNode save = objectMapper.readTree(file);
            System.out.println("=== СОДЕРЖИМОЕ ФАЙЛА ===");
            System.out.println(save);
            System.out.println("=== КОНЕЦ СОДЕРЖИМОГО ===");

            // Получаем текущие задачи
            JsonNode currentNode = save.get("current_tasks");
            if (currentNode != null) {
                for (JsonNode taskNode : currentNode) {
                    tasks.add(taskNode.asText());
                }
            }

            // Получаем выполненные задачи
            JsonNode completedNode = save.get("completed_tasks");
            if (completedNode != null) {
                for (JsonNode taskNode : completedNode) {
                        completed_tasks.add(taskNode.asText());
                }
            }
        } catch (IOException e) {
            throw new IOException("Ошибка");
        }
        return new FileData(tasks, completed_tasks);
    }

    /**
     * Экранирует специальные символы для JSON.
     * Преобразует специальные символы в их экранированные последовательности.
     *
     * @param text исходный текст для экранирования
     * @return String экранированная строка, готовая для вставки в JSON
     */
    private String Json(String text){
        return text.replace("\\","\\\\")
                .replace("\"","\\\"")
                .replace("\n","\\n");
    }
    /**
     * Восстанавливает специальные символы из JSON.
     * Преобразует экранированные последовательности обратно в обычные символы.
     *
     * @param text экранированная строка из JSON
     * @return String восстановленная строка с обычными символами
     */
    private String unJson(String text){
        return text.replace("\\\"","\"")
                .replace("\\\\","\\")
                .replace("\\n","\n");
    }
    /**
     * Удаляет указанный файл.
     * Выполняет безопасное удаление файла с проверкой существования.
     *
     * @param file файл для удаления
     */
    public void Delete(File file){
        if(file != null && file.exists()){
            file.delete();
        }
    }
    /**
     * Внутренний класс для хранения импортированных данных.
     * Содержит списки текущих и выполненных задач.
     */
    public class FileData{
        public List<String> tasks;
        public List<String> completed_tasks;

        public FileData(List<String> tasks, List<String> completed_tasks){
            this.tasks=tasks;
            this.completed_tasks=completed_tasks;
        }
    }
}
