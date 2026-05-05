package de.Fyral.ordersPlugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataManager {

    private final OrdersPlugin plugin;
    private File file;
    private FileConfiguration config;

    public DataManager(OrdersPlugin plugin) {
        this.plugin = plugin;
        setup();
    }

    public void setup() {
        file = new File(plugin.getDataFolder(), "orders.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void createOrder(UUID owner, String wMat, int wAmt, String pMat, int pAmt, boolean isPerItem) {
        int id = config.getInt("next_id", 1);
        String path = "orders." + id;
        config.set(path + ".owner", owner.toString());
        config.set(path + ".wanted_mat", wMat);
        config.set(path + ".wanted_amt", wAmt);
        config.set(path + ".price_mat", pMat);
        config.set(path + ".price_amt", pAmt);
        config.set(path + ".is_per_item", isPerItem);
        config.set(path + ".status", "ACTIVE");
        config.set("next_id", id + 1);
        save();
    }

    public OrderData getOrder(int id) {
        if (!config.contains("orders." + id)) return null;
        String path = "orders." + id;
        UUID owner = UUID.fromString(config.getString(path + ".owner"));
        String wMat = config.getString(path + ".wanted_mat");
        int wAmt = config.getInt(path + ".wanted_amt");
        String pMat = config.getString(path + ".price_mat");
        int pAmt = config.getInt(path + ".price_amt");
        boolean isPerItem = config.getBoolean(path + ".is_per_item");
        String status = config.getString(path + ".status", "ACTIVE");

        return new OrderData(id, owner, wMat + ":" + wAmt + ":" + isPerItem, pMat + ":" + pAmt, status);
    }

    public List<OrderData> getAllOrders() {
        List<OrderData> list = new ArrayList<>();
        if (!config.contains("orders")) return list;
        for (String key : config.getConfigurationSection("orders").getKeys(false)) {
            OrderData order = getOrder(Integer.parseInt(key));
            if (order != null) list.add(order);
        }
        return list;
    }

    public void updateOrder(int id, int newAmount, int newPrice) {
        String path = "orders." + id;
        config.set(path + ".wanted_amt", newAmount);
        config.set(path + ".price_amt", newPrice);
        save();
    }

    public void setStatus(int id, String status) {
        config.set("orders." + id + ".status", status);
        save();
    }

    public void deleteOrder(int id) {
        config.set("orders." + id, null);
        save();
    }

    public void save() {
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }
}