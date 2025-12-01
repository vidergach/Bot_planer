package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * OpenRouterClient - класс работы с ИИ.
 */
public class OpenRouterClient {
    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenRouterClient(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * отправка запроса через OpenRouter.
     */
    public String sendRequest(String userPrompt) throws IOException, InterruptedException {
        String requestBody = createRequestBody(userPrompt);
        HttpRequest request = createHttpRequest(requestBody);

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return processResponse(response);
    }

    private String createRequestBody(String userPrompt) {
        return String.format("""
                    {
                      "model": "gpt-3.5-turbo",
                      "messages": [
                        {"role": "system", "content": "You are a helpful assistant that breaks down tasks into subtasks. Respond with a clear list of subtasks, one per line, without numbering or bullet points."},
                        {"role": "user", "content": "%s"}
                      ]
                    }
                """, userPrompt.replace("\"", "\\\""));
    }

    private HttpRequest createHttpRequest(String requestBody) {
        return HttpRequest.newBuilder()
                .uri(URI.create("https://openrouter.ai/api/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("HTTP-Referer", "https://example.com")
                .header("X-Title", "Task Planner Bot")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    private String processResponse(HttpResponse<String> response) throws IOException {
        System.out.println("Status code: " + response.statusCode());

        JsonNode json = objectMapper.readTree(response.body());

        if (response.statusCode() != 200) {
            String errorMessage = "HTTP Error: " + response.statusCode();
            if (json.has("error")) {
                JsonNode error = json.get("error");
                errorMessage += " - " + error.get("message").asText();
                if (error.has("type")) {
                    errorMessage += " (Type: " + error.get("type").asText() + ")";
                }
            } else if (json.has("message")) {
                errorMessage += " - " + json.get("message").asText();
            }
            throw new RuntimeException(errorMessage);
        }

        if (!json.has("choices")) {
            throw new RuntimeException("Error: 'choices' field not found in response.");
        }

        return json.get("choices")
                .get(0)
                .get("message")
                .get("content")
                .asText()
                .trim();
    }
}