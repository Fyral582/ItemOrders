package de.Fyral.ordersPlugin.ah;

import de.Fyral.ordersPlugin.OrdersPlugin;
import de.Fyral.ordersPlugin.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class AhGUI implements Listener {

    private final OrdersPlugin plugin;
    private final AhManager ahManager;
    private final NamespacedKey keyAhId, keyAhPrice, keyAhCurrency, keyAhPage, keyAhSearch;

    public AhGUI(OrdersPlugin plugin, AhManager ahManager) {
        this.plugin = plugin;
        this.ahManager = ahManager;
        this.keyAhId = new NamespacedKey(plugin, "ah_id");
        this.keyAhPrice = new NamespacedKey(plugin, "ah_price");
        this.keyAhCurrency = new NamespacedKey(plugin, "ah_currency");
        this.keyAhPage = new NamespacedKey(plugin, "ah_page");
        this.keyAhSearch = new NamespacedKey(plugin, "ah_search");
    }

    private String formatTimeLeft(long timestamp) {
        int configDays = plugin.getConfig().getInt("ah_expiration_days", 7);
        long expiresAt = timestamp + (configDays * 24L * 60 * 60 * 1000);
        long timeLeft = expiresAt - System.currentTimeMillis();

        if (timeLeft <= 0) return "0m";

        long days = timeLeft / (1000 * 60 * 60 * 24);
        long hours = (timeLeft / (1000 * 60 * 60)) % 24;
        long minutes = (timeLeft / (1000 * 60)) % 60;

        if (days > 0) return Lang.get("ah.time_days").replace("%d%", String.valueOf(days)).replace("%h%", String.valueOf(hours));
        if (hours > 0) return Lang.get("ah.time_hours").replace("%h%", String.valueOf(hours)).replace("%m%", String.valueOf(minutes));
        return Lang.get("ah.time_minutes").replace("%m%", String.valueOf(minutes));
    }

    public void openMainMenu(Player p, int page, String searchQuery) {
        Inventory inv = Bukkit.createInventory(null, 54, Lang.get("menu.ah_main_prefix") + (page + 1));

        List<AhItem> allItems = ahManager.getAllItems();
        List<AhItem> filteredItems = new ArrayList<>();

        for (AhItem ahItem : allItems) {
            if (searchQuery == null || searchQuery.isEmpty()) {
                filteredItems.add(ahItem);
            } else {
                ItemStack original = ahManager.itemFromBase64(ahItem.itemBase64);
                if (original.getType().name().contains(searchQuery.toUpperCase())) {
                    filteredItems.add(ahItem);
                }
            }
        }

        int start = page * 45;
        int end = Math.min(start + 45, filteredItems.size());
        int slot = 0;

        for (int i = start; i < end; i++) {
            AhItem ahItem = filteredItems.get(i);
            ItemStack original = ahManager.itemFromBase64(ahItem.itemBase64);
            ItemStack display = original.clone();
            ItemMeta meta = display.getItemMeta();

            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            String sellerName = Bukkit.getOfflinePlayer(ahItem.seller).getName();
            if (sellerName == null) sellerName = "Unbekannt";

            lore.add("§8§m----------------------");
            lore.add(Lang.get("ah.seller").replace("%name%", sellerName));
            lore.add(Lang.get("ah.price").replace("%price%", String.valueOf(ahItem.price)).replace("%currency%", ahItem.currency.name()));

            lore.add(Lang.get("ah.expires_in").replace("%time%", formatTimeLeft(ahItem.timestamp)));
            lore.add("");

            if (ahItem.seller.equals(p.getUniqueId())) lore.add(Lang.get("ah.click_remove"));
            else lore.add(Lang.get("ah.click_buy"));

            meta.setLore(lore);
            meta.getPersistentDataContainer().set(keyAhId, PersistentDataType.INTEGER, ahItem.id);
            display.setItemMeta(meta);

            inv.setItem(slot++, display);
        }

        String queryToSave = (searchQuery == null) ? "" : searchQuery;

        if (page > 0) {
            ItemStack prev = createButton(Material.ARROW, Lang.get("btn.prev_page"));
            ItemMeta m = prev.getItemMeta();
            m.getPersistentDataContainer().set(keyAhPage, PersistentDataType.INTEGER, page - 1);
            m.getPersistentDataContainer().set(keyAhSearch, PersistentDataType.STRING, queryToSave);
            prev.setItemMeta(m);
            inv.setItem(45, prev);
        }
        if (end < filteredItems.size()) {
            ItemStack next = createButton(Material.ARROW, Lang.get("btn.next_page"));
            ItemMeta m = next.getItemMeta();
            m.getPersistentDataContainer().set(keyAhPage, PersistentDataType.INTEGER, page + 1);
            m.getPersistentDataContainer().set(keyAhSearch, PersistentDataType.STRING, queryToSave);
            next.setItemMeta(m);
            inv.setItem(53, next);
        }

        inv.setItem(48, createButton(Material.CHEST_MINECART, Lang.get("btn.ah_collection"), Lang.get("btn.ah_collection_lore")));

        ItemStack refreshBtn = createButton(Material.SUNFLOWER, Lang.get("btn.refresh"));
        ItemMeta rm = refreshBtn.getItemMeta();
        rm.getPersistentDataContainer().set(keyAhSearch, PersistentDataType.STRING, queryToSave);
        refreshBtn.setItemMeta(rm);
        inv.setItem(49, refreshBtn);

        inv.setItem(50, createButton(Material.WRITABLE_BOOK, Lang.get("btn.info"), Lang.get("btn.info_lore1"), Lang.get("btn.info_lore2")));

        p.openInventory(inv);
    }

    public void openAhChest(Player p, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, Lang.get("menu.ah_chest_prefix") + (page + 1));

        List<AhManager.AhReward> rawRewards = ahManager.getRewards(p.getUniqueId());
        List<ItemStack> allItems = new ArrayList<>();

        for (AhManager.AhReward rew : rawRewards) {
            int amountLeft = rew.amount;
            while (amountLeft > 0) {
                int stackSize = Math.min(amountLeft, rew.mat.getMaxStackSize());
                allItems.add(new ItemStack(rew.mat, stackSize));
                amountLeft -= stackSize;
            }
        }

        List<ItemStack> expiredItems = ahManager.getExpiredItems(p.getUniqueId());
        allItems.addAll(expiredItems);

        int start = page * 45;
        int end = Math.min(start + 45, allItems.size());
        int slot = 0;
        for (int i = start; i < end; i++) inv.setItem(slot++, allItems.get(i));

        if (page > 0) {
            ItemStack prev = createButton(Material.ARROW, Lang.get("btn.prev_page"));
            ItemMeta m = prev.getItemMeta(); m.getPersistentDataContainer().set(keyAhPage, PersistentDataType.INTEGER, page - 1); prev.setItemMeta(m);
            inv.setItem(45, prev);
        }
        if (end < allItems.size()) {
            ItemStack next = createButton(Material.ARROW, Lang.get("btn.next_page"));
            ItemMeta m = next.getItemMeta(); m.getPersistentDataContainer().set(keyAhPage, PersistentDataType.INTEGER, page + 1); next.setItemMeta(m);
            inv.setItem(53, next);
        }

        ItemStack takeAll = createButton(Material.HOPPER, Lang.get("btn.take_all"), Lang.get("btn.take_all_lore1"), Lang.get("btn.take_all_lore2"));
        ItemMeta tm = takeAll.getItemMeta(); tm.getPersistentDataContainer().set(keyAhPage, PersistentDataType.INTEGER, page); takeAll.setItemMeta(tm);

        inv.setItem(48, takeAll);
        inv.setItem(49, createButton(Material.BARRIER, Lang.get("btn.close_menu")));

        p.openInventory(inv);
    }

    public void openConfirmMenu(Player p, ItemStack itemToSell, Material currency, int price) {
        Inventory inv = Bukkit.createInventory(null, 27, Lang.get("menu.ah_sell_confirm"));

        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta cm = confirm.getItemMeta();
        cm.setDisplayName(Lang.get("btn.ah_sell").replace("%price%", String.valueOf(price)).replace("%currency%", currency.name()));
        cm.getPersistentDataContainer().set(keyAhPrice, PersistentDataType.INTEGER, price);
        cm.getPersistentDataContainer().set(keyAhCurrency, PersistentDataType.STRING, currency.name());
        confirm.setItemMeta(cm);

        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cam = cancel.getItemMeta(); cam.setDisplayName(Lang.get("btn.cancel")); cancel.setItemMeta(cam);

        inv.setItem(11, confirm);
        inv.setItem(13, itemToSell);
        inv.setItem(15, cancel);

        p.openInventory(inv);
    }

    public void openBuyConfirmMenu(Player p, int auctionId, ItemStack itemToBuy, int price) {
        Inventory inv = Bukkit.createInventory(null, 27, Lang.get("menu.ah_buy_confirm_prefix") + auctionId);

        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta cm = confirm.getItemMeta();
        cm.setDisplayName(Lang.get("btn.confirm"));
        cm.setLore(Arrays.asList(Lang.get("btn.confirm_lore").replace("%price%", String.valueOf(price))));
        cm.getPersistentDataContainer().set(keyAhId, PersistentDataType.INTEGER, auctionId);
        confirm.setItemMeta(cm);

        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cam = cancel.getItemMeta();
        cam.setDisplayName(Lang.get("btn.cancel"));
        cancel.setItemMeta(cam);

        inv.setItem(11, confirm);
        inv.setItem(13, itemToBuy);
        inv.setItem(15, cancel);

        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (e.getClickedInventory() == null) return;
        String title = e.getView().getTitle();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (title.equals(Lang.get("menu.ah_sell_confirm"))) {
            e.setCancelled(true);
            if (clicked.getType() == Material.LIME_STAINED_GLASS_PANE) {
                ItemStack itemToSell = e.getInventory().getItem(13);
                if (itemToSell == null) return;

                int price = clicked.getItemMeta().getPersistentDataContainer().get(keyAhPrice, PersistentDataType.INTEGER);
                Material currency = Material.valueOf(clicked.getItemMeta().getPersistentDataContainer().get(keyAhCurrency, PersistentDataType.STRING));

                ahManager.addItem(p.getUniqueId(), itemToSell, currency, price);
                e.getInventory().setItem(13, null);

                p.sendMessage(Lang.getPrefixed("msg.ah_item_added"));
                p.closeInventory();
                openMainMenu(p, 0, "");
            } else if (clicked.getType() == Material.RED_STAINED_GLASS_PANE) {
                p.closeInventory();
            }
            return;
        }

        if (title.startsWith(Lang.get("menu.ah_buy_confirm_prefix"))) {
            e.setCancelled(true);
            if (clicked.getType() == Material.RED_STAINED_GLASS_PANE) { openMainMenu(p, 0, ""); return; }
            if (clicked.getType() == Material.LIME_STAINED_GLASS_PANE) {
                int id = clicked.getItemMeta().getPersistentDataContainer().get(keyAhId, PersistentDataType.INTEGER);
                AhItem ahItem = ahManager.getItem(id);

                if (ahItem == null) {
                    p.sendMessage(Lang.getPrefixed("msg.ah_already_bought"));
                    openMainMenu(p, 0, ""); return;
                }

                if (hasEnoughItems(p, ahItem.currency, ahItem.price)) {
                    removeItems(p, ahItem.currency, ahItem.price);
                    ItemStack original = ahManager.itemFromBase64(ahItem.itemBase64);
                    HashMap<Integer, ItemStack> left = p.getInventory().addItem(original);
                    if (!left.isEmpty()) p.getWorld().dropItem(p.getLocation(), left.get(0));

                    ahManager.addReward(ahItem.seller, ahItem.currency, ahItem.price);

                    Player seller = Bukkit.getPlayer(ahItem.seller);
                    if (seller != null) seller.sendMessage(Lang.getPrefixed("msg.ah_item_sold"));

                    ahManager.removeItem(id);
                    p.sendMessage(Lang.getPrefixed("msg.ah_buy_success"));
                    openMainMenu(p, 0, "");
                } else {
                    p.sendMessage(Lang.getPrefixed("msg.ah_not_enough_currency").replace("%currency%", ahItem.currency.name()));
                    openMainMenu(p, 0, "");
                }
            }
            return;
        }

        if (title.startsWith(Lang.get("menu.ah_chest_prefix"))) {
            e.setCancelled(true);

            // Blockiert Klicks in das eigene Inventar!
            if (e.getClickedInventory() != e.getView().getTopInventory()) return;

            // HIER WAR DER FEHLER! Sprachen-unabhängige Seiten-Auslese:
            int page = 0;
            try {
                String[] parts = title.split(" ");
                page = Integer.parseInt(parts[parts.length - 1]) - 1;
            } catch (Exception ignored) {}

            if (clicked.getType() == Material.BARRIER) { openMainMenu(p, 0, ""); return; }
            if (clicked.getType() == Material.ARROW && clicked.hasItemMeta() && clicked.getItemMeta().getPersistentDataContainer().has(keyAhPage, PersistentDataType.INTEGER)) {
                openAhChest(p, clicked.getItemMeta().getPersistentDataContainer().get(keyAhPage, PersistentDataType.INTEGER)); return;
            }
            if (clicked.getType() == Material.HOPPER) {
                claimAhPage(p, e.getInventory(), page);
                return;
            }

            if (e.getSlot() < 45) {
                ItemStack cloneToGive = clicked.clone();
                HashMap<Integer, ItemStack> left = p.getInventory().addItem(cloneToGive);

                if (left.isEmpty()) {
                    removeFromChestBackend(p.getUniqueId(), clicked);
                    openAhChest(p, page); // Läd sofort neu!
                } else {
                    p.sendMessage(Lang.getPrefixed("msg.inv_full"));
                }
            }
            return;
        }

        if (title.startsWith(Lang.get("menu.ah_main_prefix").split("\\|")[0].trim())) {
            e.setCancelled(true);

            if (e.getClickedInventory() != e.getView().getTopInventory()) return;

            // HIER AUCH: Sprachen-unabhängig!
            int currentPage = 0;
            try {
                String[] parts = title.split(" ");
                currentPage = Integer.parseInt(parts[parts.length - 1]) - 1;
            } catch (Exception ignored) {}

            String currentSearch = "";
            if (clicked.hasItemMeta() && clicked.getItemMeta().getPersistentDataContainer().has(keyAhSearch, PersistentDataType.STRING)) {
                currentSearch = clicked.getItemMeta().getPersistentDataContainer().get(keyAhSearch, PersistentDataType.STRING);
            }

            if (clicked.getType() == Material.CHEST_MINECART) { openAhChest(p, 0); return; }
            if (clicked.getType() == Material.SUNFLOWER) { openMainMenu(p, currentPage, currentSearch); return; }
            if (clicked.getType() == Material.ARROW && clicked.hasItemMeta() && clicked.getItemMeta().getPersistentDataContainer().has(keyAhPage, PersistentDataType.INTEGER)) {
                openMainMenu(p, clicked.getItemMeta().getPersistentDataContainer().get(keyAhPage, PersistentDataType.INTEGER), currentSearch); return;
            }

            if (clicked.hasItemMeta() && clicked.getItemMeta().getPersistentDataContainer().has(keyAhId, PersistentDataType.INTEGER)) {
                int id = clicked.getItemMeta().getPersistentDataContainer().get(keyAhId, PersistentDataType.INTEGER);
                AhItem ahItem = ahManager.getItem(id);
                if (ahItem == null) { p.sendMessage(Lang.getPrefixed("msg.ah_item_missing")); openMainMenu(p, currentPage, currentSearch); return; }

                if (ahItem.seller.equals(p.getUniqueId())) {
                    ahManager.removeItem(id);

                    ItemStack original = ahManager.itemFromBase64(ahItem.itemBase64);
                    HashMap<Integer, ItemStack> left = p.getInventory().addItem(original.clone());

                    if (left.isEmpty()) {
                        String msgInv = Lang.getPrefixed("msg.ah_item_removed_inv");
                        if (msgInv == null || msgInv.contains("msg.ah_item_removed_inv")) msgInv = "§aDas Item wurde zurück in dein Inventar gelegt.";
                        p.sendMessage(msgInv);
                    } else {
                        List<ItemStack> expired = ahManager.getExpiredItems(p.getUniqueId());
                        expired.add(left.get(0));
                        ahManager.setExpiredItems(p.getUniqueId(), expired);

                        String msgChest = Lang.getPrefixed("msg.ah_item_removed_chest");
                        if (msgChest == null || msgChest.contains("msg.ah_item_removed_chest")) msgChest = "§eDein Inventar ist voll! Das Item liegt in der Abholstation.";
                        p.sendMessage(msgChest);
                    }

                    openMainMenu(p, currentPage, currentSearch);
                } else {
                    ItemStack displayItem = clicked.clone();
                    ItemMeta meta = displayItem.getItemMeta();
                    if (meta != null && meta.hasLore()) {
                        List<String> lore = meta.getLore();
                        lore.remove(lore.size() - 1);
                        lore.remove(lore.size() - 1);
                        meta.setLore(lore);
                        displayItem.setItemMeta(meta);
                    }
                    openBuyConfirmMenu(p, id, displayItem, ahItem.price);
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getView().getTitle().equals(Lang.get("menu.ah_sell_confirm"))) {
            ItemStack item = e.getInventory().getItem(13);
            if (item != null && item.getType() != Material.AIR) {
                e.getPlayer().getInventory().addItem(item);
                e.getPlayer().sendMessage(Lang.getPrefixed("msg.ah_sell_cancelled"));
            }
        }
    }

    private void removeFromChestBackend(UUID uuid, ItemStack clicked) {
        List<ItemStack> expired = ahManager.getExpiredItems(uuid);
        for (int i = 0; i < expired.size(); i++) {
            if (expired.get(i).getType() == clicked.getType() && expired.get(i).getAmount() == clicked.getAmount()) {
                expired.remove(i);
                ahManager.setExpiredItems(uuid, expired);
                return;
            }
        }

        List<AhManager.AhReward> rewards = ahManager.getRewards(uuid);
        for (AhManager.AhReward rew : rewards) {
            if (rew.mat == clicked.getType()) {
                if (rew.amount >= clicked.getAmount()) {
                    rew.amount -= clicked.getAmount();
                    break;
                }
            }
        }
        rewards.removeIf(r -> r.amount <= 0);
        ahManager.setRewards(uuid, rewards);
    }

    private void claimAhPage(Player p, Inventory inv, int page) {
        boolean changed = false;
        for (int i = 0; i < 45; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                ItemStack cloneToGive = item.clone();
                HashMap<Integer, ItemStack> left = p.getInventory().addItem(cloneToGive);
                if (left.isEmpty()) {
                    removeFromChestBackend(p.getUniqueId(), item);
                    changed = true;
                } else {
                    p.sendMessage(Lang.getPrefixed("msg.inv_full"));
                    break;
                }
            }
        }
        if (changed) {
            openAhChest(p, page);
        }
    }

    private boolean hasEnoughItems(Player p, Material mat, int amount) {
        int count = 0;
        for (ItemStack is : p.getInventory().getContents()) { if (is != null && is.getType() == mat) count += is.getAmount(); }
        return count >= amount;
    }

    private void removeItems(Player p, Material mat, int amount) {
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack is = contents[i];
            if (is != null && is.getType() == mat) {
                int count = is.getAmount();
                if (count > amount) { is.setAmount(count - amount); break; }
                else { p.getInventory().setItem(i, null); amount -= count; if (amount <= 0) break; }
            }
        }
    }

    private ItemStack createButton(Material m, String name, String... lore) {
        ItemStack i = new ItemStack(m); ItemMeta mt = i.getItemMeta(); mt.setDisplayName(name);
        if (lore.length > 0) mt.setLore(Arrays.asList(lore));
        i.setItemMeta(mt); return i;
    }
}