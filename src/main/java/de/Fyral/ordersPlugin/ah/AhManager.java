package de.Fyral.ordersPlugin.ah;

import de.Fyral.ordersPlugin.OrdersPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class AhManager {
    private final OrdersPlugin plugin;
    private File file;
    private FileConfiguration config;

    public AhManager(OrdersPlugin plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        File dir = new File(plugin.getDataFolder(), "ah");
        if (!dir.exists()) dir.mkdirs();
        file = new File(dir, "ah.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void addItem(UUID seller, ItemStack item, Material currency, int price) {
        int id = config.getInt("next_id", 1);
        String path = "items." + id;
        config.set(path + ".seller", seller.toString());
        config.set(path + ".item", itemToBase64(item));
        config.set(path + ".currency", currency.name());
        config.set(path + ".price", price);
        config.set("next_id", id + 1);
        save();
    }

    public void removeItem(int id) {
        config.set("items." + id, null);
        save();
    }

    public AhItem getItem(int id) {
        if (!config.contains("items." + id)) return null;
        String path = "items." + id;
        return new AhItem(
                id,
                UUID.fromString(config.getString(path + ".seller")),
                config.getString(path + ".item"),
                Material.valueOf(config.getString(path + ".currency")),
                config.getInt(path + ".price")
        );
    }

    public List<AhItem> getAllItems() {
        List<AhItem> list = new ArrayList<>();
        if (!config.contains("items")) return list;
        for (String key : config.getConfigurationSection("items").getKeys(false)) {
            AhItem item = getItem(Integer.parseInt(key));
            if (item != null) list.add(item);
        }
        return list;
    }

    // --- NEU: AH-ABHOLSTATION LOGIK ---
    public void addReward(UUID seller, Material mat, int amount) {
        List<String> rewards = config.getStringList("rewards." + seller.toString());
        rewards.add(mat.name() + ":" + amount);
        config.set("rewards." + seller.toString(), rewards);
        save();
    }

    public List<AhReward> getRewards(UUID seller) {
        List<AhReward> list = new ArrayList<>();
        List<String> raw = config.getStringList("rewards." + seller.toString());
        for (String s : raw) {
            String[] parts = s.split(":");
            list.add(new AhReward(Material.valueOf(parts[0]), Integer.parseInt(parts[1])));
        }
        return list;
    }

    public void setRewards(UUID seller, List<AhReward> remaining) {
        List<String> rewards = new ArrayList<>();
        for (AhReward data : remaining) {
            rewards.add(data.mat.name() + ":" + data.amount);
        }
        config.set("rewards." + seller.toString(), rewards.isEmpty() ? null : rewards);
        save();
    }

    public static class AhReward {
        public Material mat;
        public int amount;
        public AhReward(Material m, int a) { this.mat = m; this.amount = a; }
    }
    // ----------------------------------

    private void save() {
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public String itemToBase64(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) { throw new IllegalStateException("Fehler beim Speichern des Items", e); }
    }

    public ItemStack itemFromBase64(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (Exception e) { throw new IllegalStateException("Fehler beim Laden des Items", e); }
    }
}