package com.bigsteppers.stockcraftz.controllers;

import com.bigsteppers.stockcraftz.interfaces.LoadablePage;
import com.bigsteppers.stockcraftz.model.SessionManager;
import com.bigsteppers.stockcraftz.utils.FXUtils;
import com.bigsteppers.stockcraftz.utils.Formater;
import com.dbfx.database.DBUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

import static com.bigsteppers.stockcraftz.StockCraftzApplication.RESOURCES_PATH;

public class CraftingController implements LoadablePage {

    private static final String CRAFT_ACTION = "CRAFT";
    private static final int INVENTORY_COLUMNS = 8;
    private static final int ITEM_ICON_SIZE = 40;

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
    private Map<VBox, Map<String, Integer>> craftingSlots = new HashMap<>();
    private List<CraftingRecipe> recipes = new ArrayList<>();

    @FXML
    private void initialize() {
        setupNavbar();
        setupClickHandlers();
        updateCraftingUI();
    }

    @Override
    public void onLoad() {
        if (!SessionManager.isLoggedIn()) {
            FXUtils.navigateTo("login", craftingPage);
            return;
        }
        fetchRecipes();
        fetchInventory(SessionManager.getCurrentUser().id());
    }

    private void setupNavbar() {
        if (navbarController != null) {
            navbarController.setActivePage("crafting");
            navbarController.rootNode = craftingPage;
        }
    }

    private void fetchRecipes() {
        String sql = "SELECT id, recipe_name, required_materials FROM crafting_recipes";
        DBUtils.executeQueryAsync(sql,
                stmt -> {
                },
                rs -> {
                    List<CraftingRecipe> fetchedRecipes = new ArrayList<>();
                    while (rs.next()) {
                        Map<String, Integer> requiredMaterials = new HashMap<>();
                        JSONObject json = new JSONObject(rs.getString("required_materials"));
                        json.keys().forEachRemaining(key ->
                                requiredMaterials.put(key.toLowerCase(), json.getInt(key)));

                        fetchedRecipes.add(new CraftingRecipe(
                                rs.getInt("id"),
                                rs.getString("recipe_name"),
                                requiredMaterials
                        ));
                    }
                    return fetchedRecipes;
                },
                fetchedRecipes -> {
                    recipes = fetchedRecipes;
                    updateCraftingUI();
                },
                error -> FXUtils.showError("Failed to load recipes", "Database Error")
        );
    }

    private void fetchInventory(int userId) {
        String sql = "SELECT material_type, quantity FROM raw_material_inventory WHERE user_id = ?";
        DBUtils.executeQueryAsync(sql,
                stmt -> stmt.setInt(1, userId),
                rs -> {
                    List<RawMaterial> fetchedInventory = new ArrayList<>();
                    while (rs.next()) {
                        fetchedInventory.add(new RawMaterial(
                                rs.getString("material_type"),
                                rs.getInt("quantity")
                        ));
                    }
                    return fetchedInventory;
                },
                fetchedInventory -> {
                    inventory = fetchedInventory;
                    updateInventoryGrid();
                },
                error -> FXUtils.showError("Failed to load inventory", "Database Error")
        );
    }

    private void updateInventoryGrid() {
        inventoryGrid.getChildren().clear();
        int col = 0, row = 0;

        for (RawMaterial material : inventory.stream()
                .filter(m -> m.quantity() > 0)
                .collect(Collectors.toList())) {

            VBox slot = createInventorySlot(material);
            inventoryGrid.add(slot, col, row);

            if (++col >= INVENTORY_COLUMNS) {
                col = 0;
                row++;
            }
        }
        setupInventoryClickHandlers();
    }

    private VBox createInventorySlot(RawMaterial material) {
        VBox slot = new VBox();
        slot.getStyleClass().addAll("inventory-slot", "filled");
        slot.setUserData(material.materialType());

        ImageView icon = createItemIcon(material.materialType(), ITEM_ICON_SIZE);
        Label count = new Label(String.valueOf(material.quantity()));
        count.getStyleClass().add("item-count");

        slot.getChildren().addAll(icon, count);
        return slot;
    }

    private ImageView createItemIcon(String materialType, int size) {
        ImageView icon = new ImageView();
        icon.getStyleClass().add("item-icon");
        icon.setFitWidth(size);
        icon.setFitHeight(size);

        String imagePath = RESOURCES_PATH + "/raw_materials/" +
                Formater.toSnakeCase(materialType) + ".png";
        loadImage(icon, imagePath);

        return icon;
    }

    private void loadImage(ImageView imageView, String path) {
        java.io.InputStream is = getClass().getResourceAsStream(path);
        if (is != null) {
            imageView.setImage(new Image(is));
        }
    }

    private void setupClickHandlers() {
        for (VBox slot : Arrays.asList(slot1, slot2, slot3)) {
            slot.setOnMouseClicked(event -> {
                if (!slot.getChildren().isEmpty()) {
                    clearSlot(slot);
                    updateCraftingUI();
                }
                event.consume();
            });
        }
    }

