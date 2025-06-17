package com.bigsteppers.stockcraftz.utils;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class Formater {
    private Formater() {
        throw new IllegalStateException("Utility class");
    }

    public static <T extends Number> String formatWithCommas(T number) {
        if (number == null) {
            return "0";
        }

        DecimalFormat df = new DecimalFormat("#,###");
        df.setRoundingMode(RoundingMode.DOWN);

        return df.format(number);
    }

    public static String toSnakeCase(String text) {
        return text.toLowerCase().replace(" ", "_");
    }
}
