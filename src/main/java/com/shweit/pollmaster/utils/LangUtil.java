package com.shweit.pollmaster.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public abstract class LangUtil {

    private static Map<String, String> translations = new HashMap<>();
    private static String currentLang = "en"; // Default language

    private static JavaPlugin plugin;

    // Initialize the LangUtil class with the plugin reference
    public static void initialize(final JavaPlugin pluginInstance) {
        plugin = pluginInstance;
        loadLanguageFile();
    }

    // Load the language file based on the config.yml 'lang' setting
    public static void loadLanguageFile() {
        if (plugin == null) {
            throw new IllegalStateException("LangUtil has not been initialized. Call LangUtil.initialize() first.");
        }

        FileConfiguration config = plugin.getConfig();
        currentLang = config.getString("lang", "en"); // Default to English if not specified

        File langFile = new File(plugin.getDataFolder() + File.separator + "lang", currentLang + ".yml");

        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file not found: " + currentLang + ".yml. Falling back to default.");
            return;
        }

        // Load the YML file into a configuration
        FileConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);
        for (String key : langConfig.getKeys(false)) {
            translations.put(key, langConfig.getString(key));
        }

        plugin.getLogger().info("Loaded language file: " + currentLang + ".yml");
    }

    // Retrieve the translation by key
    public static String getTranslation(final String key) {
        return translations.getOrDefault(key, key); // Fallback to key itself if translation is not found
    }

    // Retrieve translation with placeholders replaced by values from a Map
    public static String getTranslation(final String key, final Map<String, String> params) {
        String message = getTranslation(key);

        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                message = message.replace("${" + entry.getKey() + "}", entry.getValue());
            }
        }

        return message;
    }
}
