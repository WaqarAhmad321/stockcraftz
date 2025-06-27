package com.bigsteppers.stockcraftz.controllers;

import com.bigsteppers.stockcraftz.interfaces.LoadablePage;
import com.bigsteppers.stockcraftz.model.CraftedItem;
import com.bigsteppers.stockcraftz.model.SessionManager;
import com.bigsteppers.stockcraftz.utils.FXUtils;
import com.bigsteppers.stockcraftz.utils.Formater;
import com.bigsteppers.stockcraftz.utils.PaginationUtils;
import com.dbfx.database.DBUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class CraftedItemsController implements LoadablePage {

    private static final int ITEMS_PER_PAGE = 8; // 4x2 grid
    private static final String DEFAULT_ITEM_TYPE = "TOOL";
    private static final double DEFAULT_SELL_PRICE = 100.0;

    @FXML
    private Node craftedItemsPage;
    @FXML
    private GridPane inventoryGrid;
    @FXML
    private HBox paginationBox;
    @FXML
    private NavbarController navbarController;

    private List<CraftedItem> allCraftedItems = new ArrayList<>();
    private List<CraftedItem> filteredCraftedItems = new ArrayList<>();
    private int currentPage = 1;
    private int totalPages = 1;

    @FXML
    private void initialize() {
        navbarController.setActivePage("craftedItems");
        navbarController.rootNode = craftedItemsPage;
    }

    @Override
    public void onLoad() {
        if (!SessionManager.isLoggedIn()) {
            FXUtils.navigateTo("login", craftedItemsPage);
            return;
        }
        fetchCraftedItems();
    }

    private void fetchCraftedItems() {
        int userId = SessionManager.getCurrentUser().id();
        String sql = "SELECT id, crafted_item_name, quantity FROM crafted_item_inventory WHERE user_id = ?";

        DBUtils.executeQueryAsync(sql,
                stmt -> stmt.setInt(1, userId),
                rs -> {
                    ArrayList<CraftedItem> items = new ArrayList<>();
                    while (rs.next()) {
                        items.add(new CraftedItem(
                                rs.getInt("id"),
                                rs.getString("crafted_item_name"),
                                rs.getInt("quantity")
                        ));
                    }
                    return items;
                },
                items -> {
                    allCraftedItems = items;
                    filteredCraftedItems = new ArrayList<>(items);
                    totalPages = (int) Math.ceil((double) filteredCraftedItems.size() / ITEMS_PER_PAGE);
                    updateInventoryGrid();
                    updatePagination();
                },
                error -> FXUtils.showError("Failed to load crafted items", "Database Error")
        );
    }

    private void updateInventoryGrid() {
        inventoryGrid.getChildren().clear();
        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredCraftedItems.size());

        int row = 0, col = 0;
        for (int i = startIndex; i < endIndex; i++) {
            CraftedItem item = filteredCraftedItems.get(i);
            inventoryGrid.add(createItemBox(item), col, row);

            col++;
            if (col > 3) {
                col = 0;
                row++;
            }
        }
    }

    private VBox createItemBox(CraftedItem item) {
        VBox itemBox = new VBox();
        itemBox.getStyleClass().add("inventory-item");
        itemBox.setSpacing(5);

        ImageView icon = createItemIcon(item);
        Label nameLabel = new Label(item.getCraftedItemName());
        nameLabel.getStyleClass().add("item-name");

        Label quantityLabel = new Label("x" + item.getQuantity());
        quantityLabel.getStyleClass().add("item-quantity");

        VBox actionsBox = createActionBox(item);
        itemBox.getChildren().addAll(icon, nameLabel, quantityLabel, actionsBox);

        itemBox.setOnMouseEntered(e -> actionsBox.setVisible(true));
        itemBox.setOnMouseExited(e -> actionsBox.setVisible(false));

        return itemBox;
    }

    private ImageView createItemIcon(CraftedItem item) {
        ImageView icon = new ImageView();
        icon.getStyleClass().add("item-icon");
        icon.setFitWidth(65);
        icon.setFitHeight(65);

        String imagePath = "/com/bigsteppers/stockcraftz/images/crafted_items/" +
                Formater.toSnakeCase(item.getCraftedItemName()) + ".png";
        java.io.InputStream is = getClass().getResourceAsStream(imagePath);
        if (is != null) {
            icon.setImage(new Image(is));
        }
        return icon;
    }

    private VBox createActionBox(CraftedItem item) {
        VBox actionsBox = new VBox();
        actionsBox.getStyleClass().add("item-actions");
        actionsBox.setVisible(false);
        actionsBox.setSpacing(5);

        Button sellButton = new Button("SELL");
        sellButton.getStyleClass().addAll("btn", "small");
        sellButton.setOnAction(e -> handleSellCraftedItem(item));

        Button deleteButton = new Button("REMOVE");
        deleteButton.getStyleClass().addAll("btn", "small", "danger");
        deleteButton.setOnAction(e -> handleDeleteCraftedItem(item));

        actionsBox.getChildren().addAll(sellButton, deleteButton);
        return actionsBox;
    }

    private void updatePagination() {
        PaginationUtils.updatePagination(paginationBox, currentPage, totalPages, page -> {
            currentPage = page;
            updateInventoryGrid();
        });
    }

    private void handleSellCraftedItem(CraftedItem item) {
        int userId = SessionManager.getCurrentUser().id();
        String insertSql = "INSERT INTO marketplace_items (item_name, item_type, price, seller_id) VALUES (?, ?, ?, ?)";

        DBUtils.executeUpdateAsync(insertSql,
                stmt -> {
                    stmt.setString(1, item.getCraftedItemName());
                    stmt.setString(2, DEFAULT_ITEM_TYPE);
                    stmt.setDouble(3, DEFAULT_SELL_PRICE);
                    stmt.setInt(4, userId);
                },
                success -> {
                    if (success) {
                        FXUtils.showSuccess(item.getCraftedItemName() + " listed for sale.", "Success");
                    } else {
                        FXUtils.showError("Failed to list item", "Marketplace Error");
                    }
                },
                error -> FXUtils.showError("Error listing item", "Marketplace Error")
        );
    }

    private void handleDeleteCraftedItem(CraftedItem item) {
        String itemName = item.getCraftedItemName();
        boolean confirmed = FXUtils.showConfirmation(
                "Remove 1 unit of " + itemName + " (x" + item.getQuantity() + ")?",
                "Confirm Removal"
        );

        if (!confirmed) return;

        if (item.getQuantity() > 1) {
            reduceItemQuantity(item);
        } else {
            deleteItem(item);
        }
    }

    private void reduceItemQuantity(CraftedItem item) {
        String updateSql = "UPDATE crafted_item_inventory SET quantity = quantity - 1 WHERE id = ? AND user_id = ? RETURNING quantity";

        DBUtils.executeQueryAsync(updateSql,
                stmt -> {
                    stmt.setInt(1, item.getId());
                    stmt.setInt(2, SessionManager.getCurrentUser().id());
                },
                rs -> rs.next() ? rs.getInt("quantity") : -1,
                newQuantity -> {
                    if (newQuantity >= 0) {
                        logRemoveActivity(item.getCraftedItemName(), 1);
                        FXUtils.showSuccess("1 unit removed successfully", "Success");
                        fetchCraftedItems();
                    } else {
                        FXUtils.showError("Failed to reduce quantity", "Error");
                    }
                },
                error -> FXUtils.showError("Error reducing quantity", "Error")
        );
    }

    private void deleteItem(CraftedItem item) {
        String deleteSql = "DELETE FROM crafted_item_inventory WHERE id = ? AND user_id = ? RETURNING id";

        DBUtils.executeQueryAsync(deleteSql,
                stmt -> {
                    stmt.setInt(1, item.getId());
                    stmt.setInt(2, SessionManager.getCurrentUser().id());
                },
                rs -> rs.next() ? rs.getInt("id") : -1,
                deletedId -> {
                    if (deletedId > 0) {
                        logRemoveActivity(item.getCraftedItemName(), 1);
                        FXUtils.showSuccess("Item removed successfully", "Success");
                        fetchCraftedItems();
                    } else {
                        FXUtils.showError("Failed to delete item", "Error");
                    }
                },
                error -> FXUtils.showError("Error deleting item", "Error")
        );
    }

    private void logRemoveActivity(String itemName, int amount) {
        String sql = "INSERT INTO user_activities (user_id, action_type, item_name, amount, timestamp) " +
                "VALUES (?, 'REMOVE', ?, ?, NOW())";

        DBUtils.executeUpdateAsync(sql,
                stmt -> {
                    stmt.setInt(1, SessionManager.getCurrentUser().id());
                    stmt.setString(2, itemName);
                    stmt.setInt(3, amount);
                },
                success -> {
                },
                error -> {
                }
        );
    }

    @FXML
    private void navigateToCrafting() {
        FXUtils.navigateTo("crafting", craftedItemsPage);
    }

    @FXML
    private void navigateToMarketplace() {
        FXUtils.navigateTo("marketplace", craftedItemsPage);
    }
}