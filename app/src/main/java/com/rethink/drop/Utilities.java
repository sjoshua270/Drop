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

    /**
     * Calculate distance between two points in latitude and longitude taking
     * into account height difference. If you are not interested in height
     * difference pass 0.0. Uses Haversine method as its base.
     * <p>
     * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
     * el2 End altitude in meters
     *
     * @return Distance in Meters
     **/
    public static double distance(double lat1, double lat2, double lon1,
                                  double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        Double latDistance = Math.toRadians(lat2 - lat1);
        Double lonDistance = Math.toRadians(lon2 - lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }

    public static double distanceInMiles(double lat1, double lat2, double lon1,
                                         double lon2, double el1, double el2) {
        return 0.000621371 * distance(lat1, lat2, lon1, lon2, el1, el2);
    }

    public static double distanceInKilometers(double lat1, double lat2, double lon1,
                                              double lon2, double el1, double el2) {
        return distance(lat1, lat2, lon1, lon2, el1, el2) / 1000;
    }
}
