package de.Fyral.ordersPlugin;

import de.Fyral.ordersPlugin.deliver.DeliverManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler; // WICHTIG
import org.bukkit.event.Listener;     // WICHTIG
import org.bukkit.event.player.PlayerJoinEvent; // WICHTIG
import org.bukkit.plugin.java.JavaPlugin;
import de.Fyral.ordersPlugin.ah.AhManager;
import de.Fyral.ordersPlugin.ah.AhGUI;
import de.Fyral.ordersPlugin.ah.AhCommand;

// Wir fügen "implements Listener" hinzu, damit das Plugin auf Events hören kann
public class OrdersPlugin extends JavaPlugin implements Listener {

    private DataManager dataManager;
    private DeliverManager deliverManager;
    private GUI gui;
    private Buy buy;

    @Override
    public void onEnable() {
        dataManager = new DataManager(this);
        deliverManager = new DeliverManager(this);
        buy = new Buy(this, dataManager);
        gui = new GUI(this, dataManager, buy);

        // --- DEIN NEUER AH CODE START ---
        AhManager ahManager = new AhManager(this);
        AhGUI ahGui = new AhGUI(this, ahManager);
        getServer().getPluginManager().registerEvents(ahGui, this);
        if (getCommand("ah") != null) {
            getCommand("ah").setExecutor(new AhCommand(ahGui));
        }
        // --- DEIN NEUER AH CODE ENDE ---

        if (getCommand("orders") != null) {
            getCommand("orders").setExecutor(new Commands(gui));
        }
        if (getCommand("order") != null) {
            getCommand("order").setExecutor(new Commands(gui));
        }

        // Wir registrieren die GUI als Listener
        getServer().getPluginManager().registerEvents(gui, this);

        // WICHTIG: Wir registrieren AUCH diese Klasse (this) als Listener für das Join-Event
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("OrdersPlugin erfolgreich gestartet!");
    }

    // Das Join-Event für die Benachrichtigungen
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (deliverManager.hasUnnotified(p.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                p.sendMessage("");
                p.sendMessage("§6§l[Handel] §aIn deiner Abholstation liegen neue Items!");
                p.sendMessage("§7Nutze /orders -> Abholstation.");
                p.sendMessage("");
                deliverManager.setNotified(p.getUniqueId());
            }, 40L); // 2 Sekunden Verzögerung nach dem Login
        }
    }

    @Override
    public void onDisable() {
        if (dataManager != null) dataManager.save();
    }

    public DataManager getDataManager() { return dataManager; }
    public DeliverManager getDeliverManager() { return deliverManager; }
    public GUI getGui() { return gui; }
}