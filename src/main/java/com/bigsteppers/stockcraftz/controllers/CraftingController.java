package com.bigsteppers.stockcraftz.controllers;

import com.bigsteppers.stockcraftz.interfaces.LoadablePage;
import com.bigsteppers.stockcraftz.model.SessionManager;
import com.bigsteppers.stockcraftz.utils.FXUtils;
import com.bigsteppers.stockcraftz.utils.Formater;
import com.dbfx.database.DBUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.*;

import static com.bigsteppers.stockcraftz.StockCraftzApplication.RESOURCES_PATH;

record RawMaterial(String materialType, int quantity, String imageUrl) {
}

record CraftingRecipe(int id, String craftedItemName, Map<String, Integer> requiredMaterials) {
}

public class CraftingController implements LoadablePage {

    @FXML
    private Node craftingPage;

    @FXML
    private GridPane craftingGrid;

    @FXML
    private VBox slot1, slot2, slot3;

    @FXML
    private Pane resultSlot;

    @FXML
    private ImageView resultIcon;

    @FXML
    private GridPane inventoryGrid;

    @FXML
    private Button clearButton;

    @FXML
    private Button craftButton;

    @FXML
    private NavbarController navbarController;

    private List<RawMaterial> inventory = new ArrayList<>();
    private Map<VBox, String> craftingSlots = new HashMap<>();
    private List<CraftingRecipe> recipes = new ArrayList<>();

    @FXML
    private void initialize() {
        setupNavbar();
    }

    @Override
    public void onLoad() {
        if (!SessionManager.isLoggedIn()) {
            System.err.println("CraftingController: No user logged in, redirecting to login");
            return;
        }
        try {
            int userId = SessionManager.getCurrentUser().id();
            System.out.println("CraftingController: User logged in, userId: " + userId);
            fetchRecipes();
            fetchInventory(userId);
            setupClickHandlers();
        } catch (Exception e) {
            System.err.println("CraftingController: Error accessing user ID: " + e.getMessage());
            e.printStackTrace();
            FXUtils.navigateTo("login", craftingPage);
        }
    }

    private void setupNavbar() {
        if (navbarController != null) {
            System.out.println("CraftingController: Setting navbar active page");
            navbarController.setActivePage("crafting");
            navbarController.rootNode = craftingPage;
        }
    }

    private void fetchRecipes() {
        System.out.println("CraftingController: Fetching recipes");
        String sql = "SELECT id, recipe_name, required_materials FROM crafting_recipes";
        DBUtils.executeQueryAsync(sql, stmt -> {
        }, rs -> {
            List<CraftingRecipe> fetchedRecipes = new ArrayList<>();
            try {
                while (rs.next()) {
                    System.out.println(rs.getString("required_materials"));
                    int id = rs.getInt("id");
                    String craftedItemName = rs.getString("recipe_name");
//                    String materialsStr = rs.getString("required_materials");
//                    System.out.println("martial str is!!! :" + materialsStr);
                    Map<String, Integer> requiredMaterials = new HashMap<>();
                    JSONObject json = new JSONObject(rs.getString("required_materials"));

                    for (String key : json.keySet()) {
                        requiredMaterials.put(key.toLowerCase(), json.getInt(key));
                    }

                    fetchedRecipes.add(new CraftingRecipe(id, craftedItemName, requiredMaterials));
                }
                System.out.println("CraftingController: Fetched " + fetchedRecipes.size() + " recipes");
            } catch (SQLException e) {
                throw new RuntimeException("Error reading recipes", e);
            }
            return fetchedRecipes;
        }, fetchedRecipes -> {
            recipes = fetchedRecipes;
            System.out.println("CraftingController: Recipes loaded");
        }, error -> System.err.println("CraftingController: Error fetching recipes: " + error.getMessage()));
    }

