package com.bigsteppers.stockcraftz.utils;

import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import java.util.function.Consumer;

public class PaginationUtils {

    public static void updatePagination(HBox paginationBox, int currentPage, int totalPages, Consumer<Integer> pageChangeHandler) {
        System.out.println("PaginationUtils: Updating pagination, currentPage: " + currentPage + ", totalPages: " + totalPages);
        paginationBox.getChildren().clear();

        // Previous button
        Button prevButton = new Button("<");
        prevButton.getStyleClass().add("pagination-btn");
        prevButton.setDisable(currentPage == 1);
        prevButton.setOnAction(e -> pageChangeHandler.accept(currentPage - 1));
        paginationBox.getChildren().add(prevButton);

        // Page number buttons
        for (int i = 1; i <= totalPages; i++) {
            Button pageButton = new Button(String.valueOf(i));
            pageButton.getStyleClass().add("pagination-btn");
            if (i == currentPage) {
                pageButton.getStyleClass().add("active");
            }
            final int page = i;
            pageButton.setOnAction(e -> pageChangeHandler.accept(page));
            paginationBox.getChildren().add(pageButton);
        }

        // Next button
        Button nextButton = new Button(">");
        nextButton.getStyleClass().add("pagination-btn");
        nextButton.setDisable(currentPage == totalPages);
        nextButton.setOnAction(e -> pageChangeHandler.accept(currentPage + 1));
        paginationBox.getChildren().add(nextButton);

        System.out.println("PaginationUtils: Pagination updated");
    }
}