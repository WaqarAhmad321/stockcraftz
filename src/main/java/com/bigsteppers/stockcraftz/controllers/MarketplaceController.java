package com.bigsteppers.stockcraftz.controllers;

import com.bigsteppers.stockcraftz.interfaces.LoadablePage;
import com.bigsteppers.stockcraftz.model.MarketplaceItem;
import com.bigsteppers.stockcraftz.model.SessionManager;
import com.bigsteppers.stockcraftz.utils.FXUtils;
import com.bigsteppers.stockcraftz.utils.Formater;
import com.bigsteppers.stockcraftz.utils.PaginationUtils;
import com.dbfx.database.DBUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
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

public class MarketplaceController implements LoadablePage {

    private final int itemsPerPage = 8; // 4x2 grid
    @FXML
    private Node marketplacePage;
    @FXML
    private TextField marketSearch;
    @FXML
    private Button allFilter;
    @FXML
    private Button rawMaterialsFilter;
    @FXML
    private Button toolsFilter;
    @FXML
    private GridPane inventoryGrid;
    @FXML
    private HBox paginationBox;
    @FXML
    private NavbarController navbarController;
    private List<MarketplaceItem> allItems = new ArrayList<>();
    private List<MarketplaceItem> filteredItems = new ArrayList<>();
    private String currentFilter = "ALL";
    private int currentPage = 1;
    private int totalPages = 1;

