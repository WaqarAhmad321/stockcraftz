package com.bigsteppers.stockcraftz.controllers;

import com.bigsteppers.stockcraftz.interfaces.LoadablePage;
import com.bigsteppers.stockcraftz.model.SessionManager;
import com.bigsteppers.stockcraftz.utils.FXUtils;
import com.bigsteppers.stockcraftz.utils.PaginationUtils;
import com.dbfx.database.DBUtils;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

record Activity(
        int id,
        String actionType,
        String itemName,
        int amount,
        LocalDateTime timestamp
) {
}

public class DashboardController implements LoadablePage {
    private final int itemsPerPage = 10;

    @FXML
    private Node dashboardPage;
    @FXML
    private Label balanceLabel;
    @FXML
    private Label numRawMaterialsLabel;
    @FXML
    private Label numCraftedItemsLabel;
    @FXML
    private Label rankLabel;
    @FXML
    private TableView<Activity> activityTable;
    @FXML
    private TableColumn<Activity, String> actionColumn;
    @FXML
    private TableColumn<Activity, String> itemColumn;
    @FXML
    private TableColumn<Activity, Integer> amountColumn;
    @FXML
    private TableColumn<Activity, String> timeColumn;
    @FXML
    private HBox paginationBox;
    @FXML
    private NavbarController navbarController;
    private List<Activity> allActivities = new ArrayList<>();
    private int currentPage = 1;
    private int totalPages = 1;

    @FXML
    private void initialize() {
        navbarController.setActivePage("dashboard");
        navbarController.rootNode = dashboardPage;
    }

    @Override
    public void onLoad() {
        if (!SessionManager.isLoggedIn()) {
            return;
        }

        int userId = SessionManager.getCurrentUser().id();
        setupTableColumns();
        fetchUserStats(userId);
        fetchActivities(userId);
    }

    private void setupTableColumns() {
        actionColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().actionType()));
        itemColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().itemName()));
        amountColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().amount()).asObject());
        timeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().timestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
    }

    private void fetchUserStats(int userId) {
        // Fetch balance
        String balanceSql = "SELECT balance FROM users WHERE id = ?";
        DBUtils.executeQueryAsync(balanceSql, stmt -> stmt.setInt(1, userId),
                rs -> rs.next() ? rs.getDouble("balance") : 0,
                balance -> balanceLabel.setText(String.format("%.2f", balance)),
                error -> {
                });

        // Fetch raw materials count
        String rawMaterialsSql = "SELECT SUM(quantity) as total FROM raw_material_inventory WHERE user_id = ?";
        DBUtils.executeQueryAsync(rawMaterialsSql, stmt -> stmt.setInt(1, userId),
                rs -> rs.next() ? rs.getInt("total") : 0,
                count -> numRawMaterialsLabel.setText(String.valueOf(count)),
                error -> {
                });

        // Fetch crafted items count
        String craftedItemsSql = "SELECT COUNT(*) as total FROM crafted_item_inventory WHERE user_id = ?";
        DBUtils.executeQueryAsync(craftedItemsSql, stmt -> stmt.setInt(1, userId),
                rs -> rs.next() ? rs.getInt("total") : 0,
                count -> numCraftedItemsLabel.setText(String.valueOf(count)),
                error -> {
                });

        // Fetch rank
        String rankSql = "SELECT (SELECT COUNT(*) + 1 FROM users u2 WHERE u2.xp > u.xp) as rank FROM users u WHERE u.id = ?";
        DBUtils.executeQueryAsync(rankSql, stmt -> stmt.setInt(1, userId),
                rs -> rs.next() ? rs.getInt("rank") : 0,
                rank -> rankLabel.setText("#" + rank),
                error -> {
                });
    }

    private void fetchActivities(int userId) {
        String sql = "SELECT id, action_type, item_name, amount, timestamp " +
                "FROM user_activities WHERE user_id = ? ORDER BY timestamp DESC";
        DBUtils.executeQueryAsync(sql, stmt -> stmt.setInt(1, userId),
                rs -> {
                    ArrayList<Activity> activities = new ArrayList<>();
                    while (rs.next()) {
                        activities.add(new Activity(
                                rs.getInt("id"),
                                rs.getString("action_type"),
                                rs.getString("item_name"),
                                rs.getInt("amount"),
                                rs.getTimestamp("timestamp").toLocalDateTime()
                        ));
                    }
                    return activities;
                },
                activities -> {
                    allActivities = activities;
                    totalPages = (int) Math.ceil((double) allActivities.size() / itemsPerPage);
                    updateActivityTable();
                    updatePagination();
                },
                error -> {
                });
    }

    private void updateActivityTable() {
        activityTable.setPlaceholder(new Label("No activity found"));
        activityTable.setPrefHeight(350);
        activityTable.getItems().clear();
        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allActivities.size());
        if (startIndex < endIndex && startIndex < allActivities.size()) {
            List<Activity> pageActivities = allActivities.subList(startIndex, endIndex);
            activityTable.getItems().addAll(pageActivities);
        }
        activityTable.refresh();
    }

    private void updatePagination() {
        PaginationUtils.updatePagination(paginationBox, currentPage, totalPages, page -> {
            currentPage = page;
            updateActivityTable();
            updatePagination();
        });
    }

    @FXML
    private void navigateToRawMaterials() {
        FXUtils.navigateTo("rawMaterials", dashboardPage);
    }

    @FXML
    private void navigateToMarketplace() {
        FXUtils.navigateTo("marketplace", dashboardPage);
    }

    @FXML
    private void navigateToCrafting() {
        FXUtils.navigateTo("crafting", dashboardPage);
    }
}