package de.Fyral.ordersPlugin.deliver;

import de.Fyral.ordersPlugin.OrdersPlugin;
import de.Fyral.ordersPlugin.DataManager;
import de.Fyral.ordersPlugin.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Buy implements Listener {

    private final OrdersPlugin plugin;
    private final DataManager db;
    private final Map<UUID, DeliverySession> pendingSessions = new HashMap<>();

    public Buy(OrdersPlugin plugin, DataManager db) {
        this.plugin = plugin;
        this.db = db;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openDeliveryMenu(Player p, int orderId) {
        Inventory inv = Bukkit.createInventory(null, 54, Lang.get("menu.deliver_insert_prefix") + orderId);
        p.openInventory(inv);
    }

    @EventHandler
    public void onInputClose(InventoryCloseEvent e) {
        String title = e.getView().getTitle();
        if (title.startsWith(Lang.get("menu.deliver_insert_prefix"))) {
            Player p = (Player) e.getPlayer();
            int orderId = Integer.parseInt(title.split("#")[1].trim());

            List<ItemStack> insertedItems = new ArrayList<>();
            for (ItemStack item : e.getInventory().getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    insertedItems.add(item.clone());
                }
            }

            if (insertedItems.isEmpty()) {
                p.sendMessage(Lang.getPrefixed("msg.deliver_no_items"));
                return;
            }

            pendingSessions.put(p.getUniqueId(), new DeliverySession(orderId, insertedItems));
            Bukkit.getScheduler().runTaskLater(plugin, () -> openConfirmMenu(p, orderId), 2L);
        }
    }

    private void openConfirmMenu(Player p, int orderId) {
        Inventory inv = Bukkit.createInventory(null, 27, Lang.get("menu.deliver_confirm_prefix") + orderId);

        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta cm = confirm.getItemMeta();
        cm.setDisplayName(Lang.get("btn.deliver_confirm"));
        confirm.setItemMeta(cm);

        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cam = cancel.getItemMeta();
        cam.setDisplayName(Lang.get("btn.cancel"));
        cancel.setItemMeta(cam);

        inv.setItem(11, confirm);
        inv.setItem(15, cancel);
        p.openInventory(inv);
    }

    @EventHandler
    public void onConfirmClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (title.startsWith(Lang.get("menu.deliver_confirm_prefix"))) {
            e.setCancelled(true);
            Player p = (Player) e.getWhoClicked();
            ItemStack clicked = e.getCurrentItem();

            if (clicked == null || clicked.getType() == Material.AIR) return;

            DeliverySession session = pendingSessions.get(p.getUniqueId());
            if (session == null) {
                p.closeInventory();
                return;
            }

            if (clicked.getType() == Material.LIME_STAINED_GLASS_PANE) {
                int orderId = session.orderId;
                List<ItemStack> itemsToDeliver = session.items;
                pendingSessions.remove(p.getUniqueId());
                p.closeInventory();

                confirmDelivery(p, itemsToDeliver, orderId);

            } else if (clicked.getType() == Material.RED_STAINED_GLASS_PANE) {
                DeliverySession removed = pendingSessions.remove(p.getUniqueId());
                if (removed != null) returnItems(p, removed.items);
                p.closeInventory();
                p.sendMessage(Lang.getPrefixed("msg.deliver_cancelled"));
            }
        }
    }

    @EventHandler
    public void onConfirmClose(InventoryCloseEvent e) {
        String title = e.getView().getTitle();
        if (title.startsWith(Lang.get("menu.deliver_confirm_prefix"))) {
            Player p = (Player) e.getPlayer();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                DeliverySession session = pendingSessions.remove(p.getUniqueId());
                if (session != null) {
                    returnItems(p, session.items);
                    p.sendMessage(Lang.getPrefixed("msg.deliver_cancelled"));
                }
            }, 2L);
        }
    }

    private void returnItems(Player p, List<ItemStack> items) {
        for (ItemStack item : items) {
            HashMap<Integer, ItemStack> left = p.getInventory().addItem(item);
            for (ItemStack drop : left.values()) {
                p.getWorld().dropItem(p.getLocation(), drop);
            }
        }
    }

    public void confirmDelivery(Player p, List<ItemStack> deliveredItems, int orderId) {
        // HIER KOMMT DEINE BELOHNUNGS-LOGIK HINEIN (Unverändert lassen, was du hattest)
    }

    private static class DeliverySession {
        public int orderId;
        public List<ItemStack> items;
        public DeliverySession(int orderId, List<ItemStack> items) { this.orderId = orderId; this.items = items; }
    }
}