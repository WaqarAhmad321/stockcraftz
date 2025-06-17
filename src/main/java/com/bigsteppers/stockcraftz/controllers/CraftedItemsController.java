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
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CraftedItemsController implements LoadablePage {

    private final int itemsPerPage = 8; // 4x2 grid
    @FXML
    private Node craftedItemsPage;
    @FXML
    private TextField craftedItemSearch;
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
        System.out.println("CraftedItemsController: Initializing...");
        checkFXMLComponents();
        if (navbarController != null) {
            navbarController.setActivePage("craftedItems");
            navbarController.rootNode = craftedItemsPage;
        }
        craftedItemSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("CraftedItemsController: Search query changed to: " + newVal);
            applySearchFilter(newVal);
        });
    }

    private void checkFXMLComponents() {
        if (craftedItemsPage == null) System.err.println("CraftedItemsController: craftedItemsPage is null");
        if (craftedItemSearch == null) System.err.println("CraftedItemsController: craftedItemSearch is null");
        if (inventoryGrid == null) System.err.println("CraftedItemsController: inventoryGrid is null");
        if (paginationBox == null) System.err.println("CraftedItemsController: paginationBox is null");
        if (navbarController == null) System.err.println("CraftedItemsController: navbarController is null");
    }

    @Override
    public void onLoad() {
        if (!SessionManager.isLoggedIn()) {
            System.err.println("CraftedItemsController: No user logged in, redirecting to login");
            FXUtils.navigateTo("login", craftedItemsPage);
            return;
        }
        try {
            int userId = SessionManager.getCurrentUser().id();
            System.out.println("CraftedItemsController: User logged in, userId: " + userId);
            fetchCraftedItems();
        } catch (Exception e) {
            System.err.println("CraftedItemsController: Error accessing user ID: " + e.getMessage());
            e.printStackTrace();
            FXUtils.navigateTo("login", craftedItemsPage);
        }
    }

    public void fetchCraftedItems() {
        System.out.println("CraftedItemsController: Fetching crafted items");
        int userId = SessionManager.getCurrentUser().id();
        String sql = "SELECT id, crafted_item_name, quantity FROM crafted_item_inventory WHERE user_id = ?";
        DBUtils.executeQueryAsync(sql, stmt -> {
            try {
                stmt.setInt(1, userId);
            } catch (SQLException e) {
                throw new RuntimeException("Error setting userId", e);
            }
        }, rs -> {
            ArrayList<CraftedItem> craftedItems = new ArrayList<>();
            try {
                while (rs.next()) {
                    craftedItems.add(new CraftedItem(
                            rs.getInt("id"),
                            rs.getString("crafted_item_name"),
                            rs.getInt("quantity")
                    ));
                }
                System.out.println("CraftedItemsController: Fetched " + craftedItems.size() + " crafted items");
            } catch (SQLException e) {
                throw new RuntimeException("Error reading result set", e);
            }
            return craftedItems;
        }, items -> {
            allCraftedItems = items;
            filteredCraftedItems = new ArrayList<>(items);
            totalPages = (int) Math.ceil((double) filteredCraftedItems.size() / itemsPerPage);
            System.out.println("CraftedItemsController: Total pages: " + totalPages);
            updateInventoryGrid();
            updatePagination();
        }, error -> System.err.println("CraftedItemsController: Error fetching crafted items: " + error.getMessage()));
    }

    private void applySearchFilter(String query) {
        System.out.println("CraftedItemsController: Applying search filter: " + query);
        if (query == null || query.trim().isEmpty()) {
            filteredCraftedItems = new ArrayList<>(allCraftedItems);
        } else {
            String lowerQuery = query.toLowerCase();
            filteredCraftedItems = allCraftedItems.stream()
                    .filter(item -> item.getCraftedItemName().toLowerCase().contains(lowerQuery))
                    .collect(Collectors.toList());
        }
        currentPage = 1;
        totalPages = (int) Math.ceil((double) filteredCraftedItems.size() / itemsPerPage);
        updateInventoryGrid();
        updatePagination();
    }

    private void updateInventoryGrid() {
        System.out.println("CraftedItemsController: Updating inventory grid, currentPage: " + currentPage);
        if (inventoryGrid == null) {
            System.err.println("CraftedItemsController: inventoryGrid is null");
            return;
        }
        inventoryGrid.getChildren().clear();
        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, filteredCraftedItems.size());
        int row = 0, col = 0;
        for (int i = startIndex; i < endIndex; i++) {
            CraftedItem item = filteredCraftedItems.get(i);
            VBox itemBox = new VBox();
            itemBox.getStyleClass().add("inventory-item");
            itemBox.setSpacing(5);

            ImageView icon = new ImageView();
            icon.getStyleClass().add("item-icon");
            icon.setFitWidth(65);
            icon.setFitHeight(65);
            String imagePath = "/com/bigsteppers/stockcraftz/images/crafted_items/" + Formater.toSnakeCase(item.getCraftedItemName()) + ".png";
            java.io.InputStream is = getClass().getResourceAsStream(imagePath);
            if (is != null) {
                icon.setImage(new Image(is));
            } else {
                System.err.println("CraftedItemsController: Failed to load image: " + imagePath);
            }

            Label nameLabel = new Label(item.getCraftedItemName());
            nameLabel.getStyleClass().add("item-name");

            Label quantityLabel = new Label("x" + item.getQuantity());
            quantityLabel.getStyleClass().add("item-quantity");

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
            itemBox.getChildren().addAll(icon, nameLabel, quantityLabel, actionsBox);

            itemBox.setOnMouseEntered(e -> actionsBox.setVisible(true));
            itemBox.setOnMouseExited(e -> actionsBox.setVisible(false));

            inventoryGrid.add(itemBox, col, row);
            col++;
            if (col > 3) {
                col = 0;
                row++;
            }
        }
        System.out.println("CraftedItemsController: Inventory grid updated with " + (endIndex - startIndex) + " items");
    }

    private void updatePagination() {
        System.out.println("CraftedItemsController: Updating pagination, currentPage: " + currentPage + ", totalPages: " + totalPages);
        PaginationUtils.updatePagination(paginationBox, currentPage, totalPages, page -> {
            currentPage = page;
            updateInventoryGrid();
            updatePagination();
        });
    }

    private void handleSellCraftedItem(CraftedItem item) {
        int userId = SessionManager.getCurrentUser().id();
        String insertSql = "INSERT INTO marketplace_items (item_name, item_type, price, seller_id) VALUES (?, 'TOOL', ?, ?)";

        DBUtils.executeUpdateAsync(insertSql, stmt -> {
            stmt.setString(1, item.getCraftedItemName());
            stmt.setDouble(2, 10.0); // Example price
            stmt.setInt(3, userId);
        }, success -> {
            if (success) FXUtils.showSuccess(item.getCraftedItemName() + " listed for sale.", "Success");
        }, error -> FXUtils.showError("Failed to list " + item.getCraftedItemName() + ".", "Marketplace Error"));
    }

    private void handleDeleteCraftedItem(CraftedItem item) {
        System.out.println("CraftedItemsController: Initiating deletion for crafted item: " + item.getCraftedItemName());
        String itemName = item.getCraftedItemName();
        boolean confirmed = FXUtils.showConfirmation("Remove " + itemName + "?", "Confirm Deletion");
        if (!confirmed) {
            System.out.println("CraftedItemsController: Deletion cancelled for item: " + itemName);
            return;
        }

        int userId = SessionManager.getCurrentUser().id();
        int itemId = item.getId();

        String deleteSql = "DELETE FROM crafted_item_inventory WHERE id = ? AND user_id = ? RETURNING id";
        DBUtils.executeQueryAsync(deleteSql, stmt -> {
            try {
                stmt.setInt(1, itemId);
                stmt.setInt(2, userId);
            } catch (SQLException e) {
                throw new RuntimeException("Error setting parameters", e);
            }
        }, rs -> {
            int deletedId = -1;
            try {
                if (rs.next()) {
                    deletedId = rs.getInt("id");
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error reading deleted ID", e);
            }
            return deletedId;
        }, deletedId -> {
            if (deletedId == itemId && deletedId > 0) {
                System.out.println("CraftedItemsController: Crafted item deleted, ID: " + deletedId);
                logRemoveActivity(userId, itemName);
                FXUtils.showSuccess(itemName + " removed successfully.", "Success");
                fetchCraftedItems();
            } else {
                System.err.println("CraftedItemsController: Deletion failed, ID: " + itemId);
                FXUtils.showError("Failed to delete " + itemName + ".", "Crafted Items Error");
            }
        }, error -> {
            System.err.println("CraftedItemsController: Error deleting crafted item: " + error.getMessage());
            FXUtils.showError("Error deleting " + itemName + ": " + error.getMessage(), "Crafted Items Error");
        });
    }

    private void logRemoveActivity(int userId, String itemName) {
        System.out.println("CraftedItemsController: Logging remove activity for item: " + itemName);
        String sql = "INSERT INTO user_activities (user_id, action_type, item_name, amount, timestamp) " +
                "VALUES (?, 'REMOVE', ?, 1, NOW())";
        DBUtils.executeUpdateAsync(sql, stmt -> {
            try {
                stmt.setInt(1, userId);
                stmt.setString(2, itemName);
            } catch (SQLException e) {
                throw new RuntimeException("Error logging activity", e);
            }
        }, success -> {
            if (success) {
                System.out.println("CraftedItemsController: Logged remove activity for " + itemName);
            } else {
                System.err.println("CraftedItemsController: Failed to log remove activity for " + itemName);
            }
        }, error -> System.err.println("CraftedItemsController: Error logging remove activity: " + error.getMessage()));
    }

    @FXML
    private void navigateToCrafting() {
        System.out.println("CraftedItemsController: Navigating to crafting page");
        FXUtils.navigateTo("crafting", craftedItemsPage);
    }

    @FXML
    private void navigateToMarketplace() {
        System.out.println("CraftedItemsController: Navigating to marketplace page");
        FXUtils.navigateTo("marketplace", craftedItemsPage);
    }
}