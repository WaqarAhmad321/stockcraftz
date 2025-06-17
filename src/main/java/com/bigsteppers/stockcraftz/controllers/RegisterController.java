package com.bigsteppers.stockcraftz.controllers;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.bigsteppers.stockcraftz.model.SessionManager;
import com.bigsteppers.stockcraftz.model.User;
import com.bigsteppers.stockcraftz.model.UserRole;
import com.bigsteppers.stockcraftz.utils.FXUtils;
import com.dbfx.database.DBUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;

public class RegisterController {
    @FXML
    private Node registerPage;
    @FXML
    private TextField usernameField;
    @FXML
    private TextField passwordField;
    @FXML
    private TextField confirmPasswordField;

    @FXML
    private void handleCreateAccount() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            FXUtils.showError("Username, password, and confirm password fields cannot be empty", "Register");
            return;
        }

        if (!password.equals(confirmPassword)) {
            FXUtils.showError("Passwords do not match", "Register");
            return;
        }

        DBUtils.executeQueryAsync(
                "SELECT COUNT(*) FROM users WHERE username = ?",
                stmt -> stmt.setString(1, username),
                rs -> rs.getInt(1) > 0,
                exists -> {
                    if (exists) {
                        FXUtils.showError("Username already taken. Please choose a different username.", "Register");
                        return;
                    }

                    // If username is available, proceed with inserting user
                    String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());

                    DBUtils.executeQueryAsync(
                            "INSERT INTO users (username, password, role) VALUES (?, ?, ?) RETURNING *",
                            stmt -> {
                                stmt.setString(1, username);
                                stmt.setString(2, hashedPassword);
                                stmt.setString(3, UserRole.CRAFTER.toString());
                            },
                            rs -> {
                                if (rs.next()) {
                                    return new User(
                                            rs.getInt("id"),
                                            rs.getString("username"),
                                            rs.getDouble("balance"),
                                            UserRole.valueOf(rs.getString("role"))
                                    );
                                }
                                return null;
                            },
                            user -> {
                                if (user != null) {
                                    SessionManager.setCurrentUser(user);
                                    FXUtils.navigateTo("menu", registerPage);
                                } else {
                                    FXUtils.showError("An error occurred while creating your account. Please try again later.", "Register");
                                }
                            },
                            error -> FXUtils.showError("Error while inserting user into database", "Register")
                    );
                },
                error -> FXUtils.showError("Error while checking username. Please try again later.", "Register")
        );
    }

    @FXML
    private void handleBackToLogin() {
        FXUtils.navigateTo("login", registerPage);
    }
}