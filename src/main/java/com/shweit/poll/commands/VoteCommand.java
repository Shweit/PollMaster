package com.shweit.poll.commands;

import com.shweit.poll.commands.pollDetailsCommand.PollDetailsCommand;
import com.shweit.poll.utils.ConnectionManager;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VoteCommand implements CommandExecutor, TabExecutor {
    private final Gson gson = new Gson();

    @Override
    public boolean onCommand(final CommandSender commandSender, final Command command, final String s, final String[] args) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage(ChatColor.RED + "You must be a player to execute this command.");
            return false;
        }

        if (!player.hasPermission("polls.vote")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return false;
        }

        new PollDetailsCommand().openPollDetails(player, Integer.parseInt(args[0]));
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender commandSender, final Command command, final String s, final String[] args) {
        if (args.length == 1) {
            List<Map<String, String>> openPolls = getOpenPolls();
            List<String> pollIds = new ArrayList<>();

            for (Map<String, String> poll : openPolls) {
                pollIds.add(poll.get("id"));
            }

            return pollIds;
        }

        return new ArrayList<>();
    }

    private List<Map<String, String>> getOpenPolls() {
        List<Map<String, String>> openPolls = new ArrayList<>();
        String query = "SELECT id, question, uuid, created_at FROM polls WHERE isOpen = 1";

        try (Connection connection = new ConnectionManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet results = statement.executeQuery()) {

            while (results.next()) {
                Map<String, String> pollData = new HashMap<>();
                pollData.put("id", String.valueOf(results.getInt("id")));
                pollData.put("question", results.getString("question"));
                pollData.put("creator", results.getString("uuid"));
                pollData.put("created_at", results.getString("created_at"));
                openPolls.add(pollData);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return openPolls;
    }
}
