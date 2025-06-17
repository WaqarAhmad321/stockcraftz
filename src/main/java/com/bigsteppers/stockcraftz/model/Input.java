package com.bigsteppers.stockcraftz.model;

import java.util.Scanner;

public class Input {
    private final static Scanner scanner = new Scanner(System.in);

    public static int integerInput() {
        while (true) {
            try {
                int integer = Integer.parseInt(scanner.nextLine());
                return integer;
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid numeric.");
                System.out.print("Re-Enter: ");
            }
        }
    }
}
