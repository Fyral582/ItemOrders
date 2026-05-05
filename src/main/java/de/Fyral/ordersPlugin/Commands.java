package de.Fyral.ordersPlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Commands implements CommandExecutor {

    private final GUI gui;

    public Commands(GUI gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Nur Spieler koennen diesen Befehl nutzen!");
            return true;
        }

        Player p = (Player) sender;
        gui.openMainMenu(p);
        return true;
    }
}