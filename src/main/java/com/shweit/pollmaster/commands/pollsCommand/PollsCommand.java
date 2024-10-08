package com.shweit.pollmaster.commands.pollsCommand;

import com.shweit.pollmaster.utils.ConnectionManager;
import com.shweit.pollmaster.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public final class PollsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + LangUtil.getTranslation("command_no_player"));
            return true;
        }

        if (!sender.hasPermission("pollmaster.view")) {
            sender.sendMessage(ChatColor.RED + LangUtil.getTranslation("command_no_permission"));
            return true;
        }

        Player player = (Player) sender;
        int page = 0;

        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]) - 1;
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + LangUtil.getTranslation("invalid_page_number"));
                return false;
            }
        }

        openPollsGUI(player, page);

        return true;
    }

    /**
     * Opens the Polls GUI for the player, displaying a paginated list of open polls.
     *
     * @param player The player to whom the GUI is shown.
     * @param page   The page number to display.
     */
    public void openPollsGUI(final Player player, final int page) {
        List<Map<String, String>> openPolls = getOpenPolls();

        int pollsPerPage = 28; // 28 slots for polls, 26 slots for borders and navigation
        int totalPages = (int) Math.ceil((double) openPolls.size() / pollsPerPage);

        int currentPage = page; // Lokale Variable für die Modifikation

        if (currentPage < 0 || currentPage >= totalPages) {
            currentPage = 0;
        }

        Inventory pollsInventory = Bukkit.createInventory(null, 54,
                ChatColor.GREEN + LangUtil.getTranslation("open_polls")
                + " (" + LangUtil.getTranslation("page") + " " + (currentPage + 1)
                + "/" + totalPages + ")"
        );

        // Add border around the inventory
        ItemStack borderItem = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderItem.getItemMeta();
        borderMeta.setDisplayName(" ");
        borderItem.setItemMeta(borderMeta);

        for (int i = 0; i < 54; i++) {
            if (i < 10 || i > 43 || i % 9 == 0 || (i + 1) % 9 == 0) {
                pollsInventory.setItem(i, borderItem);
            }
        }

        int start = currentPage * pollsPerPage;
        int end = Math.min(start + pollsPerPage, openPolls.size());

        for (int i = start; i < end; i++) {
            Map<String, String> poll = openPolls.get(i);
            ItemStack pollItem = new ItemStack(Material.PAPER);
            ItemMeta meta = pollItem.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + poll.get("question"));

            List<String> lore = new ArrayList<>();
            OfflinePlayer creator = Bukkit.getOfflinePlayer(UUID.fromString(poll.get("creator")));
            lore.add(ChatColor.GRAY + LangUtil.getTranslation("created_by") + ": " + ChatColor.GREEN + (creator.getName() != null ? creator.getName() : "Unknown"));
            lore.add(ChatColor.GRAY + LangUtil.getTranslation("created_on") + ": " + ChatColor.GREEN + poll.get("created_at"));
            lore.add(ChatColor.GRAY + "ID: " + ChatColor.GREEN + poll.get("id"));
            lore.add("");
            lore.add(ChatColor.GREEN + LangUtil.getTranslation("click_to_vote"));

            if (creator.getUniqueId() == player.getUniqueId()) {
                lore.add("");
                lore.add(ChatColor.RED + LangUtil.getTranslation("delete_poll_lore"));
            }
            meta.setLore(lore);
            if (creator.getUniqueId() == player.getUniqueId()) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            pollItem.setItemMeta(meta);
            pollsInventory.addItem(pollItem);
        }

        // Add pagination items
        if (currentPage > 0) {
            ItemStack previousPage = new ItemStack(Material.ARROW);
            ItemMeta previousMeta = previousPage.getItemMeta();
            previousMeta.setDisplayName(ChatColor.AQUA + LangUtil.getTranslation("previous_page"));
            previousPage.setItemMeta(previousMeta);
            pollsInventory.setItem(45, previousPage);
        }

        if (currentPage < totalPages - 1) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextPage.getItemMeta();
            nextMeta.setDisplayName(ChatColor.AQUA + LangUtil.getTranslation("next_page"));
            nextPage.setItemMeta(nextMeta);
            pollsInventory.setItem(53, nextPage);
        }

        player.openInventory(pollsInventory);
    }


    /**
     * Retrieves a list of open polls from the database.
     *
     * @return A list of maps, where each map contains poll information.
     */
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
