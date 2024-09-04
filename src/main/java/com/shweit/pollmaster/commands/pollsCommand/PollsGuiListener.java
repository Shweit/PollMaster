package com.shweit.pollmaster.commands.pollsCommand;

import com.shweit.pollmaster.PollMaster;
import com.shweit.pollmaster.commands.DeletePollCommand;
import com.shweit.pollmaster.commands.pollDetailsCommand.PollDetailsCommand;
import com.shweit.pollmaster.utils.LangUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class PollsGuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        Inventory inventory = event.getClickedInventory();

        if (inventory == null || !event.getView().getTitle().startsWith(ChatColor.GREEN + LangUtil.getTranslation("open_polls"))) {
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        String displayName = clickedItem.getItemMeta().getDisplayName();

        if (displayName.equals(ChatColor.AQUA + LangUtil.getTranslation("next_page"))) {
            handlePageChange(event, 1);
        } else if (displayName.equals(ChatColor.AQUA + LangUtil.getTranslation("previous_page"))) {
            handlePageChange(event, -1);
        } else {
            String lore = clickedItem.getItemMeta().getLore().get(2);
            int pollId = Integer.parseInt(ChatColor.stripColor(lore).replace("ID: ", ""));

            if (event.getClick().isLeftClick()) {
                new PollDetailsCommand().openPollDetails((Player) event.getWhoClicked(), pollId);
            } else if (event.getClick().isRightClick()) {
                boolean success = new DeletePollCommand().deletePoll((Player) event.getWhoClicked(), pollId);
                if (success) {
                    new PollsCommand().openPollsGUI((Player) event.getWhoClicked(), 0);
                }
            }
        }
    }

    private void handlePageChange(final InventoryClickEvent event, final int change) {
        String title = event.getView().getTitle();
        int currentPage = extractPageNumber(title);
        if (currentPage != -1) {
            PollMaster.getInstance().getServer().getScheduler().runTask(PollMaster.getInstance(), () -> {
                event.getWhoClicked().closeInventory();
                ((Player) event.getWhoClicked()).performCommand("polls " + (currentPage + change));
            });
        }
    }

    private int extractPageNumber(final String title) {
        try {
            int startIndex = title.indexOf(LangUtil.getTranslation("page") + " ") + 5;
            int endIndex = title.indexOf("/", startIndex);
            if (startIndex > 0 && endIndex > startIndex) {
                return Integer.parseInt(title.substring(startIndex, endIndex).trim());
            }
        } catch (NumberFormatException e) {
            e.printStackTrace(); // Optional: Zum Debuggen, wenn etwas schiefgeht.
        }
        return -1; // Rückgabe -1, wenn die Seitenzahl nicht extrahiert werden kann.
    }
}
