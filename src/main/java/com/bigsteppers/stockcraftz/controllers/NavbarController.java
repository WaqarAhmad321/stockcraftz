package com.bigsteppers.stockcraftz.controllers;

import com.bigsteppers.stockcraftz.utils.FXUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;

public class NavbarController {

    @FXML
    public Node rootNode; // For navigation

    @FXML
    private Button menuButton;

    @FXML
    private Button dashboardButton;

    @FXML
    private Button rawMaterialsButton;

    @FXML
    private Button craftedItemsButton;

    @FXML
    private Button craftingButton;

    @FXML
    private Button leaderboardButton;

    @FXML
    private Button marketplaceButton;

    // Method to set the active button based on the current page
    public void setActivePage(String pageName) {
        // Reset all buttons to default style
        menuButton.getStyleClass().remove("active");
        dashboardButton.getStyleClass().remove("active");
        rawMaterialsButton.getStyleClass().remove("active");
        craftedItemsButton.getStyleClass().remove("active");
        craftingButton.getStyleClass().remove("active");
        leaderboardButton.getStyleClass().remove("active");
        marketplaceButton.getStyleClass().remove("active");

        // Set active style for the current page
        switch (pageName) {
            case "menu":
                menuButton.getStyleClass().add("active");
                break;
            case "dashboard":
                dashboardButton.getStyleClass().add("active");
                break;
            case "rawMaterials":
                rawMaterialsButton.getStyleClass().add("active");
                break;
            case "craftedItems":
                craftedItemsButton.getStyleClass().add("active");
                break;
            case "crafting":
                craftingButton.getStyleClass().add("active");
                break;
            case "leaderboard":
                leaderboardButton.getStyleClass().add("active");
                break;
            case "marketplace":
                marketplaceButton.getStyleClass().add("active");
                break;
        }
    }

    @FXML
    private void initialize() {
        if (rootNode == null) {
            System.err.println("Error: rootNode is not initialized in NavbarController");
        } else {
            System.out.println("NavbarController initialized with rootNode: " + rootNode);
        }
    }

    @FXML
    private void navigateToMenu() {
        FXUtils.navigateTo("menu", rootNode);
    }

    @FXML
    private void navigateToDashboard() {
        FXUtils.navigateTo("dashboard", rootNode);
    }

    @FXML
    private void navigateToRawMaterials() {
        FXUtils.navigateTo("raw_materials", rootNode);
    }

    @FXML
    private void navigateToCraftedItems() {
        FXUtils.navigateTo("crafted_items", rootNode);
    }

    @FXML
    private void navigateToCrafting() {
        FXUtils.navigateTo("crafting", rootNode);
    }

    @FXML
    private void navigateToLeaderboard() {
        FXUtils.navigateTo("leaderboard", rootNode);
    }

    @FXML
    private void navigateToMarketplace() {
        FXUtils.navigateTo("marketplace", rootNode);
    }
}