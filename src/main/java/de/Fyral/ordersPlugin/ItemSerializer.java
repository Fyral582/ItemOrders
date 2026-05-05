package de.Fyral.ordersPlugin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import java.util.Base64;

public class ItemSerializer {

    public static String toBase64(ItemStack item) {
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.set("i", item);
            String yaml = config.saveToString();
            return Base64.getEncoder().encodeToString(yaml.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ItemStack fromBase64(String data) {
        try {
            byte[] decoded = Base64.getDecoder().decode(data);
            String yaml = new String(decoded);
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);
            return config.getItemStack("i");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}