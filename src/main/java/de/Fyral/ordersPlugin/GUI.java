package de.Fyral.ordersPlugin;

import de.Fyral.ordersPlugin.createorder.*;
import de.Fyral.ordersPlugin.deliver.DeliverManager;
import de.rapha149.signgui.SignGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class GUI implements Listener {

    private final OrdersPlugin plugin;
    private final DataManager db;
    private final de.Fyral.ordersPlugin.deliver.Buy buy;
    private final NamespacedKey keyOrderId, keyMaterial, keyPage;
    private final Map<UUID, OrderSession> activeSessions = new HashMap<>();

    public GUI(OrdersPlugin plugin, DataManager db, de.Fyral.ordersPlugin.deliver.Buy buy) {
        this.plugin = plugin;
        this.db = db;
        this.buy = buy;
        this.keyOrderId = new NamespacedKey(plugin, "order_id");
        this.keyMaterial = new NamespacedKey(plugin, "material_name");
        this.keyPage = new NamespacedKey(plugin, "page_number");
    }

    public void openMainMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, Lang.get("menu.orders_main"));
        activeSessions.remove(p.getUniqueId());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<OrderData> orders = db.getAllOrders();
            Bukkit.getScheduler().runTask(plugin, () -> {
                int slot = 0;
                for (OrderData data : orders) {
                    if (slot > 44) break;
                    if (data.status.equals("ACTIVE")) {
                        ItemStack display = createOrderDisplayItem(p, data);
                        if (display != null) inv.setItem(slot++, display);
                    }
                }
                inv.setItem(48, createButton(Material.BOOK, Lang.get("btn.my_orders"), Lang.get("btn.my_orders_lore1"), Lang.get("btn.my_orders_lore2")));
                inv.setItem(49, createButton(Material.EMERALD, Lang.get("btn.create_order")));
                inv.setItem(50, createButton(Material.SUNFLOWER, Lang.get("btn.refresh"), Lang.get("btn.refresh_lore")));
                inv.setItem(51, createButton(Material.WRITABLE_BOOK, Lang.get("btn.info"), Lang.get("btn.info_lore1"), Lang.get("btn.info_lore2")));

                p.openInventory(inv);
            });
        });
    }

    public void openMyOrders(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, Lang.get("menu.my_orders"));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<OrderData> orders = db.getAllOrders();
            Bukkit.getScheduler().runTask(plugin, () -> {
                int slot = 0;
                for (OrderData data : orders) {
                    if (data.owner.equals(p.getUniqueId()) && slot <= 44) {
                        ItemStack display = createOrderDisplayItem(p, data);
                        if (display != null) inv.setItem(slot++, display);
                    }
                }
                inv.setItem(49, createButton(Material.ARROW, Lang.get("btn.back"), Lang.get("btn.back_lore")));
                p.openInventory(inv);
            });
        });
    }

    public void openManageMenu(Player p, int orderId) {
        OrderData data = db.getOrder(orderId);
        if (data == null) return;
        Inventory inv = Bukkit.createInventory(null, 27, Lang.get("menu.manage_order_prefix") + orderId);
        if (data.status.equals("ACTIVE")) {
            ItemStack cancelBtn = createButton(Material.RED_WOOL, Lang.get("btn.cancel_order"));
            ItemMeta cm = cancelBtn.getItemMeta(); cm.getPersistentDataContainer().set(keyOrderId, PersistentDataType.INTEGER, orderId); cancelBtn.setItemMeta(cm);
            inv.setItem(11, cancelBtn);
        }
        ItemStack chestBtn = createButton(Material.CHEST, Lang.get("btn.open_chest"), Lang.get("btn.open_chest_lore"));
        ItemMeta chm = chestBtn.getItemMeta(); chm.getPersistentDataContainer().set(keyOrderId, PersistentDataType.INTEGER, orderId); chestBtn.setItemMeta(chm);
        inv.setItem(15, chestBtn);
        inv.setItem(18, createButton(Material.ARROW, Lang.get("btn.back")));
        p.openInventory(inv);
    }

    public void openOrderChest(Player p, int orderId, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, Lang.get("menu.order_chest_prefix") + orderId + " | S." + (page + 1));
        List<DeliverManager.ItemStackData> rawRewards = plugin.getDeliverManager().getRewards(orderId);
        List<ItemStack> allItems = new ArrayList<>();
        for (DeliverManager.ItemStackData rew : rawRewards) {
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
            ItemMeta m = prev.getItemMeta(); m.getPersistentDataContainer().set(keyOrderId, PersistentDataType.INTEGER, orderId); m.getPersistentDataContainer().set(keyPage, PersistentDataType.INTEGER, page - 1); prev.setItemMeta(m);
            inv.setItem(45, prev);
        }
        if (end < allItems.size()) {
            ItemStack next = createButton(Material.ARROW, Lang.get("btn.next_page"));
            ItemMeta m = next.getItemMeta(); m.getPersistentDataContainer().set(keyOrderId, PersistentDataType.INTEGER, orderId); m.getPersistentDataContainer().set(keyPage, PersistentDataType.INTEGER, page + 1); next.setItemMeta(m);
            inv.setItem(53, next);
        }
        ItemStack takeAll = createButton(Material.HOPPER, Lang.get("btn.take_all"), Lang.get("btn.take_all_lore1"), Lang.get("btn.take_all_lore2"));
        ItemMeta tm = takeAll.getItemMeta(); tm.getPersistentDataContainer().set(keyOrderId, PersistentDataType.INTEGER, orderId); tm.getPersistentDataContainer().set(keyPage, PersistentDataType.INTEGER, page); takeAll.setItemMeta(tm);
        inv.setItem(48, takeAll);
        inv.setItem(49, createButton(Material.BARRIER, Lang.get("btn.close_menu")));
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (e.getClickedInventory() == null) return;
        String title = e.getView().getTitle();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        OrderSession s = activeSessions.get(p.getUniqueId());

        if (title.startsWith(Lang.get("menu.order_chest_prefix"))) {
            e.setCancelled(true);
            int orderId = Integer.parseInt(title.split("#")[1].split("\\|")[0].trim());
            int page = Integer.parseInt(title.split("S\\.")[1].trim()) - 1;
            if (clicked.getType() == Material.BARRIER) { openManageMenu(p, orderId); return; }
            if (clicked.getType() == Material.ARROW && clicked.getItemMeta().getPersistentDataContainer().has(keyPage, PersistentDataType.INTEGER)) {
                openOrderChest(p, orderId, clicked.getItemMeta().getPersistentDataContainer().get(keyPage, PersistentDataType.INTEGER)); return;
            }
            if (clicked.getType() == Material.HOPPER) {
                claimPage(p, orderId, e.getInventory()); checkAndDeleteOrder(orderId); openOrderChest(p, orderId, page); return;
            }
            if (e.getSlot() < 45) {
                HashMap<Integer, ItemStack> left = p.getInventory().addItem(clicked);
                if (left.isEmpty()) {
                    removeRewardFromManager(orderId, clicked.getType(), clicked.getAmount());
                    e.getInventory().setItem(e.getSlot(), null); checkAndDeleteOrder(orderId);
                } else { p.sendMessage(Lang.getPrefixed("msg.inv_full")); }
            }
            return;
        }

        if (title.contains("▶") || title.contains("Suchergebnisse") || title.contains("Auswahl")) {
            e.setCancelled(true);
            if (title.startsWith(Lang.get("menu.manage_order_prefix"))) {
                if (clicked.getType() == Material.RED_WOOL) { cancelOrder(p, clicked.getItemMeta().getPersistentDataContainer().get(keyOrderId, PersistentDataType.INTEGER)); return; }
                else if (clicked.getType() == Material.CHEST) { openOrderChest(p, clicked.getItemMeta().getPersistentDataContainer().get(keyOrderId, PersistentDataType.INTEGER), 0); return; }
            }
            if (clicked.getType() == Material.SUNFLOWER) { openMainMenu(p); return; }
            if (clicked.getItemMeta().getDisplayName().contains(Lang.get("btn.back"))) { openMainMenu(p); return; }
            if (clicked.getType() == Material.ARROW && s != null) {
                if (e.getSlot() == 53) { s.currentPage++; SearchGUI.showResults(p, s, this, title.contains("Bezahl-Item")); }
                else if (e.getSlot() == 45 && s.currentPage > 0) { s.currentPage--; SearchGUI.showResults(p, s, this, title.contains("Bezahl-Item")); }
                return;
            }
            if (clicked.getItemMeta().getPersistentDataContainer().has(keyOrderId, PersistentDataType.INTEGER)) {
                int id = clicked.getItemMeta().getPersistentDataContainer().get(keyOrderId, PersistentDataType.INTEGER);
                OrderData data = db.getOrder(id);
                if (data != null) {
                    if (data.owner.equals(p.getUniqueId())) openManageMenu(p, id);
                    else if (data.status.equals("ACTIVE")) buy.openDeliveryMenu(p, id);
                }
                return;
            }
            if (title.equals(Lang.get("menu.orders_main"))) {
                if (e.getSlot() == 49) {
                    activeSessions.put(p.getUniqueId(), new OrderSession());
                    promptSign(p, Lang.get("msg.sign_search_item"), Lang.get("msg.sign_search_hint"));
                }
                else if (e.getSlot() == 48) openMyOrders(p);
            }
            else if (title.contains("Suchergebnisse") || title.contains("Auswahl")) { handleSelection(p, s, clicked); }
            else if (title.equals(Lang.get("menu.price_mode"))) {
                s.isPerItem = (e.getSlot() == 11);
                s.step = 2;
                promptSign(p, Lang.get("msg.sign_search_pay_item"), Lang.get("msg.sign_search_pay_hint"));
            }
            else if (title.equals(Lang.get("menu.confirm_order")) && e.getSlot() == 11) completeOrderCreation(p, s);
        }
    }

    private void promptSign(Player p, String line2, String line3) {
        p.closeInventory();
        try {
            SignGUI.builder()
                    .setLines("", "§8^^^^^^^^^^^^^^^", line2, line3)
                    .setType(Material.OAK_SIGN)
                    .setHandler((player, result) -> {
                        String input = result.getLineWithoutColor(0).trim();
                        if (input.isEmpty()) {
                            activeSessions.remove(player.getUniqueId());
                            player.sendMessage(Lang.getPrefixed("msg.creation_cancelled"));
                            return Collections.emptyList();
                        }
                        Bukkit.getScheduler().runTask(plugin, () -> processSignInput(player, input));
                        return Collections.emptyList();
                    })
                    .build().open(p);
        } catch (Exception e) {
            p.sendMessage(Lang.getPrefixed("msg.gui_error"));
        }
    }

    private void processSignInput(Player p, String input) {
        OrderSession s = activeSessions.get(p.getUniqueId());
        if (s == null) return;
        switch (s.step) {
            case 0: searchItems(p, s, input, false); break;
            case 1:
                try { s.amount = Math.max(1, Integer.parseInt(input)); openModeMenu(p); }
                catch (Exception ex) { p.sendMessage(Lang.getPrefixed("msg.enter_number")); activeSessions.remove(p.getUniqueId()); } break;
            case 2: searchItems(p, s, input, true); break;
            case 3:
                try {
                    int pr = Math.max(1, Integer.parseInt(input)); s.totalPaymentRequired = s.isPerItem ? (s.amount * pr) : pr; ConfirmGUI.open(p, s, this);
                } catch (Exception ex) { p.sendMessage(Lang.getPrefixed("msg.enter_number")); activeSessions.remove(p.getUniqueId()); } break;
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) { activeSessions.remove(e.getPlayer().getUniqueId()); }

    private void handleSelection(Player p, OrderSession s, ItemStack clicked) {
        String matStr = clicked.getItemMeta().getPersistentDataContainer().get(keyMaterial, PersistentDataType.STRING);
        if (matStr == null) return;
        Material mat = Material.valueOf(matStr);
        s.currentPage = 0;
        if (s.step == 0) {
            s.wantedMaterial = mat; s.step = 1; promptSign(p, Lang.get("msg.sign_amount"), Lang.get("msg.sign_amount_hint"));
        } else if (s.step == 2) {
            s.paymentMaterial = mat; s.step = 3; promptSign(p, s.isPerItem ? Lang.get("msg.sign_price_per") : Lang.get("msg.sign_price_total"), s.isPerItem ? Lang.get("msg.sign_price_hint1") : Lang.get("msg.sign_price_hint2"));
        }
    }

    private void searchItems(Player p, OrderSession s, String input, boolean isPay) {
        s.currentResults.clear();
        String query = input.toUpperCase().replace(" ", "_");
        for (Material m : Material.values()) { if (m.isItem() && m.name().contains(query)) s.currentResults.add(m); }
        if (s.currentResults.isEmpty()) {
            p.sendMessage(Lang.getPrefixed("msg.item_not_found")); activeSessions.remove(p.getUniqueId());
        } else { SearchGUI.showResults(p, s, this, isPay); }
    }

    public void openModeMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, Lang.get("menu.price_mode"));
        inv.setItem(11, createButton(Material.GOLD_NUGGET, Lang.get("btn.price_per_item")));
        inv.setItem(15, createButton(Material.GOLD_INGOT, Lang.get("btn.price_total")));
        p.openInventory(inv);
    }

    private void removeRewardFromManager(int orderId, Material mat, int amount) {
        List<DeliverManager.ItemStackData> rewards = plugin.getDeliverManager().getRewards(orderId);
        for (DeliverManager.ItemStackData rew : rewards) {
            if (rew.mat == mat) {
                if (rew.amount >= amount) { rew.amount -= amount; break; }
                else { amount -= rew.amount; rew.amount = 0; }
            }
        }
        rewards.removeIf(r -> r.amount <= 0);
        plugin.getDeliverManager().setRewards(orderId, rewards);
    }

    private void claimPage(Player p, int orderId, Inventory inv) {
        for (int i = 0; i < 45; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                HashMap<Integer, ItemStack> left = p.getInventory().addItem(item);
                if (left.isEmpty()) { removeRewardFromManager(orderId, item.getType(), item.getAmount()); inv.setItem(i, null); }
                else { p.sendMessage(Lang.getPrefixed("msg.inv_full")); break; }
            }
        }
    }

    private void checkAndDeleteOrder(int orderId) {
        OrderData data = db.getOrder(orderId);
        if (data != null && !data.status.equals("ACTIVE")) {
            List<DeliverManager.ItemStackData> rew = plugin.getDeliverManager().getRewards(orderId);
            if (rew.isEmpty()) db.deleteOrder(orderId);
        }
    }

    private void cancelOrder(Player p, int id) {
        OrderData data = db.getOrder(id);
        if (data != null) {
            String[] pParts = data.priceBase64.split(":");
            plugin.getDeliverManager().addReward(id, Material.valueOf(pParts[0]), Integer.parseInt(pParts[1]));
            db.setStatus(id, "CANCELLED");
            p.sendMessage(Lang.getPrefixed("msg.order_cancel_success"));
            openManageMenu(p, id);
        }
    }

    private void completeOrderCreation(Player p, OrderSession s) {
        if (!hasEnoughItems(p, s.paymentMaterial, s.totalPaymentRequired)) { p.sendMessage(Lang.getPrefixed("msg.not_enough_items")); return; }
        removeItems(p, s.paymentMaterial, s.totalPaymentRequired);
        p.updateInventory();
        db.createOrder(p.getUniqueId(), s.wantedMaterial.name(), s.amount, s.paymentMaterial.name(), s.totalPaymentRequired, s.isPerItem);
        p.sendMessage(Lang.getPrefixed("msg.order_created"));
        p.closeInventory(); openMainMenu(p);
    }

    private ItemStack createOrderDisplayItem(Player viewer, OrderData data) {
        try {
            String[] w = data.wantedBase64.split(":"); String[] pr = data.priceBase64.split(":");
            Material wM = Material.valueOf(w[0]); int wAmt = Integer.parseInt(w[1]); boolean isPerItem = Boolean.parseBoolean(w[2]);
            Material pM = Material.valueOf(pr[0]); int pAmt = Integer.parseInt(pr[1]);
            ItemStack i = new ItemStack(wM); ItemMeta m = i.getItemMeta();
            m.setDisplayName(Lang.get("order.name").replace("%id%", String.valueOf(data.id)));
            String ownerName = Bukkit.getOfflinePlayer(data.owner).getName();
            if (ownerName == null) ownerName = "Unbekannt";
            List<String> lore = new ArrayList<>();
            lore.add(Lang.get("order.creator").replace("%name%", ownerName));
            lore.add(Lang.get("order.wanted").replace("%amount%", String.valueOf(wAmt)).replace("%item%", wM.name()));
            if (isPerItem) {
                lore.add(Lang.get("order.price_per").replace("%price%", String.valueOf(pAmt / wAmt)).replace("%currency%", pM.name()));
                lore.add(Lang.get("order.partial_allowed"));
                lore.add(Lang.get("order.total_price_info").replace("%price%", String.valueOf(pAmt)));
            } else { lore.add(Lang.get("order.price_total").replace("%price%", String.valueOf(pAmt)).replace("%currency%", pM.name())); }
            lore.add("");
            if (data.status.equals("COMPLETED")) { lore.add(Lang.get("order.completed")); }
            else if (data.status.equals("CANCELLED")) { lore.add(Lang.get("order.cancelled")); }
            else {
                if (data.owner.equals(viewer.getUniqueId())) { lore.add(Lang.get("order.yours")); lore.add(Lang.get("order.click_manage")); }
                else { lore.add(Lang.get("order.click_deliver")); }
            }
            m.setLore(lore); m.getPersistentDataContainer().set(keyOrderId, PersistentDataType.INTEGER, data.id); i.setItemMeta(m); return i;
        } catch (Exception e) { return null; }
    }

    private boolean hasEnoughItems(Player p, Material mat, int amount) {
        int count = 0; for (ItemStack is : p.getInventory().getContents()) { if (is != null && is.getType() == mat) count += is.getAmount(); } return count >= amount;
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

    // HIER IST DER FIX: Die Methode ist jetzt public!
    public ItemStack createButton(Material m, String name, String... lore) {
        ItemStack i = new ItemStack(m); ItemMeta mt = i.getItemMeta(); mt.setDisplayName(name);
        if (lore.length > 0) mt.setLore(Arrays.asList(lore)); i.setItemMeta(mt); return i;
    }

    public NamespacedKey getKeyMaterial() { return keyMaterial; }
    public NamespacedKey getKeyOrderId() { return keyOrderId; }
}