package com.bigsteppers.stockcraftz.controllers;

import com.bigsteppers.stockcraftz.model.SessionManager;
import com.bigsteppers.stockcraftz.utils.FXUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public class MenuController {

    @FXML
    private Node menuPage; // Added for navigation

    @FXML
    private void navigateToDashboard() {
        FXUtils.navigateTo("dashboard", menuPage);
    }

    @FXML
    private void navigateToRawMaterials() {
        FXUtils.navigateTo("raw_materials", menuPage);
    }

    @FXML
    private void navigateToCraftedItems() {
        FXUtils.navigateTo("crafted_items", menuPage);
    }

    @FXML
    private void navigateToCrafting() {
        FXUtils.navigateTo("crafting", menuPage);
    }

    @FXML
    private void navigateToLeaderboard() {
        FXUtils.navigateTo("leaderboard", menuPage);
    }

    @FXML
    private void navigateToMarketplace() {
        FXUtils.navigateTo("marketplace", menuPage);
    }

    @FXML
    private void handleLogout() {
        FXUtils.navigateTo("login", menuPage);
        SessionManager.setCurrentUser(null);
    }
}