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
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (!validateInputs(username, password, confirmPassword)) {
            return;
        }

        checkUsernameAvailability(username);
    }

    private boolean validateInputs(String username, String password, String confirmPassword) {
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            FXUtils.showError("All fields are required", "Registration Error");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            FXUtils.showError("Passwords don't match", "Registration Error");
            return false;
        }

        return true;
    }

    private void checkUsernameAvailability(String username) {
        String checkUserSql = "SELECT COUNT(*) FROM users WHERE username = ?";
        DBUtils.executeQueryAsync(
                checkUserSql,
                stmt -> stmt.setString(1, username),
                rs -> rs.next() && rs.getInt(1) > 0,
                usernameExists -> {
                    if (usernameExists) {
                        FXUtils.showError("Username already taken", "Registration Error");
                    } else {
                        registerNewUser(username);
                    }
                },
                error -> FXUtils.showError("Error checking username", "Registration Error")
        );
    }

    private void registerNewUser(String username) {
        String hashedPassword = BCrypt.withDefaults().hashToString(12, passwordField.getText().toCharArray());
        String insertUserSql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?) RETURNING *";

        DBUtils.executeQueryAsync(
                insertUserSql,
                stmt -> {
                    stmt.setString(1, username);
                    stmt.setString(2, hashedPassword);
                    stmt.setString(3, UserRole.CRAFTER.toString());
                }, rs ->
                {
                    if (rs.next()) {
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
                        initializeUserInventory(user.id());
                        FXUtils.navigateTo("menu", registerPage);
                    } else {
                        FXUtils.showError("Failed to create account", "Registration Error");
                    }
                },
                error -> FXUtils.showError("Error creating account", "Registration Error")
        );
    }

    private void initializeUserInventory(int userId) {
        String[] materials = {"APPLE", "ARMOR", "DIAMOND", "GOLD", "IRON", "IRON_SWORD", "MUSHROOM",
                "POTION", "WOOD", "COAL", "FIRE", "CARROT", "EGG", "STONE", "STICK"};
        int[] quantities = {20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20};

        for (int i = 0; i < materials.length; i++) {
            addMaterialToInventory(userId, materials[i], quantities[i]);
        }
    }

    private void addMaterialToInventory(int userId, String materialType, int quantity) {
        String insertMaterialSql = "INSERT INTO raw_material_inventory (user_id, material_type, quantity) " +
                "VALUES (?, ?, ?) ON CONFLICT (user_id, material_type) " +
                "DO UPDATE SET quantity = EXCLUDED.quantity";

        DBUtils.executeUpdateAsync(
                insertMaterialSql,
                stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setString(2, materialType);
                    stmt.setInt(3, quantity);
                },
                success -> {
                },
                error -> System.err.println("Error adding " + materialType + " to inventory: " + error.getMessage())
        );
    }

    @FXML
    private void handleBackToLogin() {
        FXUtils.navigateTo("login", registerPage);
    }
}