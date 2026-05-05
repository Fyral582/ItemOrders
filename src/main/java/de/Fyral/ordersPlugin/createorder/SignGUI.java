package de.Fyral.ordersPlugin.createorder;

import de.Fyral.ordersPlugin.OrdersPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;

public class SignGUI {

    public static void open(Player p, String anleitung, OrdersPlugin plugin) {
        Block target = findAirBlock(p);
        target.setType(Material.OAK_SIGN);

        Sign sign = (Sign) target.getState();
        sign.getSide(Side.FRONT).setLine(0, "");
        sign.getSide(Side.FRONT).setLine(1, "§7^^^^^^^^^^^^^^^");
        sign.getSide(Side.FRONT).setLine(2, "§6" + anleitung);
        sign.getSide(Side.FRONT).setLine(3, "§8(Oben tippen)");
        sign.update();

        Bukkit.getScheduler().runTaskLater(plugin, () -> p.openSign(sign), 2L);
    }

    private static Block findAirBlock(Player p) {
        Location loc = p.getLocation();
        for (int y = 0; y <= 3; y++) {
            Block b = loc.clone().add(0, y, 0).getBlock();
            if (b.getType() == Material.AIR) return b;
        }
        return loc.getBlock(); // Notfall
    }
}