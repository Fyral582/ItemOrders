package de.Fyral.ordersPlugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class Buy {

    private final OrdersPlugin plugin;
    private final DataManager dataManager;

    public Buy(OrdersPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    public void openDeliveryMenu(Player p, int orderId) {
        OrderData data = dataManager.getOrder(orderId);
        if (data == null) return;

        String[] wParts = data.wantedBase64.split(":");
        Inventory inv = Bukkit.createInventory(null, 27, "§6Items übergeben #" + orderId);

        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta m = confirm.getItemMeta();
        m.setDisplayName("§a§lBESTÄTIGEN");
        m.getPersistentDataContainer().set(plugin.getGui().getKeyOrderId(), PersistentDataType.INTEGER, orderId);
        confirm.setItemMeta(m);

        inv.setItem(22, confirm);
        p.openInventory(inv);
        p.sendMessage("§7Lege §6" + wParts[1] + "x " + wParts[0] + " §7in die oberen Slots.");
    }

    public void confirmDelivery(Player p, Inventory inv, int orderId) {
        OrderData data = dataManager.getOrder(orderId);
        if (data == null) return;

        String[] wParts = data.wantedBase64.split(":");
        Material wMat = Material.valueOf(wParts[0]);
        int totalWanted = Integer.parseInt(wParts[1]);
        boolean isPerItem = Boolean.parseBoolean(wParts[2]);

        String[] pParts = data.priceBase64.split(":");
        Material pMat = Material.valueOf(pParts[0]);
        int currentTotalPrice = Integer.parseInt(pParts[1]);

        int deliveredCount = 0;
        for (int i = 0; i < 18; i++) {
            ItemStack is = inv.getItem(i);
            if (is != null && is.getType() == wMat) deliveredCount += is.getAmount();
        }

        if (deliveredCount <= 0) {
            p.sendMessage("§cDu hast keine passenden Items reingelegt!");
            return;
        }

        if (isPerItem) {
            int amountToTake = Math.min(deliveredCount, totalWanted);
            int pricePerUnit = currentTotalPrice / totalWanted;
            int rewardAmount = amountToTake * pricePerUnit;

            p.getInventory().addItem(new ItemStack(pMat, rewardAmount));
            plugin.getDeliverManager().addReward(orderId, wMat, amountToTake);
            plugin.getDeliverManager().addNotification(data.owner);

            inv.clear();
            if (deliveredCount > amountToTake) {
                p.getInventory().addItem(new ItemStack(wMat, deliveredCount - amountToTake));
            }

            if (amountToTake == totalWanted) {
                dataManager.setStatus(orderId, "COMPLETED");
                p.sendMessage("§aOrder komplett abgeschlossen!");
            } else {
                int remainingAmount = totalWanted - amountToTake;
                int remainingPrice = currentTotalPrice - rewardAmount;
                dataManager.updateOrder(orderId, remainingAmount, remainingPrice);
                p.sendMessage("§aTeil-Lieferung erfolgt! Du hast §e" + rewardAmount + "x " + pMat.name() + " §abekommt.");
                p.sendMessage("§7Restliche Menge der Order: " + remainingAmount);
            }
            p.closeInventory();
            notifyOwner(orderId, data.owner);

        } else {
            if (deliveredCount >= totalWanted) {
                p.getInventory().addItem(new ItemStack(pMat, currentTotalPrice));
                plugin.getDeliverManager().addReward(orderId, wMat, totalWanted);
                plugin.getDeliverManager().addNotification(data.owner);

                inv.clear();
                if (deliveredCount > totalWanted) {
                    p.getInventory().addItem(new ItemStack(wMat, deliveredCount - totalWanted));
                }

                dataManager.setStatus(orderId, "COMPLETED");
                p.closeInventory();
                p.sendMessage("§aHandel erfolgreich!");
                notifyOwner(orderId, data.owner);
            } else {
                p.sendMessage("§cDiese Order erfordert die komplette Menge (" + totalWanted + ") auf einmal!");
            }
        }
    }

    private void notifyOwner(int orderId, java.util.UUID ownerUUID) {
        Player owner = Bukkit.getPlayer(ownerUUID);
        if (owner != null) {
            owner.sendMessage("§6§l[Handel] §aEs gab eine Lieferung für deine Order #" + orderId + "!");
        }
    }

    public void processFulfillment(Player p, int orderId) {
        openDeliveryMenu(p, orderId);
    }
}