    private void clearSlot(VBox slot) {
        slot.getChildren().clear();
        slot.getStyleClass().remove("filled");
        craftingSlots.remove(slot);
    }

    private void setupInventoryClickHandlers() {
        inventoryGrid.getChildren().stream()
                .filter(VBox.class::isInstance)
                .map(VBox.class::cast)
                .forEach(slot -> slot.setOnMouseClicked(event -> handleInventoryClick(slot, event)));
    }

    private void handleInventoryClick(VBox slot, javafx.scene.input.MouseEvent event) {
        String materialType = (String) slot.getUserData();
        if (materialType == null) return;

        Optional<RawMaterial> materialOpt = inventory.stream()
                .filter(m -> m.materialType().equals(materialType) && m.quantity() > 0)
                .findFirst();

        if (!materialOpt.isPresent()) return;

        RawMaterial material = materialOpt.get();
        if (!canAddMaterialToCrafting(material)) {
            FXUtils.showError("Not enough " + materialType + " in inventory", "Crafting");
            return;
        }

        VBox targetSlot = findAvailableCraftingSlot(materialType);
        if (targetSlot == null) {
            FXUtils.showError("No empty crafting slots available", "Crafting");
            return;
        }

        addMaterialToSlot(targetSlot, materialType);
        updateCraftingUI();
        event.consume();
    }

    private boolean canAddMaterialToCrafting(RawMaterial material) {
        long currentCount = craftingSlots.values().stream()
                .mapToLong(m -> m.getOrDefault(material.materialType(), 0))
                .sum();
        return currentCount < material.quantity();
    }

    private VBox findAvailableCraftingSlot(String materialType) {
        return Arrays.asList(slot1, slot2, slot3).stream()
                .filter(s -> craftingSlots.getOrDefault(s, new HashMap<>()).containsKey(materialType))
                .findFirst()
                .orElseGet(() -> Arrays.asList(slot1, slot2, slot3).stream()
                        .filter(s -> s.getChildren().isEmpty())
                        .findFirst()
                        .orElse(null));
    }

    private void addMaterialToSlot(VBox targetSlot, String materialType) {
        Map<String, Integer> slotMaterials = craftingSlots.computeIfAbsent(targetSlot, k -> new HashMap<>());
        slotMaterials.merge(materialType, 1, Integer::sum);

        targetSlot.getChildren().clear();
        ImageView icon = createItemIcon(materialType, ITEM_ICON_SIZE);
        int slotCount = slotMaterials.get(materialType);
        Label count = new Label(String.valueOf(slotCount));
        count.getStyleClass().add("item-count");

        targetSlot.getChildren().addAll(icon, count);
        targetSlot.getStyleClass().add("filled");
    }

    private void updateCraftingUI() {
        updateCraftButtonState();
        updateCraftingSlotsUI();
        updateResultSlot();
    }

    private void updateCraftButtonState() {
        boolean hasMaterials = !craftingSlots.isEmpty();
        craftButton.setDisable(!hasMaterials);
        craftButton.getStyleClass().remove("disabled");
        if (!hasMaterials) {
            craftButton.getStyleClass().add("disabled");
        }
    }

    private void updateCraftingSlotsUI() {
        for (VBox slot : Arrays.asList(slot1, slot2, slot3)) {
            Map<String, Integer> materials = craftingSlots.getOrDefault(slot, new HashMap<>());
            if (materials.isEmpty()) {
                clearSlot(slot);
                continue;
            }

            String materialType = materials.keySet().iterator().next();
            updateSlotUI(slot, materialType, materials.get(materialType));
        }
    }

    private void updateSlotUI(VBox slot, String materialType, int count) {
        slot.getChildren().clear();
        ImageView icon = createItemIcon(materialType, ITEM_ICON_SIZE);
        Label countLabel = new Label(String.valueOf(count));
        countLabel.getStyleClass().add("item-count");

        slot.getChildren().addAll(icon, countLabel);
        slot.getStyleClass().add("filled");
    }

    private void updateResultSlot() {
        if (resultIcon == null) return;

        Map<String, Integer> placedMaterials = getPlacedMaterials();
        Optional<CraftingRecipe> matchingRecipe = findMatchingRecipe(placedMaterials);

        matchingRecipe.ifPresentOrElse(
                recipe -> displayRecipeResult(recipe.craftedItemName()),
                () -> resultIcon.setImage(null)
        );
    }

    private Map<String, Integer> getPlacedMaterials() {
        Map<String, Integer> placedMaterials = new HashMap<>();
        craftingSlots.values().forEach(slotMaterials ->
                slotMaterials.forEach((material, count) ->
                        placedMaterials.merge(material.toLowerCase(), count, Integer::sum)));
        return placedMaterials;
    }

    private Optional<CraftingRecipe> findMatchingRecipe(Map<String, Integer> placedMaterials) {
        return recipes.stream()
                .filter(recipe -> {
                    Map<String, Integer> required = recipe.requiredMaterials();
                    return required.size() == placedMaterials.size() &&
                            required.entrySet().stream().allMatch(e ->
                                    placedMaterials.getOrDefault(e.getKey(), 0).equals(e.getValue()));
                })
                .findFirst();
    }

