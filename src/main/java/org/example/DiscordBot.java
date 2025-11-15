package org.example;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.InputStream;
import java.util.List;

/**
 * Discord бот для управления задачами через Discord.
 * Этот класс реализует Discord бота, который взаимодействует с пользователями
 * через текстовые сообщения и файловые вложения для управления задачами.
 */
public class DiscordBot extends ListenerAdapter {
    private final String token;
    private final MessageHandler logic;

    public DiscordBot(String token, MessageHandler logic) {
        this.token = token;
        this.logic = logic;
    }

    /**
     * Запускает Discord бота и инициализирует подключение к Discord API.
     */
    public void start() {
        try {
            JDA jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(this)
                    .build();
            System.out.println("Discord бот запущен");
        } catch (Exception e) {
            System.err.println("Ошибка запуска Discord бота: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot())
            return;

        String message = event.getMessage().getContentRaw();
        String userId = event.getAuthor().getId();
        GuildMessageChannel channel = (GuildMessageChannel) event.getChannel();

        try {
            if (!event.getMessage().getAttachments().isEmpty()) {
                handleImport(event, userId, channel);
                return;
            }
            BotResponse response = logic.processUserInput(message, userId);
            if (response.hasFile()) {
                channel.sendFiles(FileUpload.fromData(response.getFile(), response.getFileName()))
                        .setContent(response.getMessage())
                        .queue();
            } else {
                channel.sendMessage(response.getMessage()).queue();
            }

        } catch (Exception e) {
            channel.sendMessage("Ошибка: " + e.getMessage()).queue();
            e.printStackTrace();
        }
    }

    /**
     * Обрабатывает вложение файла для импорта данных.
     * Загружает прикрепленный файл, передает его в логику обработки и отправляет
     * результат обратно.
     *
     * @param event полученное сообщения
     * @param userId идентификатор пользователя
     * @param channel канал, в который было отправлено сообщение
     * @see Message.Attachment
     * @see InputStream
     */
    private void handleImport(MessageReceivedEvent event, String userId, GuildMessageChannel channel) {
        List<Message.Attachment> attachments = event.getMessage().getAttachments();
        Message.Attachment fileAttachment = attachments.get(0);

        fileAttachment.getProxy().download().thenAccept(inputStream -> {
            try {
                BotResponse response = logic.processImport(inputStream, userId);
                channel.sendMessage(response.getMessage()).queue();
            } catch (Exception e) {
                channel.sendMessage("Ошибка при обработке файла: " + e.getMessage()).queue();
                e.printStackTrace();
            }
        });
    }
}