package com.shweit.pollmaster.events;

import com.shweit.pollmaster.utils.ConnectionManager;
import com.shweit.pollmaster.utils.LangUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public final class PlayerJoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (!player.hasPermission("pollmaster.view")) {
            return;
        }

        // Schedule the message to be sent after a small delay to ensure the player is fully loaded
        org.bukkit.Bukkit.getScheduler().runTaskLater(
            org.bukkit.Bukkit.getPluginManager().getPlugin("PollMaster"), 
            () -> sendPollStatsToPlayer(player), 
            20L // 1 second delay
        );
    }

    private void sendPollStatsToPlayer(final Player player) {
        try {
            int pollsCanVoteIn = getPollsPlayerCanVoteIn(player);
            int ownOpenPolls = getOwnOpenPolls(player);

            // Send welcome header
            player.sendMessage(LangUtil.getTranslation("player_join_welcome"));
            player.sendMessage(LangUtil.getTranslation("player_join_polls_stats"));
            player.sendMessage("");

            // Send voting information
            if (pollsCanVoteIn > 0) {
                Map<String, String> params = new HashMap<>();
                params.put("count", String.valueOf(pollsCanVoteIn));
                player.sendMessage(LangUtil.getTranslation("player_join_can_vote", params));
            } else {
                player.sendMessage(LangUtil.getTranslation("player_join_no_polls_to_vote"));
            }

            // Send own polls information (only if player has polls)
            if (ownOpenPolls > 0) {
                Map<String, String> params = new HashMap<>();
                params.put("count", String.valueOf(ownOpenPolls));
                player.sendMessage(LangUtil.getTranslation("player_join_your_polls", params));
            }

            player.sendMessage(LangUtil.getTranslation("player_join_footer"));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the number of polls the player can vote in (polls they haven't voted in yet)
     */
    private int getPollsPlayerCanVoteIn(final Player player) throws SQLException {
        String query = """
            SELECT COUNT(*) as count FROM polls p 
            WHERE p.isOpen = 1 
            AND p.id NOT IN (
                SELECT v.poll_id FROM votes v WHERE v.uuid = ?
            )
        """;

        try (Connection connection = new ConnectionManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            
            statement.setString(1, player.getUniqueId().toString());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("count");
                }
            }
        }
        return 0;
    }

    /**
     * Gets the number of open polls created by the player
     */
    private int getOwnOpenPolls(final Player player) throws SQLException {
        String query = "SELECT COUNT(*) as count FROM polls WHERE isOpen = 1 AND uuid = ?";

        try (Connection connection = new ConnectionManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            
            statement.setString(1, player.getUniqueId().toString());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("count");
                }
            }
        }
        return 0;
    }
}
