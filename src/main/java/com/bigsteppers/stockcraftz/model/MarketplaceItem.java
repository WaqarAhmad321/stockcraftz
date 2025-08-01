package com.bigsteppers.stockcraftz.model;

public record MarketplaceItem(
        int id,
        String itemName,
        String itemType,
        double price,
        int sellerId
) {
}