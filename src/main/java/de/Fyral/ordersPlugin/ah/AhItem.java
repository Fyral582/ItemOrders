package de.Fyral.ordersPlugin.ah;

import org.bukkit.Material;
import java.util.UUID;

public class AhItem {
    public int id;
    public UUID seller;
    public String itemBase64;
    public Material currency;
    public int price;
    public long timestamp;

    public AhItem(int id, UUID seller, String itemBase64, Material currency, int price, long timestamp) {
        this.id = id;
        this.seller = seller;
        this.itemBase64 = itemBase64;
        this.currency = currency;
        this.price = price;
        this.timestamp = timestamp;
    }
}