    private void displayRecipeResult(String craftedItemName) {
        String imagePath = "/com/bigsteppers/stockcraftz/images/crafted_items/" +
                Formater.toSnakeCase(craftedItemName) + ".png";
        loadImage(resultIcon, imagePath);
    }

    @FXML
    private void clearCraftingGrid() {
        Arrays.asList(slot1, slot2, slot3).forEach(this::clearSlot);
        updateCraftingUI();
    }

    @FXML
    private void craftItem() {
        int userId = SessionManager.getCurrentUser().id();
        Map<String, Integer> placedMaterials = getPlacedMaterials();

        findMatchingRecipe(placedMaterials).ifPresentOrElse(
                recipe -> processCrafting(userId, recipe),
                () -> FXUtils.showError("Invalid recipe! Check the crafting guide.", "Crafting")
        );
    }

    private void processCrafting(int userId, CraftingRecipe recipe) {
        if (!hasEnoughMaterials(recipe)) {
            return;
        }

        deductMaterials(userId, recipe);
        addCraftedItem(userId, recipe.craftedItemName());
    }

    private boolean hasEnoughMaterials(CraftingRecipe recipe) {
        Map<String, Integer> available = inventory.stream()
                .collect(Collectors.toMap(
                        m -> m.materialType().toLowerCase(),
                        RawMaterial::quantity
                ));

        for (Map.Entry<String, Integer> req : recipe.requiredMaterials().entrySet()) {
            int needed = req.getValue();
            int owned = available.getOrDefault(req.getKey(), 0);
            if (owned < needed) {
                FXUtils.showError(formatMaterialError(req.getKey(), needed, owned), "Crafting");
                return false;
            }
        }
        return true;
    }

    private String formatMaterialError(String material, int needed, int owned) {
        return String.format("Not enough %s to craft. Need %d, have %d.", material, needed, owned);
    }

    private void deductMaterials(int userId, CraftingRecipe recipe) {
        for (Map.Entry<String, Integer> req : recipe.requiredMaterials().entrySet()) {
            String materialType = req.getKey();
            int needed = req.getValue();

            String updateSql = "UPDATE raw_material_inventory SET quantity = quantity - ? " +
                    "WHERE user_id = ? AND material_type = ? AND quantity >= ?";
            DBUtils.executeUpdateAsync(updateSql,
                    stmt -> {
                        stmt.setInt(1, needed);
                        stmt.setInt(2, userId);
                        stmt.setString(3, materialType.toUpperCase());
                        stmt.setInt(4, needed);
                    },
                    success -> {
                        if (!success) {
                            handleZeroQuantityMaterial(userId, materialType, needed);
                        }
                    },
                    error -> {
                    }
            );
        }
    }

    private void handleZeroQuantityMaterial(int userId, String materialType, int quantity) {
        String deleteSql = "DELETE FROM raw_material_inventory " +
                "WHERE user_id = ? AND material_type = ? AND quantity = ?";
        DBUtils.executeUpdateAsync(deleteSql,
                stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setString(2, materialType.toUpperCase());
                    stmt.setInt(3, quantity);
                },
                success -> {
                },
                error -> {
                }
        );
    }

    private void addCraftedItem(int userId, String craftedItemName) {
        String insertSql = "INSERT INTO crafted_item_inventory (user_id, crafted_item_name) " +
                "VALUES (?, ?) ON CONFLICT (user_id, crafted_item_name) " +
                "DO UPDATE SET quantity = crafted_item_inventory.quantity + 1";

        DBUtils.executeUpdateAsync(insertSql,
                stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setString(2, craftedItemName);
                },
                success -> {
                    if (success) {
                        handleSuccessfulCraft(userId, craftedItemName);
                    } else {
                        FXUtils.showError("Failed to craft item", "Crafting");
                    }
                },
                error -> {
                }
        );
    }

    private void handleSuccessfulCraft(int userId, String craftedItemName) {
        logCraftingActivity(userId, craftedItemName);
        fetchInventory(userId);
        clearCraftingGrid();
        FXUtils.showSuccess(craftedItemName + " crafted successfully", "Crafting");
    }

    private void logCraftingActivity(int userId, String craftedItemName) {
        String sql = "INSERT INTO user_activities (user_id, action_type, item_name, amount, timestamp) " +
                "VALUES (?, ?, ?, 1, NOW())";
        DBUtils.executeUpdateAsync(sql,
                stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setString(2, CRAFT_ACTION);
                    stmt.setString(3, craftedItemName);
                },
                success -> {
                },
                error -> {
                }
        );
    }

    @FXML
    private void navigateToRawMaterials() {
        FXUtils.navigateTo("rawMaterials", craftingPage);
    }

    @FXML
    private void navigateToMarketplace() {
        FXUtils.navigateTo("marketplace", craftingPage);
    }

    // Record definitions
    private record RawMaterial(String materialType, int quantity) {
    }

    private record CraftingRecipe(int id, String craftedItemName, Map<String, Integer> requiredMaterials) {
    }
}