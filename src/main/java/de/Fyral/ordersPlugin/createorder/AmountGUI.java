package de.Fyral.ordersPlugin.createorder;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AmountGUI {

    // Öffnet den Amboss für die Zahleneingabe
    public static void open(Player p, String title) {
        Inventory inv = Bukkit.createInventory(p, InventoryType.ANVIL, title);
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();

        // Auch hier nutzen wir "§r" für ein sauberes Eingabefeld
        meta.setDisplayName("§r");
        paper.setItemMeta(meta);

        inv.setItem(0, paper);
        p.openInventory(inv);
    }
}