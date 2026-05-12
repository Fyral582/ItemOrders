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

public class AhGUI implements Listener {

    private final OrdersPlugin plugin;
    private final AhManager ahManager;
    private final NamespacedKey keyAhId, keyAhPrice, keyAhCurrency, keyAhPage;

    public AhGUI(OrdersPlugin plugin, AhManager ahManager) {
        this.plugin = plugin;
        this.ahManager = ahManager;
        this.keyAhId = new NamespacedKey(plugin, "ah_id");
        this.keyAhPrice = new NamespacedKey(plugin, "ah_price");
        this.keyAhCurrency = new NamespacedKey(plugin, "ah_currency");
        this.keyAhPage = new NamespacedKey(plugin, "ah_page");
    }

    public void openMainMenu(Player p, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, Lang.get("menu.ah_main_prefix") + (page + 1));

        List<AhItem> items = ahManager.getAllItems();
        int start = page * 45;
        int end = Math.min(start + 45, items.size());
        int slot = 0;

        for (int i = start; i < end; i++) {
            AhItem ahItem = items.get(i);
            ItemStack original = ahManager.itemFromBase64(ahItem.itemBase64);
            ItemStack display = original.clone();
            ItemMeta meta = display.getItemMeta();

            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            String sellerName = Bukkit.getOfflinePlayer(ahItem.seller).getName();
            if (sellerName == null) sellerName = "Unbekannt";

            lore.add("§8§m----------------------");
            lore.add(Lang.get("ah.seller").replace("%name%", sellerName));
            lore.add(Lang.get("ah.price").replace("%price%", String.valueOf(ahItem.price)).replace("%currency%", ahItem.currency.name()));
            lore.add("");

            if (ahItem.seller.equals(p.getUniqueId())) lore.add(Lang.get("ah.click_remove"));
            else lore.add(Lang.get("ah.click_buy"));

            meta.setLore(lore);
            meta.getPersistentDataContainer().set(keyAhId, PersistentDataType.INTEGER, ahItem.id);
            display.setItemMeta(meta);

            inv.setItem(slot++, display);
        }

        if (page > 0) {
            ItemStack prev = createButton(Material.ARROW, Lang.get("btn.prev_page"));
            ItemMeta m = prev.getItemMeta(); m.getPersistentDataContainer().set(keyAhPage, PersistentDataType.INTEGER, page - 1); prev.setItemMeta(m);
            inv.setItem(45, prev);
        }
        if (end < items.size()) {
            ItemStack next = createButton(Material.ARROW, Lang.get("btn.next_page"));
            ItemMeta m = next.getItemMeta(); m.getPersistentDataContainer().set(keyAhPage, PersistentDataType.INTEGER, page + 1); next.setItemMeta(m);
            inv.setItem(53, next);
        }

