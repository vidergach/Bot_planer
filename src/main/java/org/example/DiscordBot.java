package org.example;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class DiscordBot extends ListenerAdapter {
    private final String token;
    private final MessageHandler logic;
    private JDA jda;
    private boolean isRunning;

    public DiscordBot(String token, MessageHandler logic) {
        this.token = token;
        this.logic = logic;
    }

    public void start() {
        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(this)
                    .build();
            isRunning = true;
            System.out.println("Discord бот запущен: " + jda.getSelfUser().getName());
        }catch (Exception e) {
            System.err.println("Ошибка запуска Discord бота: " + e.getMessage());
            e.printStackTrace();
        }
        }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!isRunning || event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentRaw();
        GuildMessageChannel channel = (GuildMessageChannel) event.getChannel();
        if (!message.startsWith("/"))
            channel.sendMessage( """
                    Неизвестная команда.
                    Введите /help для просмотра доступных команд.""");
        processMessage(message, event.getAuthor().getId(), channel, event.getMessage());
    }

    private void processMessage(String message, String userId,
                                GuildMessageChannel channel, Message discordMessage) {
        try {
            if (message.startsWith("/export")) {
                handleExport(message, userId, channel);
            } else if (message.startsWith("/import")) {
                handleImport(discordMessage, userId, channel);
            } else {
                String response = logic.processUserInput(message, userId);
                channel.sendMessage(response).queue();
            }
        } catch (Exception e) {
            channel.sendMessage(" Ошибка: " + e.getMessage()).queue();
            e.printStackTrace();
        }
    }

    private void handleExport(String message, String userId, GuildMessageChannel channel) {
        String filename = message.substring("/export".length()).trim();
        if (filename.isEmpty()) {
            channel.sendMessage("Укажите имя файла после /export").queue();
            return;
        }

        try {
            File exportFile = logic.Export(userId, filename);
            channel.sendFiles(FileUpload.fromData(exportFile)).queue();
            logic.clean(exportFile);
        } catch (Exception e) {
            channel.sendMessage("Ошибка экспорта: " + e.getMessage()).queue();
        }
    }

    private void handleImport(Message message, String userId, GuildMessageChannel channel) {
        if (message.getAttachments().isEmpty()) {
            channel.sendMessage("Прикрепите файл для импорта").queue();
            return;
        }

        Message.Attachment attachment = message.getAttachments().get(0);

        attachment.getProxy().download().thenAcceptAsync(inputStream -> {
            File tempFile = null;
            try {
                tempFile = File.createTempFile("import", ".json");
                Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                String result = logic.Import(tempFile, userId);
                channel.sendMessage(result).queue();
            } catch (Exception e) {
                channel.sendMessage("Ошибка импорта: " + e.getMessage()).queue();
                e.printStackTrace();
            } finally {
                if (tempFile != null) {
                    logic.clean(tempFile);
                }
            }
        });
    }
}