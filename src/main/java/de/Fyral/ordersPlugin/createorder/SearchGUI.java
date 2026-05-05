package de.Fyral.ordersPlugin.createorder;

import de.Fyral.ordersPlugin.GUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

public class SearchGUI {

    public static void showResults(Player p, OrderSession s, GUI mainGUI, boolean isPay) {
        String title = isPay ? "§eBezahl-Item Auswahl" : "§aSuchergebnisse";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Berechnung der Seite
        int start = s.currentPage * 45;
        int end = Math.min(start + 45, s.currentResults.size());

        int slot = 0;
        for (int i = start; i < end; i++) {
            Material mat = s.currentResults.get(i);
            ItemStack item = new ItemStack(mat);
            ItemMeta m = item.getItemMeta();

            // Material im PersistentDataContainer speichern
            m.getPersistentDataContainer().set(mainGUI.getKeyMaterial(), PersistentDataType.STRING, mat.name());
            m.setLore(Arrays.asList("§7", "§a▶ Klicke zum Auswählen"));
            item.setItemMeta(m);

            inv.setItem(slot++, item);
        }

        // --- NAVIGATIONSPFEILE ---
        if (s.currentPage > 0) {
            inv.setItem(45, mainGUI.createButton(Material.ARROW, "§e§l◀ Vorherige Seite", "§7Seite " + s.currentPage));
        }

        if (end < s.currentResults.size()) {
            inv.setItem(53, mainGUI.createButton(Material.ARROW, "§e§lNächste Seite ▶", "§7Seite " + (s.currentPage + 2)));
        }

        // Zurück/Abbrechen Button
        inv.setItem(49, mainGUI.createButton(Material.BARRIER, "§c§lAbbrechen"));

        p.openInventory(inv);
    }
}