package org.example;

import java.io.*;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;
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
     * @param tasks список текущих задач для экспорта
     * @param completedTasks список выполненных задач для экспорта
     * @param filename имя файла для экспорта
     * @return File объект созданного файла с экспортированными задачами
     * @throws IOException если произошла ошибка ввода-вывода при создании файла
     */
    public File export(List<String> tasks, List<String> completedTasks, String filename) throws IOException {
        if (!filename.endsWith(".json")) {
            filename += ".json";
        }
        File file = new File(filename);
        try {
            FileData exportData = new FileData(
                    tasks != null ? tasks : new ArrayList<>(),
                    completedTasks != null ? completedTasks : new ArrayList<>()
            );
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, exportData);

            return file;
        } catch (IOException e) {
            throw new IOException("Ошибка экспорта задач в файл: " + filename, e);
        }
    }

    /**
     * Импортирует задачи из JSON файла.
     * Использует библиотеку Jackson для парсинга JSON структуры.
     * Извлекает списки текущих и выполненных задач.
     *
     * @return FileData объект, содержащий импортированные списки задач
     * @throws IOException если файл не существует, недоступен для чтения или имеет неверный формат
     */
    public FileData importData(InputStream inputStream) throws IOException {
        try {
            return objectMapper.readValue(inputStream, FileData.class);
        } catch (IOException e) {
            throw new IOException("Ошибка при чтении файла: " + e.getMessage(), e);
        }
    }

    /**
     * Record для хранения данных файла задач.
     *
     * @param current_tasks список текущих задач
     * @param completed_tasks список выполненных задач
     */
    public record FileData(
            List<String> current_tasks,
            List<String> completed_tasks
    )
    {
        /**
         * Конструктор, гарантирует, что поля никогда не будут null.
         */
        public FileData {
            current_tasks = current_tasks != null ? new ArrayList<>(current_tasks) : new ArrayList<>();
            completed_tasks = completed_tasks != null ? new ArrayList<>(completed_tasks) : new ArrayList<>();
        }

    }
}
