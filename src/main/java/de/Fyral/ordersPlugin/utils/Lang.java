package de.Fyral.ordersPlugin;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Lang {
    private static FileConfiguration langConfig;
    private static String prefix = "";

    public static void init(JavaPlugin plugin) {
        // 1. Config erstellen
        plugin.saveDefaultConfig();

        // 2. NEU: Wir exportieren IMMER beide Standard-Sprachen in den Ordner!
        if (!new File(plugin.getDataFolder(), "lang/en.yml").exists()) {
            if (plugin.getResource("lang/en.yml") != null) plugin.saveResource("lang/en.yml", false);
        }
        if (!new File(plugin.getDataFolder(), "lang/de.yml").exists()) {
            if (plugin.getResource("lang/de.yml") != null) plugin.saveResource("lang/de.yml", false);
        }

        // 3. Sprache aus der Config laden (Standard: "en")
        String lang = plugin.getConfig().getString("language", "en");
        File langFile = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");

        // Falls jemand einen Namen einträgt, den es nicht gibt (Fallback)
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file " + lang + ".yml not found! Defaulting to en.yml");
            langFile = new File(plugin.getDataFolder(), "lang/en.yml");
        }

        // 4. Datei in den Speicher laden
        langConfig = YamlConfiguration.loadConfiguration(langFile);
        if (langConfig.contains("prefix")) {
            prefix = ChatColor.translateAlternateColorCodes('&', langConfig.getString("prefix"));
        }
    }

    public static String get(String path) {
        if (langConfig == null || !langConfig.contains(path)) return "§c" + path;
        return ChatColor.translateAlternateColorCodes('&', langConfig.getString(path));
    }

    public static String getPrefixed(String path) {
        return prefix + get(path);
    }
}