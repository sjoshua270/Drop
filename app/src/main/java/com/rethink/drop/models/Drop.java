package com.rethink.drop.models;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.Exclude;
import com.rethink.drop.Utilities;

public class Drop {
    public static final String KEY = "KEY";
    private String userID;
    private long timestamp;
    private String imageURL;
    private String title;
    private String description;
    private Double latitude;
    private Double longitude;

    public Drop(String userID, long timestamp, String imageURL,
                String title, String description, Double latitude, Double longitude) {
        this.userID = userID;
        this.timestamp = timestamp;
        this.imageURL = imageURL;
        this.title = title;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Drop() {

    }

    public String getUserID() {
        return userID;
    }

    public long getTimestamp() {
        return timestamp;
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

    @Exclude
    public Double getDistanceFromUser(LatLng userLocation) {
        return Utilities.distanceInKilometers(
                userLocation.latitude, getLatitude(),
                userLocation.longitude, getLongitude()
        );
    }
}
