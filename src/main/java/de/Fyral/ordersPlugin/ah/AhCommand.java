package de.Fyral.ordersPlugin.ah;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import de.Fyral.ordersPlugin.Lang;

public class AhCommand implements CommandExecutor {

    private final AhGUI ahGui;

    public AhCommand(AhGUI ahGui) {
        this.ahGui = ahGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        // Wenn er nur "/ah" eingibt (ohne Argumente) -> Standard Menü
        if (args.length == 0) {
            ahGui.openMainMenu(p, 0, ""); // Leerer String bedeutet: Keine Suche
            return true;
        }

        // NEU: Die Suchfunktion! Z.B. "/ah diamond"
        if (args.length == 1 && !args[0].equalsIgnoreCase("sell")) {
            String searchQuery = args[0].toUpperCase();
            ahGui.openMainMenu(p, 0, searchQuery); // Öffnet das Menü gefiltert
            return true;
        }

        if (args[0].equalsIgnoreCase("sell")) {
            if (args.length != 3) {
                p.sendMessage(Lang.getPrefixed("msg.ah_usage"));
                return true;
            }

            Material currency = Material.matchMaterial(args[1].toUpperCase());
            if (currency == null || !currency.isItem()) {
                p.sendMessage(Lang.getPrefixed("msg.invalid_currency"));
                return true;
            }

            int amount;
            try { amount = Integer.parseInt(args[2]); } catch (Exception e) {
                p.sendMessage(Lang.getPrefixed("msg.enter_number"));
                return true;
            }

            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) {
                p.sendMessage(Lang.getPrefixed("msg.hold_item"));
                return true;
            }

            ItemStack toSell = hand.clone();
            p.getInventory().setItemInMainHand(null);

            ahGui.openConfirmMenu(p, toSell, currency, amount);
            return true;
        }

        return true;
    }
}