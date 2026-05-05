package de.Fyral.ordersPlugin.createorder;

import org.bukkit.Material;
import java.util.ArrayList;
import java.util.List;

public class OrderSession {
    public Material wantedMaterial = null;
    public int amount = 1;
    public boolean isPerItem = false;
    public Material paymentMaterial = null;
    public int totalPaymentRequired = 1;
    public List<Material> currentResults = new ArrayList<>();
    public int currentPage = 0;

    // 0 = Sucht gesuchtes Item, 1 = Gibt Menge ein,
    // 2 = Sucht Bezahl-Item, 3 = Gibt Preis ein
    public int step = 0;
}