        inv.setItem(48, createButton(Material.CHEST_MINECART, Lang.get("btn.ah_collection"), Lang.get("btn.ah_collection_lore")));
        inv.setItem(49, createButton(Material.SUNFLOWER, Lang.get("btn.refresh")));
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
                openMainMenu(p, 0);
            } else if (clicked.getType() == Material.RED_STAINED_GLASS_PANE) {
                p.closeInventory();
            }
            return;
        }

        if (title.startsWith(Lang.get("menu.ah_buy_confirm_prefix"))) {
            e.setCancelled(true);
            if (clicked.getType() == Material.RED_STAINED_GLASS_PANE) { openMainMenu(p, 0); return; }
            if (clicked.getType() == Material.LIME_STAINED_GLASS_PANE) {
                int id = clicked.getItemMeta().getPersistentDataContainer().get(keyAhId, PersistentDataType.INTEGER);
                AhItem ahItem = ahManager.getItem(id);

                if (ahItem == null) {
                    p.sendMessage(Lang.getPrefixed("msg.ah_already_bought"));
                    openMainMenu(p, 0); return;
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
                    openMainMenu(p, 0);
                } else {
                    p.sendMessage(Lang.getPrefixed("msg.ah_not_enough_currency").replace("%currency%", ahItem.currency.name()));
                    openMainMenu(p, 0);
                }
            }
            return;
        }

        if (title.startsWith(Lang.get("menu.ah_chest_prefix"))) {
            e.setCancelled(true);
            int page = Integer.parseInt(title.split("S\\.")[1].trim()) - 1;

            if (clicked.getType() == Material.BARRIER) { openMainMenu(p, 0); return; }
            if (clicked.getType() == Material.ARROW && clicked.hasItemMeta() && clicked.getItemMeta().getPersistentDataContainer().has(keyAhPage, PersistentDataType.INTEGER)) {
                openAhChest(p, clicked.getItemMeta().getPersistentDataContainer().get(keyAhPage, PersistentDataType.INTEGER)); return;
            }
            if (clicked.getType() == Material.HOPPER) {
                claimAhPage(p, e.getInventory());
                openAhChest(p, page); return;
            }

            if (e.getSlot() < 45) {
                HashMap<Integer, ItemStack> left = p.getInventory().addItem(clicked);
                if (left.isEmpty()) {
                    removeAhReward(p.getUniqueId(), clicked.getType(), clicked.getAmount());
                    e.getInventory().setItem(e.getSlot(), null);
                } else { p.sendMessage(Lang.getPrefixed("msg.inv_full")); }
            }
            return;
        }

        if (title.startsWith(Lang.get("menu.ah_main_prefix").split("\\|")[0].trim())) {
            e.setCancelled(true);
            int currentPage = 0;
            if (title.contains("S.")) currentPage = Integer.parseInt(title.split("S\\.")[1].trim()) - 1;

            if (clicked.getType() == Material.CHEST_MINECART) { openAhChest(p, 0); return; }
            if (clicked.getType() == Material.SUNFLOWER) { openMainMenu(p, currentPage); return; }
            if (clicked.getType() == Material.ARROW && clicked.hasItemMeta() && clicked.getItemMeta().getPersistentDataContainer().has(keyAhPage, PersistentDataType.INTEGER)) {
                openMainMenu(p, clicked.getItemMeta().getPersistentDataContainer().get(keyAhPage, PersistentDataType.INTEGER)); return;
            }

            if (clicked.hasItemMeta() && clicked.getItemMeta().getPersistentDataContainer().has(keyAhId, PersistentDataType.INTEGER)) {
                int id = clicked.getItemMeta().getPersistentDataContainer().get(keyAhId, PersistentDataType.INTEGER);
                AhItem ahItem = ahManager.getItem(id);
                if (ahItem == null) { p.sendMessage(Lang.getPrefixed("msg.ah_item_missing")); openMainMenu(p, currentPage); return; }

                if (ahItem.seller.equals(p.getUniqueId())) {
                    ahManager.removeItem(id);
                    HashMap<Integer, ItemStack> left = p.getInventory().addItem(ahManager.itemFromBase64(ahItem.itemBase64));
                    if (!left.isEmpty()) p.getWorld().dropItem(p.getLocation(), left.get(0));
                    p.sendMessage(Lang.getPrefixed("msg.ah_item_removed"));
                    openMainMenu(p, currentPage);
                } else {
                    ItemStack displayItem = clicked.clone();
                    ItemMeta meta = displayItem.getItemMeta();
                    if (meta != null && meta.hasLore()) {
                        List<String> lore = meta.getLore();
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

    private void removeAhReward(java.util.UUID uuid, Material mat, int amount) {
        List<AhManager.AhReward> rewards = ahManager.getRewards(uuid);
        for (AhManager.AhReward rew : rewards) {
            if (rew.mat == mat) {
                if (rew.amount >= amount) { rew.amount -= amount; break; }
                else { amount -= rew.amount; rew.amount = 0; }
            }
        }
        rewards.removeIf(r -> r.amount <= 0);
        ahManager.setRewards(uuid, rewards);
    }

    private void claimAhPage(Player p, Inventory inv) {
        for (int i = 0; i < 45; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                HashMap<Integer, ItemStack> left = p.getInventory().addItem(item);
                if (left.isEmpty()) {
                    removeAhReward(p.getUniqueId(), item.getType(), item.getAmount());
                    inv.setItem(i, null);
                } else { p.sendMessage(Lang.getPrefixed("msg.inv_full")); break; }
            }
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