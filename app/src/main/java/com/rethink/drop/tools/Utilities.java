package com.rethink.drop.tools;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.rethink.drop.R;

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
                                   double lon2) {

        final int R = 6371; // Radius of the earth

        Double latDistance = Math.toRadians(lat2 - lat1);
        Double lonDistance = Math.toRadians(lon2 - lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = 0.0 - 0.0;

        //noinspection SuspiciousNameCombination
        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }

    public static double distanceInMiles(double lat1, double lat2, double lon1,
                                         double lon2) {
        return 0.000621371 * distance(lat1, lat2, lon1, lon2);
    }

    public static double distanceInKilometers(double lat1, double lat2, double lon1,
                                              double lon2) {
        return distance(lat1, lat2, lon1, lon2) / 1000;
    }

    // ===== Image Magic =====

    private static Bitmap scaleDown(Bitmap realImage, float maxImageSize) {
        float ratio = Math.min(
                maxImageSize / realImage.getWidth(),
                maxImageSize / realImage.getHeight());
        if (ratio < 1) {
            int width = Math.round(ratio * realImage.getWidth());
            int height = Math.round(ratio * realImage.getHeight());
            return Bitmap.createScaledBitmap(realImage, width, height, false);
        } else {
            return realImage;
        }
    }

    public static UploadTask uploadImage(Context context, Bitmap bitmap, String path) {
        final ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
        progressDialog.setTitle(R.string.uploading);
        progressDialog.show();

        Bitmap image = scaleDown(bitmap, 1024f);
        Bitmap icon = scaleDown(bitmap, 256f);

        StorageReference imageReference = FirebaseStorage.getInstance()
                                                         .getReferenceFromUrl("gs://drop-143619.appspot.com")
                                                         .child(path);
        final ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 95, imageStream);
        UploadTask uploadImage = imageReference.putBytes(imageStream.toByteArray());
        uploadImage.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                float progress = 100f * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount();
                progressDialog.setProgress(Math.round(progress));
                if (progress == progressDialog.getMax()) {
                    progressDialog.cancel();
                }
            }
        });

        final StorageReference iconReference = FirebaseStorage.getInstance()
                                                              .getReferenceFromUrl("gs://drop-143619.appspot.com")
                                                              .child(path + "_icon");
        final ByteArrayOutputStream iconStream = new ByteArrayOutputStream();
        icon.compress(Bitmap.CompressFormat.JPEG, 70, iconStream);
        UploadTask uploadIcon = iconReference.putBytes(iconStream.toByteArray());
        uploadIcon.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                float progress = 100f * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount();
                progressDialog.setSecondaryProgress(Math.round(progress));
            }
        });

        return uploadImage;
    }

    // ===== End Image Magic =====
}