    private void fetchInventory(int userId) {
        System.out.println("CraftingController: Fetching inventory for userId: " + userId);
        String sql = "SELECT material_type, quantity, image_url FROM raw_material_inventory WHERE user_id = ?";
        DBUtils.executeQueryAsync(sql, stmt -> {
            try {
                stmt.setInt(1, userId);
            } catch (SQLException e) {
                throw new RuntimeException("Error setting userId", e);
            }
        }, rs -> {
            List<RawMaterial> fetchedInventory = new ArrayList<>();
            try {
                while (rs.next()) {
                    fetchedInventory.add(new RawMaterial(
                            rs.getString("material_type"),
                            rs.getInt("quantity"),
                            rs.getString("image_url")
                    ));
                }
                System.out.println("CraftingController: Fetched " + fetchedInventory.size() + " raw materials");
            } catch (SQLException e) {
                throw new RuntimeException("Error reading inventory", e);
            }
            return fetchedInventory;
        }, fetchedInventory -> {
            inventory = fetchedInventory;
            updateInventoryGrid();
        }, error -> System.err.println("CraftingController: Error fetching inventory: " + error.getMessage()));
    }

    private void updateInventoryGrid() {
        System.out.println("CraftingController: Updating inventory grid");
        inventoryGrid.getChildren().clear();
        int col = 0, row = 0;
        for (RawMaterial material : inventory) {
            if (material.quantity() <= 0) continue;
            VBox slot = new VBox();
            slot.getStyleClass().add("inventory-slot");
            slot.getStyleClass().add("filled");

            ImageView icon = new ImageView();
            icon.getStyleClass().add("item-icon");
            icon.setFitWidth(40);
            icon.setFitHeight(40);
            java.io.InputStream is = getClass().getResourceAsStream(RESOURCES_PATH + "/raw_materials/" + Formater.toSnakeCase(material.materialType()) + ".png");
            if (is != null) {
                icon.setImage(new Image(is));
                System.out.println("RawMaterialsController: Loaded placeholder image for material: ");
            } else {
                System.err.println("RawMaterialsController: Failed to load placeholder.png for material: ");
            }

            Label count = new Label(String.valueOf(material.quantity()));
            count.getStyleClass().add("item-count");

            slot.getChildren().addAll(icon, count);
            slot.setUserData(material.materialType());
            inventoryGrid.add(slot, col, row);
            col++;
            if (col > 7) {
                col = 0;
                row++;
            }
        }
        System.out.println("CraftingController: Inventory grid updated with " + inventory.size() + " items");
        setupInventoryClickHandlers();
    }

    private void setupClickHandlers() {
        System.out.println("CraftingController: Setting up click handlers");
        // Crafting slots (removal on click)
        for (VBox slot : Arrays.asList(slot1, slot2, slot3)) {
            slot.setOnMouseClicked(event -> {
                if (!slot.getChildren().isEmpty()) {
                    slot.getChildren().clear();
                    slot.getStyleClass().remove("filled");
                    craftingSlots.remove(slot);
                    updateCraftButton();
                    resultIcon.setImage(null);
                    System.out.println("CraftingController: Removed material from slot");
                }
                event.consume();
            });
        }
    }

