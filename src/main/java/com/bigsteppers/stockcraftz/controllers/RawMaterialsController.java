package com.bigsteppers.stockcraftz.controllers;

import com.bigsteppers.stockcraftz.interfaces.LoadablePage;
import com.bigsteppers.stockcraftz.model.MaterialType;
import com.bigsteppers.stockcraftz.model.RawMaterial;
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

import static com.bigsteppers.stockcraftz.StockCraftzApplication.RESOURCES_PATH;

public class RawMaterialsController implements LoadablePage {

    private final int itemsPerPage = 8; // 4x2 grid
    @FXML
    private Node rawMaterialsPage;
    @FXML
    private TextField rawMaterialSearch;
    @FXML
    private GridPane inventoryGrid;
    @FXML
    private HBox paginationBox;
    @FXML
    private NavbarController navbarController;
    private List<RawMaterial> allMaterials = new ArrayList<>();
    private List<RawMaterial> filteredMaterials = new ArrayList<>();
    private int currentPage = 1;
    private int totalPages = 1;

    @FXML
    private void initialize() {
        System.out.println("RawMaterialsController: Initializing...");
        checkFXMLComponents();
        if (navbarController != null) {
            navbarController.setActivePage("rawMaterials");
            navbarController.rootNode = rawMaterialsPage;
        }
        rawMaterialSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("RawMaterialsController: Search query changed to: " + newVal);
            applySearchFilter(newVal);
        });
    }

    private void checkFXMLComponents() {
        if (rawMaterialsPage == null) System.err.println("RawMaterialsController: rawMaterialsPage is null");
        if (rawMaterialSearch == null) System.err.println("RawMaterialsController: rawMaterialSearch is null");
        if (inventoryGrid == null) System.err.println("RawMaterialsController: inventoryGrid is null");
        if (paginationBox == null) System.err.println("RawMaterialsController: paginationBox is null");
        if (navbarController == null) System.err.println("RawMaterialsController: navbarController is null");
    }

    @Override
    public void onLoad() {
        if (!SessionManager.isLoggedIn()) {
            System.err.println("RawMaterialsController: No user logged in, redirecting to login");
            FXUtils.navigateTo("login", rawMaterialsPage);
            return;
        }
        try {
            int userId = SessionManager.getCurrentUser().id();
            System.out.println("RawMaterialsController: User logged in, userId: " + userId);
            fetchMaterials();
        } catch (Exception e) {
            System.err.println("RawMaterialsController: Error accessing user ID: " + e.getMessage());
            e.printStackTrace();
            FXUtils.navigateTo("login", rawMaterialsPage);
        }
    }

    public void fetchMaterials() {
        System.out.println("RawMaterialsController: Fetching materials");
        int userId = SessionManager.getCurrentUser().id();
        String sql = "SELECT id, material_type, quantity FROM raw_material_inventory WHERE user_id = ?";
        DBUtils.executeQueryAsync(sql, stmt -> {
            try {
                stmt.setInt(1, userId);
            } catch (SQLException e) {
                throw new RuntimeException("Error setting userId", e);
            }
        }, rs -> {
            ArrayList<RawMaterial> rawMaterials = new ArrayList<>();
            try {
                while (rs.next()) {
                    rawMaterials.add(new RawMaterial(
                            rs.getInt("id"),
                            rs.getInt("quantity"),
                            MaterialType.valueOf(rs.getString("material_type"))
                    ));
                }
                System.out.println("RawMaterialsController: Fetched " + rawMaterials.size() + " raw materials");
            } catch (SQLException e) {
                throw new RuntimeException("Error reading result set", e);
            }
            return rawMaterials;
        }, materials -> {
            allMaterials = materials;
            filteredMaterials = new ArrayList<>(materials);
            totalPages = (int) Math.ceil((double) filteredMaterials.size() / itemsPerPage);
            System.out.println("RawMaterialsController: Total pages: " + totalPages);
            updateInventoryGrid();
            updatePagination();
        }, error -> System.err.println("RawMaterialsController: Error fetching materials: " + error.getMessage()));
    }

    private void applySearchFilter(String query) {
        System.out.println("RawMaterialsController: Applying search filter: " + query);
        if (query == null || query.trim().isEmpty()) {
            filteredMaterials = new ArrayList<>(allMaterials);
        } else {
            String lowerQuery = query.toLowerCase();
            filteredMaterials = allMaterials.stream()
                    .filter(material -> material.getMaterialType().name().toLowerCase().contains(lowerQuery))
                    .collect(Collectors.toList());
        }
        currentPage = 1;
        totalPages = (int) Math.ceil((double) filteredMaterials.size() / itemsPerPage);
        updateInventoryGrid();
        updatePagination();
    }

    private void updateInventoryGrid() {
        System.out.println("RawMaterialsController: Updating inventory grid, currentPage: " + currentPage);
        if (inventoryGrid == null) {
            System.err.println("RawMaterialsController: inventoryGrid is null");
            return;
        }
        inventoryGrid.getChildren().clear();
        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, filteredMaterials.size());
        int row = 0, col = 0;
        for (int i = startIndex; i < endIndex; i++) {
            RawMaterial material = filteredMaterials.get(i);
            VBox itemBox = new VBox();
            itemBox.getStyleClass().add("inventory-item");
            itemBox.setSpacing(5);

            ImageView icon = new ImageView();
            icon.getStyleClass().add("item-icon");
            icon.setFitWidth(64);
            icon.setFitHeight(64);
            String imagePath = RESOURCES_PATH + "/raw_materials/" + Formater.toSnakeCase(material.getMaterialType().name()) + ".png";
            java.io.InputStream is = getClass().getResourceAsStream(imagePath);
            if (is != null) {
                icon.setImage(new Image(is));
            } else {
                System.err.println("RawMaterialsController: Failed to load image: " + imagePath);
            }

            Label nameLabel = new Label(material.getMaterialType().name());
            nameLabel.getStyleClass().add("item-name");

            Label quantityLabel = new Label("x" + material.getQuantity());
            quantityLabel.getStyleClass().add("item-quantity");

            VBox actionsBox = new VBox();
            actionsBox.getStyleClass().add("item-actions");
            actionsBox.setVisible(false);
            actionsBox.setSpacing(5);

            Button deleteButton = new Button("REMOVE");
            deleteButton.getStyleClass().addAll("btn", "small", "danger");
            deleteButton.setOnAction(e -> handleDeleteMaterial(material));

            actionsBox.getChildren().add(deleteButton);
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
        System.out.println("RawMaterialsController: Inventory grid updated with " + (endIndex - startIndex) + " items");
    }

    private void updatePagination() {
        System.out.println("RawMaterialsController: Updating pagination, currentPage: " + currentPage + ", totalPages: " + totalPages);
        PaginationUtils.updatePagination(paginationBox, currentPage, totalPages, page -> {
            currentPage = page;
            updateInventoryGrid();
            updatePagination();
        });
    }

    private void handleDeleteMaterial(RawMaterial material) {
        System.out.println("RawMaterialsController: Initiating deletion for material: " + material.getMaterialType().name());
        String materialType = material.getMaterialType().name();
        int quantity = material.getQuantity();
        boolean confirmed = FXUtils.showConfirmation("Remove " + materialType + " (x" + quantity + ")?", "Confirm Deletion");
        if (!confirmed) {
            System.out.println("RawMaterialsController: Deletion cancelled for material: " + materialType);
            return;
        }

        int userId = SessionManager.getCurrentUser().id();
        int materialId = material.getId();

        String deleteSql = "DELETE FROM raw_material_inventory WHERE id = ? AND user_id = ? RETURNING id";
        DBUtils.executeQueryAsync(deleteSql, stmt -> {
            try {
                stmt.setInt(1, materialId);
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
            if (deletedId == materialId && deletedId > 0) {
                System.out.println("RawMaterialsController: Material deleted, ID: " + deletedId);
                logRemoveActivity(userId, materialType, quantity);
                FXUtils.showSuccess(materialType + " removed successfully.", "Success");
                fetchMaterials();
            } else {
                System.err.println("RawMaterialsController: Deletion failed, ID: " + materialId);
                FXUtils.showError("Failed to delete " + materialType + ".", "Raw Materials Error");
            }
        }, error -> {
            System.err.println("RawMaterialsController: Error deleting material: " + error.getMessage());
            FXUtils.showError("Error deleting " + materialType + ": " + error.getMessage(), "Raw Materials Error");
        });
    }

    private void logRemoveActivity(int userId, String itemName, int amount) {
        System.out.println("RawMaterialsController: Logging remove activity for item: " + itemName);
        String sql = "INSERT INTO user_activities (user_id, action_type, item_name, amount, timestamp) " +
                "VALUES (?, 'REMOVE', ?, ?, NOW())";
        DBUtils.executeUpdateAsync(sql, stmt -> {
            try {
                stmt.setInt(1, userId);
                stmt.setString(2, itemName);
                stmt.setInt(3, amount);
            } catch (SQLException e) {
                throw new RuntimeException("Error logging activity", e);
            }
        }, success -> {
            if (success) {
                System.out.println("RawMaterialsController: Logged remove activity for " + itemName);
            } else {
                System.err.println("RawMaterialsController: Failed to log remove activity for " + itemName);
            }
        }, error -> System.err.println("RawMaterialsController: Error logging remove activity: " + error.getMessage()));
    }

    @FXML
    private void navigateToCrafting() {
        System.out.println("RawMaterialsController: Navigating to crafting page");
        FXUtils.navigateTo("crafting", rawMaterialsPage);
    }

    @FXML
    private void navigateToMarketplace() {
        System.out.println("RawMaterialsController: Navigating to marketplace page");
        FXUtils.navigateTo("marketplace", rawMaterialsPage);
    }
}