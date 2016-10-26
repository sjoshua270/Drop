package com.rethink.drop;

import android.graphics.Bitmap;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

public class Utilities {
    public static boolean useMetric(Locale locale) {
        String countryCode = locale.getCountry();
        switch (countryCode) {
            case "US":
                return false; // USA
            case "LR":
                return false; // liberia
            case "MM":
                return false; // burma
            default:
                return true;
        }
    }

    public static String getDistanceString(Locale locale) {
        String imperial = "%1$s mi";
        String metric = "%1$s km";
        if (useMetric(locale)) {
            return metric;
        } else {
            return imperial;
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
    private static double distance(double lat1, double lat2, double lon1,
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

        //noinspection SuspiciousNameCombination
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

    public static Bitmap scaleDown(Bitmap realImage, float maxImageSize, boolean filter) {
        float ratio = Math.min(
                maxImageSize / realImage.getWidth(),
                maxImageSize / realImage.getHeight());
        int width, height;
        if (ratio < 1) {
            width = Math.round(ratio * realImage.getWidth());
            height = Math.round(ratio * realImage.getHeight());
        } else {
            width = realImage.getWidth();
            height = realImage.getHeight();
        }
        return Bitmap.createScaledBitmap(realImage, width, height, filter);
    }

    public static Bitmap generateIcon(Bitmap original) {
        int imageWidth = original.getWidth();
        int imageHeight = original.getHeight();

        int imageStartX = 0;
        int imageStartY = 0;
        // Calculate starting X or Y
        if (imageWidth > imageHeight) {
            imageStartX = (imageWidth - imageHeight) / 2;
        } else {
            imageStartY = (imageHeight - imageWidth) / 2;
        }
        // Get minimum dimension for squaring
        int imageMinDimen = Math.min(imageHeight, imageWidth);
        // Crop image to square
        original = Bitmap.createBitmap(original, imageStartX, imageStartY, imageMinDimen, imageMinDimen);
        // Scale image down
        return Bitmap.createScaledBitmap(original, 256, 256, false);
    }

    public static UploadTask uploadImage(Bitmap bitmap, String path) {
        StorageReference iconReference = FirebaseStorage.getInstance()
                                                        .getReferenceFromUrl("gs://drop-143619.appspot.com")
                                                        .child(path);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return iconReference.putBytes(stream.toByteArray());
    }
}
