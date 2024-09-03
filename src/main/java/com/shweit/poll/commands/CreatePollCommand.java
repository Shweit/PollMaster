package com.shweit.poll.commands;

import com.shweit.poll.utils.ConnectionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import com.google.gson.Gson;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CreatePollCommand implements CommandExecutor, TabExecutor {
    private final Gson gson = new Gson();

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length < 3) {
            player.sendMessage("Usage: /createpoll \"<question>\" \"<answer1>\" \"<answer2>\" ... [--multi]");
            return false;
        }

        // Frage und Antworten extrahieren
        String question = null;
        List<String> answers = new ArrayList<>();
        boolean allowMultipleAnswers = false;

        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                if (args[i].equalsIgnoreCase("--multi")) {
                    allowMultipleAnswers = true;
                }
            } else if (args[i].startsWith("\"") && args[i].endsWith("\"")) {
                // Ganzes Argument ist in Anführungszeichen eingeschlossen
                if (question == null) {
                    question = args[i].substring(1, args[i].length() - 1);
                } else {
                    answers.add(args[i].substring(1, args[i].length() - 1));
                }
            } else if (args[i].startsWith("\"")) {
                // Beginn einer Frage oder Antwort
                StringBuilder sb = new StringBuilder(args[i].substring(1));
                while (++i < args.length && !args[i].endsWith("\"")) {
                    sb.append(" ").append(args[i]);
                }
                if (i < args.length) {
                    sb.append(" ").append(args[i].substring(0, args[i].length() - 1));
                }

                if (question == null) {
                    question = sb.toString();
                } else {
                    answers.add(sb.toString());
                }
            } else {
                answers.add(args[i]);
            }
        }

        if (question == null || answers.size() < 2) {
            player.sendMessage("You must provide a valid question and at least two answers.");
            return false;
        }

        // Antwortmöglichkeiten als JSON-String konvertieren
        String optionsAsJsonString = convertListToJson(answers);

        // Speichere das Poll in der Datenbank
        try {
            savePollToDatabase(player.getUniqueId(), question, optionsAsJsonString, allowMultipleAnswers);
            player.sendMessage("Poll created successfully!");
        } catch (SQLException e) {
            player.sendMessage("An error occurred while saving the poll.");
            e.printStackTrace();
        }

        return true;
    }

    private String convertListToJson(final List<String> list) {
        return gson.toJson(list);
    }

    private void savePollToDatabase(final UUID uniqueId, final String question, final String answersAsJsonString, final boolean multi) throws SQLException {
        Connection connection = new ConnectionManager().getConnection();
        String insertPollQuery = "INSERT INTO polls (uuid, question, answers, allowMultiple, isOpen) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertPollQuery)) {
            preparedStatement.setString(1, uniqueId.toString());
            preparedStatement.setString(2, question);
            preparedStatement.setString(3, answersAsJsonString);
            preparedStatement.setBoolean(4, multi);
            preparedStatement.setBoolean(5, true);
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public List<String> onTabComplete(final CommandSender commandSender, final Command command, final String s, final String[] strings) {
        return List.of();
    }
}
