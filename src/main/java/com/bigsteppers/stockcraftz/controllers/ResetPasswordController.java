package com.bigsteppers.stockcraftz.controllers;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.bigsteppers.stockcraftz.utils.FXUtils;
import com.dbfx.database.DBUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;

public class ResetPasswordController {
    @FXML
    private Node resetPasswordPage;
    @FXML
    private TextField usernameField;
    @FXML
    private TextField oldPasswordField;
    @FXML
    private TextField newPasswordField;

    @FXML
    private void handleResetPassword() {
        String username = usernameField.getText().trim();
        String oldPassword = oldPasswordField.getText().trim();
        String newPassword = newPasswordField.getText().trim();

        if (username.isEmpty() || oldPassword.isEmpty() || newPassword.isEmpty()) {
            FXUtils.showError("All fields (username, old password, new password) are required", "Reset Password");
            return;
        }

        DBUtils.executeQueryAsync(
                "SELECT password FROM users WHERE username = ?",
                stmt -> stmt.setString(1, username),
                rs -> {
                    if (rs.next()) {
                        String hashedPassword = rs.getString("password");
                        return BCrypt.verifyer().verify(oldPassword.toCharArray(), hashedPassword).verified;
                    }
                    return false;
                },
                isVerified -> {
                    if (!isVerified) {
                        FXUtils.showError("Username or old password is incorrect", "Reset Password");
                        return;
                    }

                    String newHashedPassword = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray());
                    DBUtils.executeUpdateAsync(
                            "UPDATE users SET password = ? WHERE username = ?",
                            stmt -> {
                                try {
                                    stmt.setString(1, newHashedPassword);
                                    stmt.setString(2, username);
                                } catch (Exception e) {
                                    throw new RuntimeException("Error setting parameters", e);
                                }
                            },
                            success -> {
                                if (success) {
                                    FXUtils.showSuccess("Password reset successfully. Please log in with your new password.", "Reset Password");
                                    FXUtils.navigateTo("login", resetPasswordPage);
                                } else {
                                    FXUtils.showError("Failed to update password. Please try again.", "Reset Password");
                                }
                            },
                            error -> FXUtils.showError("Error updating password: " + error.getMessage(), "Reset Password")
                    );
                },
                error -> FXUtils.showError("Error checking username or password. Please try again later.", "Reset Password")
        );
    }

    @FXML
    private void handleBackToLogin() {
        FXUtils.navigateTo("login", resetPasswordPage);
    }
}