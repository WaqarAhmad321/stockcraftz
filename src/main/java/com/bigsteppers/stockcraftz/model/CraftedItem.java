package com.bigsteppers.stockcraftz.model;

public class CraftedItem extends InventoryItem {
    private String craftedItemName;

    public CraftedItem(int id, String craftedItemName, int quantity) {
        super(id, quantity);
        setCraftedItemName(craftedItemName);
    }

    public String getCraftedItemName() {
        return craftedItemName;
    }

    public void setCraftedItemName(String craftedItemName) {
        this.craftedItemName = craftedItemName;
    }
}
