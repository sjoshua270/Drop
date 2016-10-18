package com.rethink.drop;

import java.util.Locale;

public class Utilities {
    public static String getDistanceString(Locale locale) {
        String countryCode = locale.getCountry();
        String imperial = "%1$s mi";
        String metric = "%1$s km";
        switch (countryCode) {
            case "US":
                return imperial; // USA
            case "LR":
                return imperial; // liberia
            case "MM":
                return imperial; // burma
            default:
                return metric;
        }
    }
}
