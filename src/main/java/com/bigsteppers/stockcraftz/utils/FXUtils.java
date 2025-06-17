package com.bigsteppers.stockcraftz.utils;

import com.bigsteppers.stockcraftz.StockCraftzApplication;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class FXUtils {

    public static void navigateTo(String pageName, Node node) {
        StockCraftzApplication app = (StockCraftzApplication) node.getScene().getRoot().getUserData();
        app.showPage(pageName);
    }

    public static void showSuccess(String message, String headerText) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText(headerText);
        alert.showAndWait();
    }

    public static void showError(String message, String headerText) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(headerText);
        alert.showAndWait();
    }

    public static boolean showConfirmation(String message, String headerText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText(headerText);
        return alert.showAndWait().filter(response -> response == ButtonType.OK).isPresent();
    }
}