package de.Fyral.ordersPlugin;

import de.Fyral.ordersPlugin.createorder.*;
import de.Fyral.ordersPlugin.deliver.DeliverManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class GUI implements Listener {

    private final OrdersPlugin plugin;
    private final DataManager db;
    private final Buy buy;
    private final NamespacedKey keyOrderId, keyMaterial, keyPage;
    private final Map<UUID, OrderSession> activeSessions = new HashMap<>();

    public GUI(OrdersPlugin plugin, DataManager db, Buy buy) {
        this.plugin = plugin;
        this.db = db;
        this.buy = buy;
        this.keyOrderId = new NamespacedKey(plugin, "order_id");
        this.keyMaterial = new NamespacedKey(plugin, "material_name");
        this.keyPage = new NamespacedKey(plugin, "page_number");
    }

    public void openMainMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§8▶ §6Alle Orders");
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
                inv.setItem(48, createButton(Material.BOOK, "§b§lMeine Orders", "§7Hier findest du auch", "§7abgeschlossene Orders zur Abholung."));
                inv.setItem(49, createButton(Material.EMERALD, "§a§lOrder erstellen"));
                inv.setItem(50, createButton(Material.SUNFLOWER, "§e§lAktualisieren", "§7Klicke zum Neuladen."));
                inv.setItem(53, createButton(Material.WRITABLE_BOOK, "§6§lPlugin Information", "§7Plugin erstellt von: §eFyral", "§7Getestet von: §eFyral §7und §eSchneeemillll"));
                p.openInventory(inv);
            });
        });
    }

    public void openMyOrders(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§8▶ §bMeine Orders");
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
                inv.setItem(49, createButton(Material.ARROW, "§7Zurück", "§7Zum Hauptmenü"));
                p.openInventory(inv);
            });
        });
    }

    public void openManageMenu(Player p, int orderId) {
        OrderData data = db.getOrder(orderId);
        if (data == null) return;

        Inventory inv = Bukkit.createInventory(null, 27, "§8▶ §6Order verwalten #" + orderId);

        if (data.status.equals("ACTIVE")) {
            ItemStack cancelBtn = createButton(Material.RED_WOOL, "§c§lORDER ABBRECHEN");
            ItemMeta cm = cancelBtn.getItemMeta();
            cm.getPersistentDataContainer().set(keyOrderId, PersistentDataType.INTEGER, orderId);
            cancelBtn.setItemMeta(cm);
            inv.setItem(11, cancelBtn);
        }

        ItemStack chestBtn = createButton(Material.CHEST, "§a§lORDER-KISTE ÖFFNEN", "§7Items einzeln entnehmen.");
        ItemMeta chm = chestBtn.getItemMeta();
        chm.getPersistentDataContainer().set(keyOrderId, PersistentDataType.INTEGER, orderId);
        chestBtn.setItemMeta(chm);

        inv.setItem(15, chestBtn);
        inv.setItem(18, createButton(Material.ARROW, "§7Zurück"));
        p.openInventory(inv);
    }

    public void openOrderChest(Player p, int orderId, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, "§8▶ §6Kiste #" + orderId + " | S." + (page + 1));

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
        for (int i = start; i < end; i++) {
            inv.setItem(slot++, allItems.get(i));
        }

        if (page > 0) {
            ItemStack prev = createButton(Material.ARROW, "§7Vorherige Seite");
            ItemMeta m = prev.getItemMeta();
            m.getPersistentDataContainer().set(keyOrderId, PersistentDataType.INTEGER, orderId);
            m.getPersistentDataContainer().set(keyPage, PersistentDataType.INTEGER, page - 1);
            prev.setItemMeta(m);
            inv.setItem(45, prev);
        }
        if (end < allItems.size()) {
            ItemStack next = createButton(Material.ARROW, "§7Nächste Seite");
            ItemMeta m = next.getItemMeta();
            m.getPersistentDataContainer().set(keyOrderId, PersistentDataType.INTEGER, orderId);
            m.getPersistentDataContainer().set(keyPage, PersistentDataType.INTEGER, page + 1);
            next.setItemMeta(m);
            inv.setItem(53, next);
        }

        // HIER: Der Button heißt jetzt DROP ALL
        ItemStack takeAll = createButton(Material.HOPPER, "§a§lDROP ALL", "§7Nimmt alle Items dieser Seite.", "§7(Inventar voll? Fliegt auf den Boden!)");
        ItemMeta tm = takeAll.getItemMeta();
        tm.getPersistentDataContainer().set(keyOrderId, PersistentDataType.INTEGER, orderId);
        tm.getPersistentDataContainer().set(keyPage, PersistentDataType.INTEGER, page);
        takeAll.setItemMeta(tm);

        inv.setItem(48, takeAll);
        inv.setItem(49, createButton(Material.BARRIER, "§cZurück zum Menü"));

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

        if (title.contains("§8▶ §6Kiste #")) {
            e.setCancelled(true); // Verhindert reinlegen
            int orderId = Integer.parseInt(title.split("#")[1].split(" ")[0]);
            int page = Integer.parseInt(title.split("S\\.")[1]) - 1;

            if (clicked.getType() == Material.BARRIER) { openManageMenu(p, orderId); return; }

            if (clicked.getType() == Material.ARROW) {
                if (clicked.getItemMeta().getPersistentDataContainer().has(keyPage, PersistentDataType.INTEGER)) {
                    int newPage = clicked.getItemMeta().getPersistentDataContainer().get(keyPage, PersistentDataType.INTEGER);
                    openOrderChest(p, orderId, newPage);
                }
                return;
            }

            if (clicked.getType() == Material.HOPPER) {
                claimPage(p, orderId, e.getInventory());
                checkAndDeleteOrder(orderId);
                openOrderChest(p, orderId, page);
                return;
            }

            // HIER: Einzelnes Item rausnehmen mit Drop-Schutz
            if (e.getSlot() < 45) {
                ItemStack clickedItem = clicked.clone();
                HashMap<Integer, ItemStack> left = p.getInventory().addItem(clickedItem);

                if (!left.isEmpty()) {
                    // Inventar voll -> Ab auf den Boden!
                    for (ItemStack drop : left.values()) {
                        p.getWorld().dropItemNaturally(p.getLocation(), drop);
                    }
                    p.sendMessage("§cInventar voll! Item wurde gedroppt.");
                }

                removeRewardFromManager(orderId, clicked.getType(), clicked.getAmount());
                e.getInventory().setItem(e.getSlot(), null);
                checkAndDeleteOrder(orderId);
            }
            return;
        }

        if (title.contains("§6Items übergeben")) {
            if (e.getRawSlot() >= 18 && e.getRawSlot() <= 26) {
                e.setCancelled(true);
            }
            if (clicked.getType() == Material.LIME_STAINED_GLASS_PANE) {
                if (clicked.hasItemMeta() && clicked.getItemMeta().getPersistentDataContainer().has(keyOrderId, PersistentDataType.INTEGER)) {
                    int id = clicked.getItemMeta().getPersistentDataContainer().get(keyOrderId, PersistentDataType.INTEGER);
                    buy.confirmDelivery(p, e.getInventory(), id);
                }
            }
            return;
        }

        if (title.contains("§8▶") || title.contains("Suchergebnisse") || title.contains("Auswahl")) {
            e.setCancelled(true);

            if (title.contains("verwalten")) {
                if (clicked.getType() == Material.RED_WOOL) {
                    cancelOrder(p, clicked.getItemMeta().getPersistentDataContainer().get(keyOrderId, PersistentDataType.INTEGER));
                    return;
                } else if (clicked.getType() == Material.CHEST) {
                    openOrderChest(p, clicked.getItemMeta().getPersistentDataContainer().get(keyOrderId, PersistentDataType.INTEGER), 0);
                    return;
                }
            }

            if (clicked.getType() == Material.SUNFLOWER) { openMainMenu(p); return; }
            if (clicked.getItemMeta().getDisplayName().contains("Zurück")) { openMainMenu(p); return; }

            if (clicked.getType() == Material.ARROW && s != null) {
                if (e.getSlot() == 53) { s.currentPage++; SearchGUI.showResults(p, s, this, title.contains("Bezahl-Item")); }
                else if (e.getSlot() == 45 && s.currentPage > 0) { s.currentPage--; SearchGUI.showResults(p, s, this, title.contains("Bezahl-Item")); }
                return;
            }

            if (clicked.getItemMeta().getPersistentDataContainer().has(keyOrderId, PersistentDataType.INTEGER)) {
                int id = clicked.getItemMeta().getPersistentDataContainer().get(keyOrderId, PersistentDataType.INTEGER);
                OrderData data = db.getOrder(id);
                if (data != null) {
                    if (data.owner.equals(p.getUniqueId())) {
                        openManageMenu(p, id);
                    } else if (data.status.equals("ACTIVE")) {
                        buy.openDeliveryMenu(p, id);
                    }
                }
                return;
            }

            if (title.equals("§8▶ §6Alle Orders")) {
                // Wichtig: Erst auf null prüfen, dann auf Typ!
                if (clicked == null || clicked.getType() == Material.AIR) return;

                if (e.getSlot() == 49) {
                    activeSessions.put(p.getUniqueId(), new OrderSession());
                    SignGUI.open(p, "Welches Item?", plugin);
                }
                else if (e.getSlot() == 48) {
                    openMyOrders(p);
                }
                // Der Klickschutz für das Buch:
                else if (clicked.getType() == Material.WRITABLE_BOOK) {
                    // p.sendMessage("§7Dieses Plugin wurde mit Herz von Fyral erstellt!");
                    return;
                }
            }
            else if (title.contains("Suchergebnisse") || title.contains("Auswahl")) { handleSelection(p, s, clicked); }
            // HIER: Text auf "Bezahlungs Item" geändert
            else if (title.equals("§8▶ §aPreis-Modus wählen")) { s.isPerItem = (e.getSlot() == 11); s.step = 2; SignGUI.open(p, "Bezahlungs Item", plugin); }
            else if (title.equals("§8▶ §aBestätigung") && e.getSlot() == 11) completeOrderCreation(p, s);
        }
    }

    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent e) {
        if (e.getView().getTitle().contains("§6Items übergeben")) {
            Player p = (Player) e.getPlayer();
            Inventory inv = e.getInventory();
            for (int i = 0; i < 18; i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    p.getInventory().addItem(item);
                }
            }
        }
    }

    // --- NEU: Verhindert, dass echte Schilder das Plugin auslösen ---
    @EventHandler
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent e) {
        // Wenn der Spieler einen echten Block platziert, ist er definitiv
        // nicht mehr im Plugin-Menü -> Sitzung löschen!
        activeSessions.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent e) {
        // Räumt den Zwischenspeicher auf, wenn jemand offline geht
        activeSessions.remove(e.getPlayer().getUniqueId());
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

    // HIER: Die neue claimPage Logik mit Drop-Schutz
    private void claimPage(Player p, int orderId, Inventory inv) {
        for (int i = 0; i < 45; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                HashMap<Integer, ItemStack> left = p.getInventory().addItem(item);

                if (!left.isEmpty()) {
                    for (ItemStack drop : left.values()) {
                        p.getWorld().dropItemNaturally(p.getLocation(), drop);
                    }
                }

                removeRewardFromManager(orderId, item.getType(), item.getAmount());
                inv.setItem(i, null);
            }
        }
        p.sendMessage("§aSeite abgeholt (Überschuss wurde gedroppt)!");
    }

    private void checkAndDeleteOrder(int orderId) {
        OrderData data = db.getOrder(orderId);
        if (data != null && !data.status.equals("ACTIVE")) {
            List<DeliverManager.ItemStackData> rew = plugin.getDeliverManager().getRewards(orderId);
            if (rew.isEmpty()) {
                db.deleteOrder(orderId);
            }
        }
    }

    private void cancelOrder(Player p, int id) {
        OrderData data = db.getOrder(id);
        if (data != null) {
            String[] pParts = data.priceBase64.split(":");
            plugin.getDeliverManager().addReward(id, Material.valueOf(pParts[0]), Integer.parseInt(pParts[1]));
            db.setStatus(id, "CANCELLED");
            p.sendMessage("§cOrder abgebrochen! Hol deine Items in der Order-Kiste ab.");
            openManageMenu(p, id);
        }
    }

    private void completeOrderCreation(Player p, OrderSession s) {
        if (!hasEnoughItems(p, s.paymentMaterial, s.totalPaymentRequired)) {
            p.sendMessage("§cNicht genug Items!"); return;
        }
        removeItems(p, s.paymentMaterial, s.totalPaymentRequired);
        p.updateInventory();

        db.createOrder(p.getUniqueId(), s.wantedMaterial.name(), s.amount, s.paymentMaterial.name(), s.totalPaymentRequired, s.isPerItem);

        p.sendMessage("§aOrder erstellt!");
        p.closeInventory();
        openMainMenu(p);
    }

    private ItemStack createOrderDisplayItem(Player viewer, OrderData data) {
        try {
            String[] w = data.wantedBase64.split(":");
            String[] pr = data.priceBase64.split(":");

            Material wM = Material.valueOf(w[0]);
            int wAmt = Integer.parseInt(w[1]);
            boolean isPerItem = Boolean.parseBoolean(w[2]);

            Material pM = Material.valueOf(pr[0]);
            int pAmt = Integer.parseInt(pr[1]);

            ItemStack i = new ItemStack(wM);
            ItemMeta m = i.getItemMeta();
            m.setDisplayName("§6Order #" + data.id);

            String ownerName = Bukkit.getOfflinePlayer(data.owner).getName();
            if (ownerName == null) ownerName = "Unbekannt";

            List<String> lore = new ArrayList<>();
            lore.add("§7Erstellt von: §f" + ownerName);
            lore.add("§7Gesucht: §a" + wAmt + "x " + wM);

            if (isPerItem) {
                int perItem = pAmt / wAmt;
                lore.add("§7Preis pro Stück: §e" + perItem + "x " + pM);
                lore.add("§b§l⚡ TEIL-LIEFERUNG MÖGLICH");
                lore.add("§8(Gesamtpreis: " + pAmt + ")");
            } else {
                lore.add("§7Preis insgesamt: §e" + pAmt + "x " + pM);
            }

            lore.add("");
            if (data.status.equals("COMPLETED")) {
                lore.add("§a§lABGESCHLOSSEN - Bitte abholen!");
            } else if (data.status.equals("CANCELLED")) {
                lore.add("§c§lABGEBROCHEN - Bitte abholen!");
            } else {
                if (data.owner.equals(viewer.getUniqueId())) {
                    lore.add("§6§lDEINE ORDER");
                    lore.add("§e▶ Klicke zum Verwalten");
                } else {
                    lore.add("§a▶ Klicke zum Liefern");
                }
            }

            m.setLore(lore);
            m.getPersistentDataContainer().set(keyOrderId, PersistentDataType.INTEGER, data.id);
            i.setItemMeta(m);
            return i;
        } catch (Exception e) {
            return null;
        }
    }

    @EventHandler
    public void onSignDone(SignChangeEvent e) {
        Player p = e.getPlayer();
        OrderSession s = activeSessions.get(p.getUniqueId());
        if (s == null) return;

        String input = e.getLine(0).trim();

        // Wenn der Spieler ESC gedrückt hat oder das Schild leer lässt: Abbrechen!
        if (input.isEmpty()) {
            activeSessions.remove(p.getUniqueId());
            p.sendMessage("§cOrder-Eingabe abgebrochen.");
            return;
        }

        // Erst jetzt, wo wir sicher sind, dass es eine Plugin-Eingabe ist,
        // löschen wir das virtuelle Block-Schild.
        Bukkit.getScheduler().runTask(plugin, () -> e.getBlock().setType(Material.AIR));

        Bukkit.getScheduler().runTask(plugin, () -> {
            switch (s.step) {
                case 0: searchItems(p, s, input, false); break;
                case 1: try { s.amount = Math.max(1, Integer.parseInt(input)); openModeMenu(p); } catch (Exception ex) { p.sendMessage("§cZahl!"); } break;
                case 2: searchItems(p, s, input, true); break;
                case 3: try { int pr = Math.max(1, Integer.parseInt(input)); s.totalPaymentRequired = s.isPerItem ? (s.amount * pr) : pr; ConfirmGUI.open(p, s, this); } catch (Exception ex) { p.sendMessage("§cZahl!"); } break;
            }
        });
    }

    private void handleSelection(Player p, OrderSession s, ItemStack clicked) {
        String matStr = clicked.getItemMeta().getPersistentDataContainer().get(keyMaterial, PersistentDataType.STRING);
        if (matStr == null) return;
        Material mat = Material.valueOf(matStr);
        s.currentPage = 0;

        if (s.step == 0) {
            s.wantedMaterial = mat;
            s.step = 1;
            SignGUI.open(p, "Menge?", plugin);
        } else if (s.step == 2) {
            s.paymentMaterial = mat;
            s.step = 3;
            String schildText = s.isPerItem ? "Preis pro Item?" : "Gesamtpreis?";
            SignGUI.open(p, schildText, plugin);
        }
    }

    private void searchItems(Player p, OrderSession s, String input, boolean isPay) {
        s.currentResults.clear();
        String query = input.toUpperCase().replace(" ", "_");
        for (Material m : Material.values()) { if (m.isItem() && m.name().contains(query)) s.currentResults.add(m); }
        if (s.currentResults.isEmpty()) p.sendMessage("§cKein Item!");
        else SearchGUI.showResults(p, s, this, isPay);
    }

    public void openModeMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8▶ §aPreis-Modus wählen");
        inv.setItem(11, createButton(Material.GOLD_NUGGET, "§b§lPRO ITEM"));
        inv.setItem(15, createButton(Material.GOLD_INGOT, "§e§lINSGESAMT"));
        p.openInventory(inv);
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

    public ItemStack createButton(Material m, String name, String... lore) {
        ItemStack i = new ItemStack(m); ItemMeta mt = i.getItemMeta(); mt.setDisplayName(name);
        if (lore.length > 0) mt.setLore(Arrays.asList(lore));
        i.setItemMeta(mt); return i;
    }

    public NamespacedKey getKeyMaterial() { return keyMaterial; }
    public NamespacedKey getKeyOrderId() { return keyOrderId; }
}