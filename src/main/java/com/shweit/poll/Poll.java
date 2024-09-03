package com.shweit.poll;

import com.shweit.poll.commands.CreatePollCommand;
import com.shweit.poll.commands.pollDetailsCommand.PollDetailGuiListener;
import com.shweit.poll.commands.pollsCommand.PollsCommand;
import com.shweit.poll.commands.pollsCommand.PollsGuiListener;
import com.shweit.poll.utils.ConnectionManager;
import com.shweit.poll.utils.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class Poll extends JavaPlugin {

    public static Connection connection;
    public static FileConfiguration config;
    private static Poll instance;

    @Override
    public void onEnable() {
        createConfig();
        config = getConfig();
        instance = this;

        setupDatabase();
        getCommand("createpoll").setExecutor(new CreatePollCommand());
        getCommand("polls").setExecutor(new PollsCommand());
        getServer().getPluginManager().registerEvents(new PollsGuiListener(), this);
        getServer().getPluginManager().registerEvents(new PollDetailGuiListener(), this);
    }

    @Override
    public void onDisable() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            Logger.error(e.toString());
            e.printStackTrace();
        }
    }

    public static Poll getInstance() {
        return instance;
    }

    private void setupDatabase() {
        File dbFile = new File(getDataFolder(), "polls.sqlite");

        if (!dbFile.exists()) {
            try {
                if (!getDataFolder().exists()) {
                    getDataFolder().mkdirs();
                }

                Connection connection = new ConnectionManager().getConnection();

                executeSqlScript(connection, "sql/polls_table.sql");
                executeSqlScript(connection, "sql/votes_table.sql");

            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            try {
                Connection connection = new ConnectionManager().getConnection();
                Logger.debug("Connected to existing polls.sqlite database.");
            } catch (SQLException e) {
                Logger.error(e.toString());
            }
        }
    }

    private void executeSqlScript(Connection connection, String fileName) {
        try {
            InputStream is = getResource(fileName);
            if (is == null) {
                Logger.error("SQL file not found: " + fileName);
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sql.append(line);
            }
            reader.close();

            // Führe das SQL-Skript aus
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql.toString());
                Logger.debug("SQL script executed successfully.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists())  {
            saveResource("config.yml", false);
        }
    }
}