    @FXML
    private void initialize() {
        System.out.println("MarketplaceController: Initializing...");
        checkFXMLComponents();
        if (navbarController != null) {
            navbarController.setActivePage("marketplace");
            navbarController.rootNode = marketplacePage;
        }
        marketSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("MarketplaceController: Search query changed to: " + newVal);
            // TODO: Implement search filtering
        });
    }

    private void checkFXMLComponents() {
        if (marketplacePage == null) System.err.println("MarketplaceController: marketplacePage is null");
        if (marketSearch == null) System.err.println("MarketplaceController: marketSearch is null");
        if (allFilter == null) System.err.println("MarketplaceController: allFilter is null");
        if (rawMaterialsFilter == null) System.err.println("MarketplaceController: rawMaterialsFilter is null");
        if (toolsFilter == null) System.err.println("MarketplaceController: toolsFilter is null");
        if (inventoryGrid == null) System.err.println("MarketplaceController: inventoryGrid is null");
        if (paginationBox == null) System.err.println("MarketplaceController: paginationBox is null");
        if (navbarController == null) System.err.println("MarketplaceController: navbarController is null");
    }

    @Override
    public void onLoad() {
        if (!SessionManager.isLoggedIn()) {
            System.err.println("MarketplaceController: No user logged in, redirecting to login");
            FXUtils.navigateTo("login", marketplacePage);
            return;
        }
        try {
            int userId = SessionManager.getCurrentUser().id();
            System.out.println("MarketplaceController: User logged in, userId: " + userId);
            fetchMarketplaceItems();
        } catch (Exception e) {
            System.err.println("MarketplaceController: Error accessing user ID: " + e.getMessage());
            e.printStackTrace();
            FXUtils.navigateTo("login", marketplacePage);
        }
    }

    private void fetchMarketplaceItems() {
        System.out.println("MarketplaceController: Fetching marketplace items");
        String sql = "SELECT id, item_name, item_type, price, image_url, seller_id FROM marketplace_items";
        DBUtils.executeQueryAsync(sql, stmt -> {
        }, rs -> {
            ArrayList<MarketplaceItem> items = new ArrayList<>();
            try {
                while (rs.next()) {
                    items.add(new MarketplaceItem(
                            rs.getInt("id"),
                            rs.getString("item_name"),
                            rs.getString("item_type"),
                            rs.getDouble("price"),
                            rs.getString("image_url"),
                            rs.getInt("seller_id")
                    ));
                }
                System.out.println("MarketplaceController: Fetched " + items.size() + " items from database");
            } catch (SQLException e) {
                throw new RuntimeException("Error reading result set", e);
            }
            return items;
        }, items -> {
            allItems = items;
            applyFilter(currentFilter);
            updateInventoryGrid();
            updatePagination();
            System.out.println("MarketplaceController: Successfully processed " + items.size() + " items");
        }, error -> System.err.println("MarketplaceController: Error fetching marketplace items: " + error.getMessage()));
    }

    private void applyFilter(String filter) {
        System.out.println("MarketplaceController: Applying filter: " + filter);
        currentFilter = filter;
        allFilter.getStyleClass().remove("active");
        rawMaterialsFilter.getStyleClass().remove("active");
        toolsFilter.getStyleClass().remove("active");
        switch (filter) {
            case "RAW_MATERIALS":
                filteredItems = allItems.stream()
                        .filter(item -> item.itemType().equals("RAW_MATERIAL"))
                        .collect(Collectors.toList());
                rawMaterialsFilter.getStyleClass().add("active");
                break;
            case "TOOLS":
                filteredItems = allItems.stream()
                        .filter(item -> item.itemType().equals("TOOL"))
                        .collect(Collectors.toList());
                toolsFilter.getStyleClass().add("active");
                break;
            default:
                filteredItems = new ArrayList<>(allItems);
                allFilter.getStyleClass().add("active");
                break;
        }
        currentPage = 1;
        totalPages = (int) Math.ceil((double) filteredItems.size() / itemsPerPage);
        System.out.println("MarketplaceController: Filtered " + filteredItems.size() + " items, total pages: " + totalPages);
        updateInventoryGrid();
        updatePagination();
    }

    private void updateInventoryGrid() {
        System.out.println("MarketplaceController: Updating inventory grid, currentPage: " + currentPage);
        if (inventoryGrid == null) {
            System.err.println("MarketplaceController: inventoryGrid is null");
            return;
        }
        inventoryGrid.getChildren().clear();
        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, filteredItems.size());
        int row = 0, col = 0;
        for (int i = startIndex; i < endIndex; i++) {
            MarketplaceItem item = filteredItems.get(i);
            VBox itemBox = new VBox();
            itemBox.getStyleClass().add("marketplace-item");
            itemBox.setSpacing(5);

            ImageView icon = new ImageView();
            icon.getStyleClass().add("item-icon");
            icon.setFitWidth(40);
            icon.setFitHeight(40);
            String imagePath = "/com/bigsteppers/stockcraftz/images/crafted_items/" + Formater.toSnakeCase(item.itemName()) + ".png";
            java.io.InputStream is = getClass().getResourceAsStream(imagePath);
            if (is != null) {
                icon.setImage(new Image(is));
            } else {
                System.err.println("MarketplaceController: Failed to load image: " + imagePath);
            }

            Label nameLabel = new Label(item.itemName());
            nameLabel.getStyleClass().add("item-name");

            Label priceLabel = new Label(String.format("$%.2f", item.price()));
            priceLabel.getStyleClass().add("item-price");

            if (item.sellerId() != SessionManager.getCurrentUser().id()) {
                Button buyButton = new Button("BUY");
                buyButton.getStyleClass().addAll("btn", "small");
                buyButton.setOnAction(e -> handleBuyItem(item));
                itemBox.getChildren().addAll(icon, nameLabel, priceLabel, buyButton);
            } else {
                itemBox.getChildren().addAll(icon, nameLabel, priceLabel);
            }

            inventoryGrid.add(itemBox, col, row);
            col++;
            if (col > 3) {
                col = 0;
                row++;
            }
        }
        System.out.println("MarketplaceController: Inventory grid updated with " + (endIndex - startIndex) + " items");
    }

    private void updatePagination() {
        System.out.println("MarketplaceController: Updating pagination, currentPage: " + currentPage + ", totalPages: " + totalPages);
        PaginationUtils.updatePagination(paginationBox, currentPage, totalPages, page -> {
            currentPage = page;
            updateInventoryGrid();
            updatePagination();
        });
    }

    private void handleBuyItem(MarketplaceItem item) {
        System.out.println("MarketplaceController: Attempting to buy item: " + item.itemName());
        int userId = SessionManager.getCurrentUser().id();
        double price = item.price();

        // Check user balance
        String balanceSql = "SELECT balance FROM users WHERE id = ?";
        DBUtils.executeQueryAsync(balanceSql, stmt -> {
            try {
                stmt.setInt(1, userId);
            } catch (SQLException e) {
                throw new RuntimeException("Error setting userId", e);
            }
        }, rs -> {
            double balance = 0;
            try {
                if (rs.next()) {
                    balance = rs.getDouble("balance");
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error reading balance", e);
            }
            return balance;
        }, balance -> {
            if (balance < price) {
                System.err.println("MarketplaceController: Insufficient funds, balance: " + balance + ", price: " + price);
                showError("Insufficient funds to purchase " + item.itemName() + ". Need $" + price + ", have $" + balance + ".");
                return;
            }

            // Update buyer balance
            String updateBalanceSql = "UPDATE users SET balance = balance - ? WHERE id = ?";
            DBUtils.executeUpdateAsync(updateBalanceSql, stmt -> {
                try {
                    stmt.setDouble(1, price);
                    stmt.setInt(2, userId);
                } catch (SQLException e) {
                    throw new RuntimeException("Error updating balance", e);
                }
            }, success -> {
                if (!success) {
                    System.err.println("MarketplaceController: Failed to update balance for userId: " + userId);
                    showError("Failed to update balance.");
                    return;
                }

                // Update seller balance
                String updateSellerBalanceSql = "UPDATE users SET balance = balance + ? WHERE id = ?";
                DBUtils.executeUpdateAsync(updateSellerBalanceSql, stmt -> {
                    try {
                        stmt.setDouble(1, price);
                        stmt.setInt(2, item.sellerId());
                    } catch (SQLException e) {
                        throw new RuntimeException("Error updating seller balance", e);
                    }
                }, sellerSuccess -> {
                    if (!sellerSuccess) {
                        System.err.println("MarketplaceController: Failed to update seller balance for sellerId: " + item.sellerId());
                    }
                }, error -> System.err.println("MarketplaceController: Error updating seller balance: " + error.getMessage()));

                // Remove item from marketplace
                String removeItemSql = "DELETE FROM marketplace_items WHERE id = ?";
                DBUtils.executeUpdateAsync(removeItemSql, stmt -> {
                    try {
                        stmt.setInt(1, item.id());
                    } catch (SQLException e) {
                        throw new RuntimeException("Error removing item", e);
                    }
                }, removeSuccess -> {
                    if (!removeSuccess) {
                        System.err.println("MarketplaceController: Failed to remove item: " + item.itemName());
                    }
                }, error -> System.err.println("MarketplaceController: Error removing item: " + error.getMessage()));

                // Add item to inventory
                if (item.itemType().equals("RAW_MATERIAL")) {
                    String insertSql = "INSERT INTO raw_material_inventory (user_id, material_type, quantity, image_url) " +
                            "VALUES (?, ?, 1, ?) ON DUPLICATE KEY UPDATE quantity = quantity + 1";
                    DBUtils.executeUpdateAsync(insertSql, stmt -> {
                        try {
                            stmt.setInt(1, userId);
                            stmt.setString(2, item.itemName());
                            stmt.setString(3, item.imageUrl());
                        } catch (SQLException e) {
                            throw new RuntimeException("Error inserting raw material", e);
                        }
                    }, insertSuccess -> {
                        if (insertSuccess) {
                            System.out.println("MarketplaceController: Purchased raw material: " + item.itemName());
                            logBuyActivity(userId, item.itemName());
                            fetchMarketplaceItems(); // Refresh marketplace
                        } else {
                            System.err.println("MarketplaceController: Failed to add raw material: " + item.itemName());
                            showError("Failed to purchase " + item.itemName() + ".");
                        }
                    }, error -> System.err.println("MarketplaceController: Error purchasing raw material: " + error.getMessage()));
                } else if (item.itemType().equals("TOOL")) {
                    String insertSql = "INSERT INTO crafted_item_inventory (user_id, crafted_item_name) VALUES (?, ?) ON DUPLICATE KEY UPDATE quantity = quantity + 1";
                    DBUtils.executeUpdateAsync(insertSql, stmt -> {
                        try {
                            stmt.setInt(1, userId);
                            stmt.setString(2, item.itemName());
                        } catch (SQLException e) {
                            throw new RuntimeException("Error inserting tool", e);
                        }
                    }, insertSuccess -> {
                        if (insertSuccess) {
                            System.out.println("MarketplaceController: Purchased tool: " + item.itemName());
                            logBuyActivity(userId, item.itemName());
                            fetchMarketplaceItems(); // Refresh marketplace
                        } else {
                            System.err.println("MarketplaceController: Failed to add tool: " + item.itemName());
                            showError("Failed to purchase " + item.itemName() + ".");
                        }
                    }, error -> System.err.println("MarketplaceController: Error purchasing tool: " + error.getMessage()));
                }
            }, error -> System.err.println("MarketplaceController: Error updating balance: " + error.getMessage()));
        }, error -> System.err.println("MarketplaceController: Error fetching balance: " + error.getMessage()));
    }

    private void logBuyActivity(int userId, String itemName) {
        System.out.println("MarketplaceController: Logging buy activity for item: " + itemName);
        String sql = "INSERT INTO user_activities (user_id, action_type, item_name, amount, timestamp) " +
                "VALUES (?, 'BUY', ?, 1, NOW())";
        DBUtils.executeUpdateAsync(sql, stmt -> {
            try {
                stmt.setInt(1, userId);
                stmt.setString(2, itemName);
            } catch (SQLException e) {
                throw new RuntimeException("Error logging activity", e);
            }
        }, success -> {
            if (success) {
                System.out.println("MarketplaceController: Logged buy activity for " + itemName);
            } else {
                System.err.println("MarketplaceController: Failed to log buy activity for " + itemName);
            }
        }, error -> System.err.println("MarketplaceController: Error logging buy activity: " + error.getMessage()));
    }

    private void showError(String message) {
        System.out.println("MarketplaceController: Error: " + message);
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText("Purchase Error");
        alert.showAndWait();
    }

    @FXML
    private void filterAll() {
        applyFilter("ALL");
    }

    @FXML
    private void filterRawMaterials() {
        applyFilter("RAW_MATERIALS");
    }

    @FXML
    private void filterTools() {
        applyFilter("TOOLS");
    }

    @FXML
    private void navigateToRawMaterials() {
        System.out.println("MarketplaceController: Navigating to raw materials page");
        FXUtils.navigateTo("rawMaterials", marketplacePage);
    }

    @FXML
    private void navigateToCrafting() {
        System.out.println("MarketplaceController: Navigating to crafting page");
        FXUtils.navigateTo("crafting", marketplacePage);
    }
}