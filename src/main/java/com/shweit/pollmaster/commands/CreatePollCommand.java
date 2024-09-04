package com.shweit.pollmaster.commands;

import com.shweit.pollmaster.utils.ConnectionManager;
import com.shweit.pollmaster.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import com.google.gson.Gson;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CreatePollCommand implements CommandExecutor, TabExecutor {
    private final Gson gson = new Gson();

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + LangUtil.getTranslation("command_no_player"));
            return true;
        }

        if (!player.hasPermission("pollmaster.create")) {
            player.sendMessage(ChatColor.RED + LangUtil.getTranslation("command_no_permission"));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + LangUtil.getTranslation("usage") + "/createpoll \"<question>\" \"<answer1>\" \"<answer2>\" ... [--multi]");
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
            player.sendMessage(LangUtil.getTranslation("invalid_arguments_count"));
            return false;
        }

        // Antwortmöglichkeiten als JSON-String konvertieren
        String optionsAsJsonString = convertListToJson(answers);

        // Speichere das Poll in der Datenbank
        int id = 0;
        try {
            id = savePollToDatabase(player.getUniqueId(), question, optionsAsJsonString, allowMultipleAnswers);
            player.sendMessage(LangUtil.getTranslation("poll_created"));
        } catch (SQLException e) {
            player.sendMessage(LangUtil.getTranslation("error_while_creating_poll"));
            e.printStackTrace();
        }

        int finalId = id;
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.sendMessage(ChatColor.GREEN + "A new poll has been created by " + player.getName() + ".");
            p.sendMessage(ChatColor.GREEN + "To view the poll, type /vote " + finalId);
        });

        return true;
    }

    private String convertListToJson(final List<String> list) {
        return gson.toJson(list);
    }

    private int savePollToDatabase(final UUID uniqueId, final String question, final String answersAsJsonString, final boolean multi) throws SQLException {
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

        // Return the ID of the poll
        String query = "SELECT id FROM polls WHERE uuid = ? AND question = ? AND answers = ? AND allowMultiple = ? AND isOpen = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, uniqueId.toString());
            statement.setString(2, question);
            statement.setString(3, answersAsJsonString);
            statement.setBoolean(4, multi);
            statement.setBoolean(5, true);
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return results.getInt("id");
                }
            }
        }

        return 0;
    }

    @Override
    public List<String> onTabComplete(final CommandSender commandSender, final Command command, final String s, final String[] strings) {
        return List.of();
    }
}