    private void setupInventoryClickHandlers() {
        // Inventory slots (add to crafting grid on click)
        inventoryGrid.getChildren().filtered(node -> node instanceof VBox).forEach(slot -> {
            slot.setOnMouseClicked(event -> {
                String materialType = (String) slot.getUserData();
                if (materialType != null) {
                    RawMaterial material = inventory.stream()
                            .filter(m -> m.materialType().equals(materialType) && m.quantity() > 0)
                            .findFirst()
                            .orElse(null);
                    if (material != null) {
                        // Find first empty slot
                        VBox targetSlot = Arrays.asList(slot1, slot2, slot3).stream()
                                .filter(s -> s.getChildren().isEmpty())
                                .findFirst()
                                .orElse(null);
                        if (targetSlot != null) {
                            ImageView icon = new ImageView();
                            icon.getStyleClass().add("item-icon");
                            icon.setFitWidth(40);
                            icon.setFitHeight(40);
                            java.io.InputStream is = getClass().getResourceAsStream(RESOURCES_PATH + "/raw_materials/" + Formater.toSnakeCase(material.materialType()) + ".png");
                            if (is != null) {
                                icon.setImage(new Image(is));
                                System.out.println("RawMaterialsController: Loaded placeholder image for material: ");
                            } else {
                                System.err.println("RawMaterialsController: Failed to load placeholder.png for material: ");
                            }
                            targetSlot.getChildren().add(icon);
                            targetSlot.getStyleClass().add("filled");
                            craftingSlots.put(targetSlot, materialType);
                            updateCraftButton();
                            System.out.println("CraftingController: Added " + materialType + " to crafting slot");
                        } else {
                            System.out.println("CraftingController: No empty crafting slots available");
                        }
                    }
                }
                event.consume();
            });
        });
    }

    private void updateCraftButton() {
        boolean hasMaterials = !craftingSlots.isEmpty();
        craftButton.setDisable(!hasMaterials);
        craftButton.getStyleClass().remove("disabled");
        if (!hasMaterials) {
            craftButton.getStyleClass().add("disabled");
        }
        System.out.println("CraftingController: Craft button updated, enabled: " + hasMaterials);
    }

    @FXML
    private void clearCraftingGrid() {
        System.out.println("CraftingController: Clearing crafting grid");
        for (VBox slot : Arrays.asList(slot1, slot2, slot3)) {
            slot.getChildren().clear();
            slot.getStyleClass().remove("filled");
        }
        craftingSlots.clear();
        resultIcon.setImage(null);
        updateCraftButton();
    }

