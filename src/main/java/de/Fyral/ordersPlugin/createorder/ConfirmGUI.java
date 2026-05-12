package de.Fyral.ordersPlugin.createorder;

import de.Fyral.ordersPlugin.GUI;
import de.Fyral.ordersPlugin.Lang; // <-- HIER IST DIE WICHTIGE IMPORT-ZEILE!
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ConfirmGUI {

    public static void open(Player p, OrderSession s, GUI mainGUI) {
        // Zieht sich den Titel aus der Lang
        Inventory inv = Bukkit.createInventory(null, 27, Lang.get("menu.confirm_order"));

        // Hintergrund füllen
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, mainGUI.createButton(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        // Welcher Modus wurde gewählt? (Holt die Namen direkt aus den Sprachdateien)
        String mode = s.isPerItem ? Lang.get("btn.price_per_item") : Lang.get("btn.price_total");

        // Info-Buch in der Mitte (komplett dynamisch)
        inv.setItem(13, mainGUI.createButton(Material.WRITTEN_BOOK, Lang.get("order.yours"),
                Lang.get("menu.confirm_item").replace("%amount%", String.valueOf(s.amount)).replace("%item%", s.wantedMaterial.name()),
                Lang.get("menu.confirm_price").replace("%amount%", String.valueOf(s.totalPaymentRequired)).replace("%item%", s.paymentMaterial.name()),
                Lang.get("menu.confirm_mode").replace("%mode%", mode)));

        // Bestätigen & Abbrechen Buttons
        inv.setItem(11, mainGUI.createButton(Material.LIME_WOOL, Lang.get("btn.order_now"), Lang.get("btn.order_now_lore")));
        inv.setItem(15, mainGUI.createButton(Material.RED_WOOL, Lang.get("btn.cancel"), Lang.get("btn.cancel_lore")));

        p.openInventory(inv);
    }
}