package de.Fyral.ordersPlugin.createorder;

import de.Fyral.ordersPlugin.GUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ConfirmGUI {

    public static void open(Player p, OrderSession s, GUI mainGUI) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8▶ §aBestätigung");

        // Hintergrund füllen
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, mainGUI.createButton(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        // Info-Buch in der Mitte
        inv.setItem(13, mainGUI.createButton(Material.WRITTEN_BOOK, "§d§lDEINE ORDER",
                "§7Item: §f" + s.amount + "x " + s.wantedMaterial,
                "§7Preis: §f" + s.totalPaymentRequired + "x " + s.paymentMaterial,
                "§7Modus: §f" + (s.isPerItem ? "Pro Item" : "Gesamtpreis")));

        // Bestätigen & Abbrechen
        inv.setItem(11, mainGUI.createButton(Material.LIME_WOOL, "§a§lBESTELLEN", "§7Geld wird sofort abgezogen!"));
        inv.setItem(15, mainGUI.createButton(Material.RED_WOOL, "§c§lABBRECHEN", "§7Vorgang beenden."));

        p.openInventory(inv);
    }
}