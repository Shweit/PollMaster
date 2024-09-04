package com.shweit.pollmaster.commands;

import com.shweit.pollmaster.PollMaster;
import com.shweit.pollmaster.utils.CheckForUpdate;
import com.shweit.pollmaster.utils.Logger;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.List;

public final class VersionCommand implements CommandExecutor, TabExecutor {

    @Override
    public boolean onCommand(final CommandSender commandSender, final Command command, final String s, final String[] args) {
        Logger.debug("VersionCommand.onCommand()");
        switch (args[0]) {
            case "version":
                commandSender.sendMessage(ChatColor.GREEN + "PollMaster version: " + ChatColor.GOLD + PollMaster.getInstance().getDescription().getVersion());
                commandSender.sendMessage("");
                commandSender.sendMessage(ChatColor.GREEN + "Check for updates...");

                // Check for updates
                CheckForUpdate checkForUpdate = new CheckForUpdate();
                boolean updateAvailable = checkForUpdate.checkForPluginUpdate();
                if (updateAvailable) {
                    commandSender.sendMessage(ChatColor.GREEN + "Update available! New Version: "
                            + ChatColor.GOLD + PollMaster.getInstance().getDescription().getVersion()
                            + ChatColor.GREEN + " -> " + ChatColor.GOLD + checkForUpdate.latestVersion
                    );
                } else {
                    commandSender.sendMessage(ChatColor.GREEN + "No updates available.");
                }

                return true;

            default:
                return false;
        }
    }

    @Override
    public List<String> onTabComplete(final CommandSender commandSender, final Command command, final String s, final String[] args) {
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            subCommands.add("version");
            return subCommands;
        }

        return new ArrayList<>();
    }
}
