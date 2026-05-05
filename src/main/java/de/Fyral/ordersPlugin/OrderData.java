package de.Fyral.ordersPlugin;

import java.util.UUID;

public class OrderData {
    public int id;
    public UUID owner;
    public String wantedBase64;
    public String priceBase64;
    public String status;

    public OrderData(int id, UUID owner, String wantedBase64, String priceBase64, String status) {
        this.id = id;
        this.owner = owner;
        this.wantedBase64 = wantedBase64;
        this.priceBase64 = priceBase64;
        this.status = status;
    }
}