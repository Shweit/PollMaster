package com.shweit.poll.commands.pollDetailsCommand;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shweit.poll.utils.ConnectionManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public final class PollDetailsCommand {

    private final Gson gson = new Gson();

    /**
     * Opens a detailed view of a poll in an inventory GUI for the player.
     * @param player The player for whom the GUI is opened.
     * @param pollId The ID of the poll to display.
     */
    public void openPollDetails(final Player player, final int pollId) {
        if (!player.hasPermission("polls.vote")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to view poll details.");
        }

        Inventory pollDetailsInventory = Bukkit.createInventory(null, 54, ChatColor.BLUE + "Poll Details");

        PollDetails pollDetails = getPollDetails(pollId);
        List<String> answers = pollDetails.getAnswers();
        List<String> playerVotes = getPlayerVotes(player.getUniqueId(), pollId);
        Map<String, Integer> voteCounts = getVoteCounts(pollId);

        // Add a border around the inventory
        ItemStack borderItem = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderItem.getItemMeta();
        borderMeta.setDisplayName(" ");
        borderItem.setItemMeta(borderMeta);

        for (int i = 0; i < 54; i++) {
            if (i < 10 || i > 43 || i % 9 == 0 || (i + 1) % 9 == 0) {
                pollDetailsInventory.setItem(i, borderItem);
            }
        }

        // Add the question at the top center
        ItemStack questionItem = new ItemStack(Material.PAPER);
        ItemMeta questionMeta = questionItem.getItemMeta();
        questionMeta.setDisplayName(ChatColor.GOLD + pollDetails.getQuestion());

        List<String> questionLore = new ArrayList<>();
        OfflinePlayer creator = Bukkit.getOfflinePlayer(UUID.fromString(pollDetails.getCreator()));
        questionLore.add(ChatColor.GRAY + "Created by: " + ChatColor.GREEN + creator.getName());
        questionLore.add(ChatColor.GRAY + "Created on: " + ChatColor.GREEN + pollDetails.getCreatedAt());
        questionLore.add(ChatColor.GRAY + "ID: " + ChatColor.GREEN + pollDetails.getPollId());
        questionLore.add("");
        questionLore.add(ChatColor.GRAY + "Your vote: " + ChatColor.AQUA + (!playerVotes.isEmpty() ? String.join(", ", playerVotes) : "None"));
        if (pollDetails.isMulti()) {
            questionLore.add("");
            questionLore.add(ChatColor.GREEN + "You can vote for multiple answers.");
        }
        questionMeta.setLore(questionLore);

        questionItem.setItemMeta(questionMeta);
        pollDetailsInventory.setItem(13, questionItem);

        // Add the answers
        int startSlot = 19; // First slot for answers
        for (String answer : answers) {
            Material material = playerVotes.contains(answer) ? Material.GREEN_TERRACOTTA : Material.RED_TERRACOTTA;
            ItemStack answerItem = new ItemStack(material);
            ItemMeta meta = answerItem.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + answer);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Votes: " + ChatColor.AQUA + voteCounts.getOrDefault(answer, 0));
            meta.setLore(lore);

            answerItem.setItemMeta(meta);
            pollDetailsInventory.setItem(startSlot++, answerItem);

            if (startSlot == 26) {
                startSlot = 28; // Skip the middle slot to the next row
            }
        }

        player.openInventory(pollDetailsInventory);
    }

    /**
     * Retrieves detailed information about a poll from the database.
     * @param pollId The ID of the poll to retrieve.
     * @return A PollDetails object containing the poll's details.
     */
    private PollDetails getPollDetails(final int pollId) {
        PollDetails pollDetails = null;
        String query = "SELECT question, answers, uuid, created_at, allowMultiple FROM polls WHERE id = ?";

        try (Connection connection = new ConnectionManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, pollId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String question = resultSet.getString("question");
                    String answersJson = resultSet.getString("answers");
                    String creator = resultSet.getString("uuid");
                    String createdAt = resultSet.getString("created_at");
                    boolean multi = resultSet.getBoolean("allowMultiple");

                    List<String> answers = decodeAnswers(answersJson);
                    pollDetails = new PollDetails(pollId, question, answers, creator, createdAt, multi);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return pollDetails;
    }

    /**
     * Retrieves the votes of a player for a specific poll.
     * @param playerUUID The UUID of the player.
     * @param pollId The ID of the poll.
     * @return A list of answers the player voted for.
     */
    private List<String> getPlayerVotes(final UUID playerUUID, final int pollId) {
        try (Connection connection = new ConnectionManager().getConnection()) {
            String votesJson = getPlayerVoteJson(playerUUID, pollId, connection);
            if (votesJson != null && !votesJson.isEmpty()) {
                if (votesJson.startsWith("[")) {
                    // The votes are stored as a JSON array
                    return gson.fromJson(votesJson, new TypeToken<List<String>>() { } .getType());
                } else {
                    // The votes are stored as a simple string
                    List<String> singleVoteList = new ArrayList<>();
                    singleVoteList.add(votesJson);
                    return singleVoteList;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    /**
     * Retrieves the votes of a player for a specific poll as a JSON string.
     * @param playerUUID The UUID of the player.
     * @param pollId The ID of the poll.
     * @param connection The database connection.
     * @return The votes as a JSON string.
     * @throws SQLException If an SQL error occurs.
     */
    private String getPlayerVoteJson(final UUID playerUUID, final int pollId, final Connection connection) throws SQLException {
        String query = "SELECT answers FROM votes WHERE uuid = ? AND poll_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playerUUID.toString());
            statement.setInt(2, pollId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("answers");
                }
            }
        }
        return null;
    }

    /**
     * Retrieves the vote counts for each answer in a poll.
     * @param pollId The ID of the poll.
     * @return A map of answers to their corresponding vote counts.
     */
    private Map<String, Integer> getVoteCounts(final int pollId) {
        Map<String, Integer> voteCounts = new HashMap<>();
        String query = "SELECT answers FROM votes WHERE poll_id = ?";

        try (Connection connection = new ConnectionManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, pollId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String answers = resultSet.getString("answers");
                    if (answers != null && !answers.isEmpty()) {
                        if (answers.startsWith("[")) {
                            List<String> answerList = gson.fromJson(answers, new TypeToken<List<String>>() { } .getType());
                            for (String answer : answerList) {
                                voteCounts.put(answer, voteCounts.getOrDefault(answer, 0) + 1);
                            }
                        } else {
                            voteCounts.put(answers, voteCounts.getOrDefault(answers, 0) + 1);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return voteCounts;
    }

    /**
     * Decodes a JSON string of answers into a list.
     * @param answersJson The JSON string of answers.
     * @return A list of answers.
     */
    private List<String> decodeAnswers(final String answersJson) {
        Type listType = new TypeToken<List<String>>() { } .getType();
        return gson.fromJson(answersJson, listType);
    }
}
