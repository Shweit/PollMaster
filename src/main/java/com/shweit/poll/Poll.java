package com.shweit.poll;

import com.shweit.poll.commands.CreatePollCommand;
import com.shweit.poll.utils.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Poll extends JavaPlugin {

    public static Connection connection;
    public static FileConfiguration config;

    @Override
    public void onEnable() {
        createConfig();
        config = getConfig();

        setupDatabase();
        getCommand("createpoll").setExecutor(new CreatePollCommand());
    }

    @Override
    public void onDisable() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            Logger.error(e.toString());
        }
    }

    private void setupDatabase() {
        // Überprüfen, ob die Datei polls.sqlite existiert
        File dbFile = new File(getDataFolder(), "polls.sqlite");

        if (!dbFile.exists()) {
            // Wenn sie nicht existiert, erstelle sie
            try {
                if (!getDataFolder().exists()) {
                    getDataFolder().mkdirs();
                }

                connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getPath());
                Logger.debug("Connected to new polls.sqlite database.");

                // Lade und führe das SQL-Skript aus
                executeSqlScript(connection, "schema.sql");

            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            // Wenn die Datei existiert, verbinde mit der Datenbank
            try {
                connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getPath());
                Logger.debug("Connected to existing polls.sqlite database.");
            } catch (SQLException e) {
                Logger.error(e.toString());
            }
        }
    }

    private void executeSqlScript(Connection connection, String fileName) {
        try {
            // Lade die SQL-Datei aus dem Ressourcen-Ordner
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
