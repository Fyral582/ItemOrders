package de.Fyral.ordersPlugin.deliver;

import de.Fyral.ordersPlugin.OrdersPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DeliverManager {
    private final OrdersPlugin plugin;
    private File file;
    private FileConfiguration config;

    public DeliverManager(OrdersPlugin plugin) {
        this.plugin = plugin;
        setup();
    }

    public void setup() {
        // 1. Wir definieren den neuen Unterordner 'orders'
        File ordersFolder = new File(plugin.getDataFolder(), "orders");

        // 2. Erstelle den Unterordner, falls er nicht existiert
        if (!ordersFolder.exists()) {
            ordersFolder.mkdirs();
        }

        // 3. Speichere die Datei IM neuen Unterordner
        file = new File(ordersFolder, "rewards.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void addReward(int orderId, Material mat, int amount) {
        List<String> rewards = config.getStringList(String.valueOf(orderId));
        rewards.add(mat.name() + ":" + amount);
        config.set(String.valueOf(orderId), rewards);
        save();
    }

    public List<ItemStackData> getRewards(int orderId) {
        List<ItemStackData> list = new ArrayList<>();
        List<String> raw = config.getStringList(String.valueOf(orderId));
        for (String s : raw) {
            String[] parts = s.split(":");
            list.add(new ItemStackData(Material.valueOf(parts[0]), Integer.parseInt(parts[1])));
        }
        return list;
    }

    public void setRewards(int orderId, List<ItemStackData> remaining) {
        List<String> rewards = new ArrayList<>();
        for (ItemStackData data : remaining) {
            rewards.add(data.mat.name() + ":" + data.amount);
        }
        config.set(String.valueOf(orderId), rewards.isEmpty() ? null : rewards);
        save();
    }

    public boolean hasUnnotified(UUID uuid) {
        return config.contains("notifications." + uuid.toString()) && !config.getBoolean("notifications." + uuid + "_notified", true);
    }

    public void setNotified(UUID uuid) {
        config.set("notifications." + uuid + "_notified", true);
        save();
    }

    public void addNotification(UUID uuid) {
        config.set("notifications." + uuid.toString(), true);
        config.set("notifications." + uuid + "_notified", false);
        save();
    }

    public void save() {
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public static class ItemStackData {
        public Material mat;
        public int amount;
        public ItemStackData(Material m, int a) { this.mat = m; this.amount = a; }
    }
}