    @FXML
    private void craftItem() {
        System.out.println("CraftingController: Attempting to craft item");
        int userId = SessionManager.getCurrentUser().id();
        Map<String, Integer> placedMaterials = new HashMap<>();
        craftingSlots.values().forEach(material ->
                placedMaterials.merge(material.toLowerCase(), 1, Integer::sum));
        System.out.println("placed materials " + placedMaterials);
        CraftingRecipe matchingRecipe = findMatchingRecipe(placedMaterials);
        if (matchingRecipe == null) {
            showError("Invalid recipe! Check the crafting guide.");
            return;
        }

        // Check inventory
        Map<String, Integer> available = new HashMap<>();

        for (RawMaterial material : inventory) {
            available.put(material.materialType().toLowerCase(), material.quantity());
        }

        for (Map.Entry<String, Integer> req : matchingRecipe.requiredMaterials().entrySet()) {
            int needed = req.getValue();
            int owned = available.getOrDefault(req.getKey(), 0);
            System.out.println(available);
            if (owned < needed) {
                showError("Not enough " + req.getKey() + " to craft " + matchingRecipe.craftedItemName() + " " + req.getValue() + " times." + " " + (owned) + " more needed.");
                return;
            }
        }

        // Deduct materials
        for (Map.Entry<String, Integer> req : matchingRecipe.requiredMaterials().entrySet()) {
            String materialType = req.getKey();
            int needed = req.getValue();

            String updateSql = "UPDATE raw_material_inventory SET quantity = quantity - ? " +
                    "WHERE user_id = ? AND material_type = ? AND quantity > ?";

            DBUtils.executeUpdateAsync(updateSql, stmt -> {
                try {
                    stmt.setInt(1, needed);
                    stmt.setInt(2, userId);
                    stmt.setString(3, materialType.toUpperCase());
                    stmt.setInt(4, needed);
                } catch (SQLException e) {
                    throw new RuntimeException("Error preparing update statement", e);
                }
            }, success -> {
                if (!success) {
                    // If update fails, try to delete where quantity == needed
                    String deleteSql = "DELETE FROM raw_material_inventory " +
                            "WHERE user_id = ? AND material_type = ? AND quantity = ?";

                    DBUtils.executeUpdateAsync(deleteSql, deleteStmt -> {
                        try {
                            deleteStmt.setInt(1, userId);
                            deleteStmt.setString(2, materialType.toUpperCase());
                            deleteStmt.setInt(3, needed);
                        } catch (SQLException e) {
                            throw new RuntimeException("Error preparing delete statement", e);
                        }
                    }, deleted -> {
                        if (!deleted) {
                            System.err.println("CraftingController: Failed to remove material " + materialType);
                        }
                    }, error -> {
                        System.err.println("CraftingController: Error deleting material: " + error.getMessage());
                    });
                }
            }, error -> {
                System.err.println("CraftingController: Error updating material: " + error.getMessage());
            });
        }

        // Add crafted item
        String craftedItemName = matchingRecipe.craftedItemName();
        String insertSql = "INSERT INTO crafted_item_inventory (user_id, crafted_item_name) VALUES (?, ?)";
        DBUtils.executeUpdateAsync(insertSql, stmt -> {
            try {
                stmt.setInt(1, userId);
                stmt.setString(2, craftedItemName);
            } catch (SQLException e) {
                throw new RuntimeException("Error inserting crafted item", e);
            }
        }, success -> {
            if (success) {
                System.out.println("CraftingController: Crafted " + craftedItemName);
                // Update result slot
                java.io.InputStream is = getClass().getResourceAsStream(RESOURCES_PATH + "/crafted_items/" + Formater.toSnakeCase(craftedItemName) + ".png");
                if (is != null) {
                    resultIcon.setImage(new Image(is));
                    System.out.println("RawMaterialsController: Loaded placeholder image for material: ");
                } else {
                    System.err.println("RawMaterialsController: Failed to load placeholder.png for material: ");
                }
                // Log activity
                logCraftingActivity(userId, craftedItemName);
                // Refresh inventory
                fetchInventory(userId);
                clearCraftingGrid();
            } else {
                showError("Failed to craft item.");
            }
        }, error -> System.err.println("CraftingController: Error crafting item: " + error.getMessage()));
    }

    private CraftingRecipe findMatchingRecipe(Map<String, Integer> placedMaterials) {
        for (CraftingRecipe recipe : recipes) {
            Map<String, Integer> required = recipe.requiredMaterials();
            if (required.size() == placedMaterials.size() &&
                    required.entrySet().stream().allMatch(e ->
                            placedMaterials.getOrDefault(e.getKey(), 0).equals(e.getValue()))) {
                return recipe;
            }
        }
        return null;
    }

    private void logCraftingActivity(int userId, String craftedItemName) {
        String sql = "INSERT INTO user_activities (user_id, action_type, item_name, amount, timestamp) " +
                "VALUES (?, 'CRAFT', ?, 1, NOW())";
        DBUtils.executeUpdateAsync(sql, stmt -> {
            try {
                stmt.setInt(1, userId);
                stmt.setString(2, craftedItemName);
            } catch (SQLException e) {
                throw new RuntimeException("Error logging activity", e);
            }
        }, success -> {
            if (success) {
                System.out.println("CraftingController: Logged activity for crafting " + craftedItemName);
            }
        }, error -> System.err.println("CraftingController: Error logging activity: " + error.getMessage()));
    }

    private void showError(String message) {
        System.out.println("CraftingController: Error: " + message);
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText("Crafting Error");
        alert.showAndWait();
    }

    @FXML
    private void navigateToRawMaterials() {
        System.out.println("CraftingController: Navigating to raw materials page");
        FXUtils.navigateTo("rawMaterials", craftingPage);
    }

    @FXML
    private void navigateToMarketplace() {
        System.out.println("CraftingController: Navigating to marketplace page");
        FXUtils.navigateTo("marketplace", craftingPage);
    }
}