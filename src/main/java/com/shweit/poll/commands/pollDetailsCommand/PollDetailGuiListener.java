package com.shweit.poll.commands.pollDetailsCommand;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shweit.poll.utils.ConnectionManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PollDetailGuiListener implements Listener {
    private final Gson gson = new Gson();

    /**
     * Handles the click event in the Poll Details inventory.
     *
     * @param event The InventoryClickEvent triggered when a player clicks inside the inventory.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getClickedInventory();

        if (inventory == null || !event.getView().getTitle().equals(ChatColor.BLUE + "Poll Details")) {
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        String displayName = clickedItem.getItemMeta().getDisplayName();
        if (!displayName.startsWith(ChatColor.YELLOW.toString())) {
            return;
        }

        String answer = ChatColor.stripColor(displayName);
        Player player = (Player) event.getWhoClicked();
        UUID playerUUID = player.getUniqueId();
        int pollId = getPollIdFromInventory(inventory);

        if (pollId == -1) {
            player.sendMessage(ChatColor.RED + "Could not find the poll ID.");
            return;
        }

        try (Connection connection = new ConnectionManager().getConnection()) {
            if (hasVoted(playerUUID, pollId, connection)) {
                if (isSelectedAnswer(playerUUID, pollId, answer, connection)) {
                    // Remove the answer if it is already selected
                    removeVote(playerUUID, pollId, answer, connection);
                    player.sendMessage(ChatColor.RED + "You deselected: " + answer);
                } else if (allowsMultipleAnswers(pollId, connection)) {
                    // Add a new answer if multiple answers are allowed
                    addVote(playerUUID, pollId, answer, connection);
                    player.sendMessage(ChatColor.GREEN + "You selected: " + answer);
                } else {
                    player.sendMessage(ChatColor.RED + "You have already voted. Multiple answers are not allowed.");
                }
            } else {
                // Add the answer if no answer has been selected yet
                addVote(playerUUID, pollId, answer, connection);
                player.sendMessage(ChatColor.GREEN + "You selected: " + answer);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            player.sendMessage(ChatColor.RED + "An error occurred while processing your vote.");
        }

        // Reopen the poll details inventory to reflect the updated votes
        new PollDetailsCommand().openPollDetails(player, pollId);
    }

    /**
     * Retrieves the poll ID from the inventory based on the lore of the item in slot 13.
     *
     * @param inventory The inventory containing the poll details.
     * @return The poll ID or -1 if not found.
     */
    private int getPollIdFromInventory(Inventory inventory) {
        ItemStack item = inventory.getItem(13); // Get the item in slot 13

        if (item != null && item.hasItemMeta() && item.getItemMeta().hasLore()) {
            List<String> lore = item.getItemMeta().getLore();

            if (lore != null) {
                for (String line : lore) {
                    if (line.startsWith(ChatColor.GRAY + "ID: ")) {
                        try {
                            return Integer.parseInt(ChatColor.stripColor(line).replace("ID: ", ""));
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return -1; // Return -1 if no ID is found
    }

    /**
     * Checks if the player has already voted in the poll.
     *
     * @param playerUUID  The UUID of the player.
     * @param pollId      The ID of the poll.
     * @param connection  The database connection.
     * @return True if the player has voted, false otherwise.
     * @throws SQLException If an SQL error occurs.
     */
    private boolean hasVoted(UUID playerUUID, int pollId, Connection connection) throws SQLException {
        String query = "SELECT answers FROM votes WHERE uuid = ? AND poll_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playerUUID.toString());
            statement.setInt(2, pollId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String votes = resultSet.getString("answers");
                    return votes != null && !votes.isEmpty();
                }
            }
        }
        return false;
    }

    /**
     * Checks if a specific answer is selected by the player in the poll.
     *
     * @param playerUUID  The UUID of the player.
     * @param pollId      The ID of the poll.
     * @param answer      The answer to check.
     * @param connection  The database connection.
     * @return True if the answer is selected, false otherwise.
     * @throws SQLException If an SQL error occurs.
     */
    private boolean isSelectedAnswer(UUID playerUUID, int pollId, String answer, Connection connection) throws SQLException {
        String query = "SELECT answers FROM votes WHERE uuid = ? AND poll_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playerUUID.toString());
            statement.setInt(2, pollId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String votes = resultSet.getString("answers");
                    if (votes != null && !votes.isEmpty()) {
                        if (votes.startsWith("[")) {
                            // JSON array
                            List<String> voteList = gson.fromJson(votes, new TypeToken<List<String>>() {}.getType());
                            return voteList.contains(answer);
                        } else {
                            // Simple string answer
                            return votes.equals(answer);
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if the poll allows multiple answers.
     *
     * @param pollId      The ID of the poll.
     * @param connection  The database connection.
     * @return True if multiple answers are allowed, false otherwise.
     * @throws SQLException If an SQL error occurs.
     */
    private boolean allowsMultipleAnswers(int pollId, Connection connection) throws SQLException {
        String query = "SELECT allowMultiple FROM polls WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, pollId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBoolean("allowMultiple");
                }
            }
        }
        return false;
    }

    /**
     * Adds a vote for the player to the poll.
     *
     * @param playerUUID  The UUID of the player.
     * @param pollId      The ID of the poll.
     * @param answer      The answer to add.
     * @param connection  The database connection.
     * @throws SQLException If an SQL error occurs.
     */
    private void addVote(UUID playerUUID, int pollId, String answer, Connection connection) throws SQLException {
        String existingVotes = getPlayerVoteJson(playerUUID, pollId, connection);
        List<String> votes;

        if (existingVotes != null && !existingVotes.isEmpty()) {
            votes = gson.fromJson(existingVotes, new TypeToken<List<String>>() {}.getType());
        } else {
            votes = new ArrayList<>();
        }

        if (!votes.contains(answer)) {
            votes.add(answer);
        }

        String updateQuery = "UPDATE votes SET answers = ?, created_at = CURRENT_TIMESTAMP WHERE poll_id = ? AND uuid = ?";
        String insertQuery = "INSERT INTO votes (poll_id, uuid, answers, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";

        try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
            updateStatement.setString(1, gson.toJson(votes));
            updateStatement.setInt(2, pollId);
            updateStatement.setString(3, playerUUID.toString());

            int rowsAffected = updateStatement.executeUpdate();
            if (rowsAffected == 0) {
                // If no row was updated, insert a new record
                try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
                    insertStatement.setInt(1, pollId);
                    insertStatement.setString(2, playerUUID.toString());
                    insertStatement.setString(3, gson.toJson(votes));
                    insertStatement.executeUpdate();
                }
            }
        }
    }

    /**
     * Removes a vote for the player from the poll.
     *
     * @param playerUUID  The UUID of the player.
     * @param pollId      The ID of the poll.
     * @param answer      The answer to remove.
     * @param connection  The database connection.
     * @throws SQLException If an SQL error occurs.
     */
    private void removeVote(UUID playerUUID, int pollId, String answer, Connection connection) throws SQLException {
        String existingVotes = getPlayerVoteJson(playerUUID, pollId, connection);
        if (existingVotes == null || existingVotes.isEmpty()) {
            return;
        }

        List<String> votes;
        if (existingVotes.startsWith("[")) {
            // JSON array
            votes = gson.fromJson(existingVotes, new TypeToken<List<String>>() {}.getType());
        } else {
            // Simple string answer
            votes = new ArrayList<>();
            votes.add(existingVotes);
        }

        votes.remove(answer);

        String query;
        if (votes.isEmpty()) {
            query = "DELETE FROM votes WHERE poll_id = ? AND uuid = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, pollId);
                statement.setString(2, playerUUID.toString());
                statement.executeUpdate();
            }
        } else {
            query = "UPDATE votes SET answers = ?, created_at = CURRENT_TIMESTAMP WHERE poll_id = ? AND uuid = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, gson.toJson(votes));
                statement.setInt(2, pollId);
                statement.setString(3, playerUUID.toString());
                statement.executeUpdate();
            }
        }
    }

    /**
     * Retrieves the player's votes for a specific poll as a JSON string.
     *
     * @param playerUUID  The UUID of the player.
     * @param pollId      The ID of the poll.
     * @param connection  The database connection.
     * @return The votes as a JSON string, or null if none are found.
     * @throws SQLException If an SQL error occurs.
     */
    private String getPlayerVoteJson(UUID playerUUID, int pollId, Connection connection) throws SQLException {
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
}
