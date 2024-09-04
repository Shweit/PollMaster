package com.shweit.poll.commands;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shweit.poll.utils.ConnectionManager;
import com.shweit.poll.utils.Logger;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public final class DeletePollCommand implements CommandExecutor, TabExecutor {


    public boolean deletePoll(final Player whoClicked, final int pollId) {
        Gson gson = new Gson(); // To handle JSON parsing

        // Connect to the database
        try (Connection connection = new ConnectionManager().getConnection()) {
            // Check if the player is the creator of the poll
            String checkCreatorQuery = "SELECT uuid FROM polls WHERE id = ?";
            try (PreparedStatement checkCreatorStmt = connection.prepareStatement(checkCreatorQuery)) {
                checkCreatorStmt.setInt(1, pollId);
                try (ResultSet resultSet = checkCreatorStmt.executeQuery()) {
                    if (resultSet.next()) {
                        String creatorUUID = resultSet.getString("uuid");
                        if (!creatorUUID.equals(whoClicked.getUniqueId().toString())) {
                            whoClicked.sendMessage(ChatColor.RED + "You can only delete polls that you have created.");
                            return false;
                        }
                    } else {
                        whoClicked.sendMessage(ChatColor.RED + "Poll ID " + pollId + " not found.");
                        return false;
                    }
                }
            }

            // Get the poll details and answers to display stats of the poll and the winning answer
            String pollDetailsQuery = "SELECT question, answers FROM polls WHERE id = ?";
            Map<String, Integer> voteCounts = new HashMap<>();
            String pollQuestion = "";
            List<String> possibleAnswers = new ArrayList<>();

            try (PreparedStatement pollDetailsStmt = connection.prepareStatement(pollDetailsQuery)) {
                pollDetailsStmt.setInt(1, pollId);
                try (ResultSet resultSet = pollDetailsStmt.executeQuery()) {
                    if (resultSet.next()) {
                        pollQuestion = resultSet.getString("question");
                        // Parse answers from JSON array
                        possibleAnswers = gson.fromJson(resultSet.getString("answers"), new TypeToken<List<String>>() { } .getType());
                    }
                }
            }

            // Count the votes for each answer
            String voteCountQuery = "SELECT answers FROM votes WHERE poll_id = ?";
            try (PreparedStatement voteCountStmt = connection.prepareStatement(voteCountQuery)) {
                voteCountStmt.setInt(1, pollId);
                try (ResultSet resultSet = voteCountStmt.executeQuery()) {
                    while (resultSet.next()) {
                        // Get the voted answers stored as JSON array
                        String answersJson = resultSet.getString("answers");
                        List<String> votedAnswers = gson.fromJson(answersJson, new TypeToken<List<String>>() { } .getType());
                        for (String votedAnswer : votedAnswers) {
                            votedAnswer = votedAnswer.trim();
                            voteCounts.put(votedAnswer, voteCounts.getOrDefault(votedAnswer, 0) + 1);
                        }
                    }
                }
            }

            // Display poll results in chat
            whoClicked.sendMessage(ChatColor.GRAY + "-------------------- [" + ChatColor.LIGHT_PURPLE + "Poll Results" + ChatColor.GRAY + "] --------------------");
            whoClicked.sendMessage("");
            whoClicked.sendMessage("");
            whoClicked.sendMessage(ChatColor.YELLOW + "Question: " + ChatColor.WHITE + pollQuestion);
            whoClicked.sendMessage("");

            // Display each answer with the number of votes
            int maxVotes = 0;
            List<String> winningAnswers = new ArrayList<>();

            for (String answer : possibleAnswers) {
                int votes = voteCounts.getOrDefault(answer.trim(), 0);
                whoClicked.sendMessage(ChatColor.AQUA + "Answer: " + ChatColor.WHITE + answer.trim() + ChatColor.GRAY + " | Votes: " + ChatColor.GREEN + votes);

                // Track the answer(s) with the highest votes
                if (votes > maxVotes) {
                    maxVotes = votes;
                    winningAnswers.clear();
                    winningAnswers.add(answer.trim());
                } else if (votes == maxVotes) {
                    winningAnswers.add(answer.trim());
                }
            }

            // Display the winning answer(s)
            whoClicked.sendMessage("");
            whoClicked.sendMessage("");
            whoClicked.sendMessage(ChatColor.LIGHT_PURPLE + "Winning Answer(s): " + ChatColor.WHITE + String.join(", ", winningAnswers));
            whoClicked.sendMessage("");
            whoClicked.sendMessage(ChatColor.GRAY + "-------------------- [" + ChatColor.LIGHT_PURPLE + "Poll Results" + ChatColor.GRAY + "] --------------------");

            // Prepare the SQL statements to delete the poll and associated votes
            String deletePollQuery = "DELETE FROM polls WHERE id = ?";
            String deleteVotesQuery = "DELETE FROM votes WHERE poll_id = ?";

            // Delete the poll itself
            try (PreparedStatement deletePollStmt = connection.prepareStatement(deletePollQuery)) {
                deletePollStmt.setInt(1, pollId);
                int rowsAffectedPolls = deletePollStmt.executeUpdate();

                // Check if the poll was successfully deleted
                if (rowsAffectedPolls > 0) {
                    // Delete associated votes
                    try (PreparedStatement deleteVotesStmt = connection.prepareStatement(deleteVotesQuery)) {
                        deleteVotesStmt.setInt(1, pollId);
                        deleteVotesStmt.executeUpdate(); // Delete all votes associated with the poll
                    }

                    whoClicked.sendMessage(ChatColor.GREEN + "Poll ID " + pollId + " has been successfully deleted.");
                    return true;
                } else {
                    whoClicked.sendMessage(ChatColor.RED + "Poll ID " + pollId + " not found. Deletion failed.");
                    return false;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            whoClicked.sendMessage(ChatColor.RED + "An error occurred while trying to delete the poll.");
        }

        return false;
    }


    @Override
    public boolean onCommand(final CommandSender commandSender, final Command command, final String s, final String[] args) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage("This command can only be used by players.");
            return true;
        }

        deletePoll(player, Integer.parseInt(args[0]));
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender commandSender, final Command command, final String s, final String[] args) {
        if (args.length == 1) {
            Logger.debug("Tab completion for DeletePollCommand");
            return getPlayerPolls((Player) commandSender);
        }

        return new ArrayList<>();
    }

    private ArrayList<String> getPlayerPolls(final Player player) {
        ArrayList<String> pollData = new ArrayList<>();
        String query = "SELECT id FROM polls WHERE isOpen = 1 AND uuid = ?";

        // Create connection and statement using try-with-resources
        try (Connection connection = new ConnectionManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            // Set the player's UUID as a parameter in the prepared statement
            statement.setString(1, player.getUniqueId().toString());

            // Execute the query and process the results
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    pollData.add(String.valueOf(results.getInt("id")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return pollData;
    }

}
