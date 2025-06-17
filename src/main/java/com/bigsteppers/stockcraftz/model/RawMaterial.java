package com.bigsteppers.stockcraftz.model;

public class RawMaterial extends InventoryItem {
    private MaterialType materialType;

    public RawMaterial(int id, int quantity, MaterialType materialType) {
        super(id, quantity);
        this.materialType = materialType;
    }

    // Getters & Setters
    public MaterialType getMaterialType() {
        return materialType;
    }

    public void setMaterialType(MaterialType materialType) {
        this.materialType = materialType;
    }
}
