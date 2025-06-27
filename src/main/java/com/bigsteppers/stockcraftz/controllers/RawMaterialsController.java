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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

import static com.bigsteppers.stockcraftz.StockCraftzApplication.RESOURCES_PATH;

public class RawMaterialsController implements LoadablePage {

    private static final int ITEMS_PER_PAGE = 8; // 4x2 grid
    private static final String REMOVE_ACTION = "REMOVE";

    @FXML
    private Node rawMaterialsPage;
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
        navbarController.setActivePage("rawMaterials");
        navbarController.rootNode = rawMaterialsPage;
    }

    @Override
    public void onLoad() {
        if (!SessionManager.isLoggedIn()) {
            FXUtils.navigateTo("login", rawMaterialsPage);
            return;
        }
        fetchMaterials();
    }

    private void fetchMaterials() {
        int userId = SessionManager.getCurrentUser().id();
        String sql = "SELECT id, material_type, quantity FROM raw_material_inventory WHERE user_id = ?";

        DBUtils.executeQueryAsync(sql,
                stmt -> stmt.setInt(1, userId),
                rs -> {
                    List<RawMaterial> materials = new ArrayList<>();
                    while (rs.next()) {
                        materials.add(new RawMaterial(
                                rs.getInt("id"),
                                rs.getInt("quantity"),
                                MaterialType.valueOf(rs.getString("material_type"))
                        ));
                    }
                    return materials;
                },
                materials -> {
                    allMaterials = materials;
                    filteredMaterials = new ArrayList<>(materials);
                    totalPages = (int) Math.ceil((double) filteredMaterials.size() / ITEMS_PER_PAGE);
                    updateInventoryGrid();
                    updatePagination();
                },
                error -> FXUtils.showError("Failed to load materials", "Database Error")
        );
    }

    private void updateInventoryGrid() {
        inventoryGrid.getChildren().clear();
        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredMaterials.size());

        int row = 0, col = 0;
        for (int i = startIndex; i < endIndex; i++) {
            RawMaterial material = filteredMaterials.get(i);
            inventoryGrid.add(createMaterialBox(material), col, row);

            col++;
            if (col > 3) {
                col = 0;
                row++;
            }
        }
    }

    private VBox createMaterialBox(RawMaterial material) {
        VBox itemBox = new VBox();
        itemBox.getStyleClass().add("inventory-item");
        itemBox.setSpacing(5);

        ImageView icon = createMaterialIcon(material);
        Label nameLabel = new Label(material.getMaterialType().name());
        nameLabel.getStyleClass().add("item-name");

        Label quantityLabel = new Label("x" + material.getQuantity());
        quantityLabel.getStyleClass().add("item-quantity");

        VBox actionsBox = createActionBox(material);
        itemBox.getChildren().addAll(icon, nameLabel, quantityLabel, actionsBox);

        itemBox.setOnMouseEntered(e -> actionsBox.setVisible(true));
        itemBox.setOnMouseExited(e -> actionsBox.setVisible(false));

        return itemBox;
    }

    private ImageView createMaterialIcon(RawMaterial material) {
        ImageView icon = new ImageView();
        icon.getStyleClass().add("item-icon");
        icon.setFitWidth(64);
        icon.setFitHeight(64);

        String imagePath = RESOURCES_PATH + "/raw_materials/" +
                Formater.toSnakeCase(material.getMaterialType().name()) + ".png";
        java.io.InputStream is = getClass().getResourceAsStream(imagePath);
        if (is != null) {
            icon.setImage(new Image(is));
        }
        return icon;
    }

    private VBox createActionBox(RawMaterial material) {
        VBox actionsBox = new VBox();
        actionsBox.getStyleClass().add("item-actions");
        actionsBox.setVisible(false);
        actionsBox.setSpacing(5);

        Button deleteButton = new Button("REMOVE");
        deleteButton.getStyleClass().addAll("btn", "small", "danger");
        deleteButton.setOnAction(e -> handleDeleteMaterial(material));

        actionsBox.getChildren().add(deleteButton);
        return actionsBox;
    }

    private void updatePagination() {
        PaginationUtils.updatePagination(paginationBox, currentPage, totalPages, page -> {
            currentPage = page;
            updateInventoryGrid();
        });
    }

    private void handleDeleteMaterial(RawMaterial material) {
        String materialType = material.getMaterialType().name();
        boolean confirmed = FXUtils.showConfirmation(
                "Remove 1 unit of " + materialType + " (x" + material.getQuantity() + ")?",
                "Confirm Removal"
        );

        if (!confirmed) return;

        if (material.getQuantity() > 1) {
            reduceMaterialQuantity(material);
        } else {
            deleteMaterial(material);
        }
    }

    private void reduceMaterialQuantity(RawMaterial material) {
        String updateSql = "UPDATE raw_material_inventory SET quantity = quantity - 1 WHERE id = ? AND user_id = ? RETURNING quantity";

        DBUtils.executeQueryAsync(updateSql,
                stmt -> {
                    stmt.setInt(1, material.getId());
                    stmt.setInt(2, SessionManager.getCurrentUser().id());
                },
                rs -> rs.next() ? rs.getInt("quantity") : -1,
                newQuantity -> {
                    if (newQuantity >= 0) {
                        logRemoveActivity(material.getMaterialType().name(), 1);
                        FXUtils.showSuccess("1 unit removed successfully", "Success");
                        fetchMaterials();
                    } else {
                        FXUtils.showError("Failed to reduce quantity", "Error");
                    }
                },
                error -> FXUtils.showError("Error reducing quantity", "Error")
        );
    }

    private void deleteMaterial(RawMaterial material) {
        String deleteSql = "DELETE FROM raw_material_inventory WHERE id = ? AND user_id = ? RETURNING id";

        DBUtils.executeQueryAsync(deleteSql,
                stmt -> {
                    stmt.setInt(1, material.getId());
                    stmt.setInt(2, SessionManager.getCurrentUser().id());
                },
                rs -> rs.next() ? rs.getInt("id") : -1,
                deletedId -> {
                    if (deletedId > 0) {
                        logRemoveActivity(material.getMaterialType().name(), 1);
                        FXUtils.showSuccess("Material removed successfully", "Success");
                        fetchMaterials();
                    } else {
                        FXUtils.showError("Failed to delete material", "Error");
                    }
                },
                error -> FXUtils.showError("Error deleting material", "Error")
        );
    }

    private void logRemoveActivity(String itemName, int amount) {
        String sql = "INSERT INTO user_activities (user_id, action_type, item_name, amount, timestamp) " +
                "VALUES (?, ?, ?, ?, NOW())";

        DBUtils.executeUpdateAsync(sql,
                stmt -> {
                    stmt.setInt(1, SessionManager.getCurrentUser().id());
                    stmt.setString(2, REMOVE_ACTION);
                    stmt.setString(3, itemName);
                    stmt.setInt(4, amount);
                },
                success -> {
                },
                error -> {
                }
        );
    }

    @FXML
    private void navigateToCrafting() {
        FXUtils.navigateTo("crafting", rawMaterialsPage);
    }

    @FXML
    private void navigateToMarketplace() {
        FXUtils.navigateTo("marketplace", rawMaterialsPage);
    }
}