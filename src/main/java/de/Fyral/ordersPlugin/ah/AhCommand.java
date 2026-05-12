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

        if (args.length == 0) {
            // Öffnet das Menü auf Seite 0 (Erste Seite)
            ahGui.openMainMenu(p, 0);
            return true;
        }

        if (args[0].equalsIgnoreCase("sell")) {
            if (args.length != 3) {
                p.sendMessage(Lang.getPrefixed("msg.ah_usage"));
                return true;
            }

            Material currency = Material.matchMaterial(args[1].toUpperCase());
            if (currency == null || !currency.isItem()) {
                p.sendMessage("§cUngültiges Bezahl-Item!");
                return true;
            }

            int amount;
            try { amount = Integer.parseInt(args[2]); } catch (Exception e) {
                p.sendMessage("§cBitte eine gültige Zahl angeben!");
                return true;
            }

            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) {
                p.sendMessage("§cDu musst ein Item in der Hand halten!");
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