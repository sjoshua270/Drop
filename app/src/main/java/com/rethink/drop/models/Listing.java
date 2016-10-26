package com.rethink.drop.models;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.Exclude;

public class Listing {
    public static final String KEY = "KEY";
    private String userID;
    private long timestamp;
    private String imageURL;
    private String title;
    private String description;
    private Double latitude;
    private Double longitude;

    public Listing(String userID, long timestamp, String imageURL,
                   String title, String description, Double latitude, Double longitude) {
        this.userID = userID;
        this.timestamp = timestamp;
        this.imageURL = imageURL;
        this.title = title;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Listing() {

    }

    public String getUserID() {
        return userID;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Exclude
    public String getIconURL() {
        return imageURL + "_icon";
    }

    public String getImageURL() {
        return imageURL;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    @Exclude
    public LatLng getLatLng() {
        return new LatLng(latitude, longitude);
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
}
