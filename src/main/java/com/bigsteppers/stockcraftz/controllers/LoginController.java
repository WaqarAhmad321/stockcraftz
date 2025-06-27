package com.bigsteppers.stockcraftz.controllers;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.bigsteppers.stockcraftz.model.SessionManager;
import com.bigsteppers.stockcraftz.model.User;
import com.bigsteppers.stockcraftz.utils.FXUtils;
import com.dbfx.database.DBUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML
    private Node loginPage;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            FXUtils.showError("Username and password fields cannot be empty", "Login");
            return;
        }

        String sql = "SELECT * FROM users WHERE username = ?";

        DBUtils.executeQueryAsync(sql, stmt -> {
                    stmt.setString(1, username);
                    System.out.println(username);
                }, rs -> {
                    if (rs.next() && BCrypt.verifyer().verify(password.toCharArray(), rs.getString("password")).verified) {
                        return new User(
                                rs.getInt("id"),
                                rs.getString("username"),
                                rs.getDouble("balance")
                        );
                    }

                    return null;
                }, user -> {
                    if (user != null) {
                        SessionManager.setCurrentUser(user);
                        FXUtils.navigateTo("menu", loginPage);
                        usernameField.clear();
                        passwordField.clear();
                    } else {
                        new Alert(Alert.AlertType.ERROR, "Invalid username or password").showAndWait();
                    }
                }, err -> {
                    new Alert(Alert.AlertType.ERROR, "An error occurred while logging in. Please try again later.").showAndWait();
                }
        );
    }

    @FXML
    private void navigateToRegister() {
        FXUtils.navigateTo("register", loginPage);
    }

    @FXML
    private void navigateToResetPassword() {
        FXUtils.navigateTo("resetPassword", loginPage);
        System.out.println("navigate");
    }
}