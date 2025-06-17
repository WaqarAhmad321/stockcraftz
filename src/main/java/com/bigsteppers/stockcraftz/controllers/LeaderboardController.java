package com.bigsteppers.stockcraftz.controllers;

import com.bigsteppers.stockcraftz.interfaces.LoadablePage;
import com.bigsteppers.stockcraftz.model.SessionManager;
import com.bigsteppers.stockcraftz.utils.PaginationUtils;
import com.dbfx.database.DBUtils;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;

record LeaderboardUser(
        int id,
        String username,
        double balance,
        int craftedItems,
        int rank
) {
}

public class LeaderboardController implements LoadablePage {

    private final int itemsPerPage = 5;
    @FXML
    private Node leaderboardPage;
    @FXML
    private TableView<LeaderboardUser> leaderboardTable;
    @FXML
    private TableColumn<LeaderboardUser, Integer> rankColumn;
    @FXML
    private TableColumn<LeaderboardUser, String> playerColumn;
    @FXML
    private TableColumn<LeaderboardUser, Double> portfolioColumn;
    @FXML
    private TableColumn<LeaderboardUser, Integer> craftedColumn;
    @FXML
    private HBox paginationBox;
    @FXML
    private Label percentileLabel;
    @FXML
    private Label weeklyChangeLabel;
    @FXML
    private Label nextRankLabel;
    @FXML
    private NavbarController navbarController;
    private List<LeaderboardUser> allUsers = new ArrayList<>();
    private int currentPage = 1;
    private int totalPages = 1;

    @FXML
    private void initialize() {
        navbarController.setActivePage("leaderboard");
        navbarController.rootNode = leaderboardPage;

        leaderboardTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        leaderboardTable.setPlaceholder(new Label("No users found"));
        leaderboardTable.setTableMenuButtonVisible(false);
        leaderboardTable.setFocusTraversable(false);
        leaderboardTable.setEditable(false);

        rankColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().rank()).asObject());
        playerColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().username()));
        portfolioColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().balance()).asObject());
        craftedColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().craftedItems()).asObject());
    }

    @Override
    public void onLoad() {
        if (!SessionManager.isLoggedIn()) {
            System.err.println("LeaderboardController: No user logged in, redirecting to login");
            return;
        }

        fetchLeaderboardData();
    }

    private void fetchLeaderboardData() {
        String sql = "SELECT u.id, u.username, u.balance, " +
                "COUNT(c.id) as crafted_items " +
                "FROM users u " +
                "LEFT JOIN crafted_item_inventory c ON u.id = c.user_id " +
                "GROUP BY u.id, u.username, u.balance " +
                "ORDER BY u.balance DESC";

        DBUtils.executeQueryAsync(sql, null,
                rs -> {
                    ArrayList<LeaderboardUser> users = new ArrayList<>();
                    int rank = 1;
                    while (rs.next()) {
                        users.add(new LeaderboardUser(
                                rs.getInt("id"),
                                rs.getString("username"),
                                rs.getDouble("balance"),
                                rs.getInt("crafted_items"),
                                rank++
                        ));
                    }
                    return users;
                },
                users -> {
                    allUsers = users;
                    totalPages = (int) Math.ceil((double) allUsers.size() / itemsPerPage);
                    updateLeaderboardTable();
                    updatePagination();
                    updateUserStats();
                },
                error -> {
                    new Alert(Alert.AlertType.ERROR, "Error fetching leaderboard data: " + error.getMessage()).showAndWait();
                });
    }

    private void updateLeaderboardTable() {
        leaderboardTable.getItems().clear();
        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allUsers.size());

        if (startIndex < endIndex && startIndex < allUsers.size()) {
            List<LeaderboardUser> pageUsers = allUsers.subList(startIndex, endIndex);
            leaderboardTable.getItems().addAll(pageUsers);
        }
        leaderboardTable.refresh();
    }

    private void updatePagination() {
        PaginationUtils.updatePagination(paginationBox, currentPage, totalPages, page -> {
            currentPage = page;
            updateLeaderboardTable();
            updatePagination();
        });
    }

    private void updateUserStats() {
        int currentUserId = SessionManager.getCurrentUser().id();
        LeaderboardUser currentUser = allUsers.stream()
                .filter(u -> u.id() == currentUserId)
                .findFirst()
                .orElse(null);

        if (currentUser == null) {
            return;
        }

        double percentile = ((double) (allUsers.size() - currentUser.rank()) / allUsers.size()) * 100;
        percentileLabel.setText(String.format("%.0f%%", percentile));
        weeklyChangeLabel.setText("+0"); // Placeholder

        if (currentUser.rank() > 1) {
            LeaderboardUser nextUser = allUsers.get(currentUser.rank() - 2);
            int balanceDifference = (int) (nextUser.balance() - currentUser.balance());
            nextRankLabel.setText(String.format("%,d", balanceDifference));
        } else {
            nextRankLabel.setText("TOP");
        }
    }
}