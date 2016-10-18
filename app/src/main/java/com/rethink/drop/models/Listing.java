package com.rethink.drop.models;

public class Listing {
    public static final String KEY = "KEY";
    private String userID;
    private long timestamp;
    private String iconURL;
    private String imageURL;
    private String title;
    private String description;
    private Double latitude;
    private Double longitude;

    public Listing(String userID, long timestamp, String iconURL, String imageURL,
                   String title, String description, Double latitude, Double longitude) {
        this.userID = userID;
        this.timestamp = timestamp;
        this.iconURL = iconURL;
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

    public String getIconURL() {
        return iconURL;
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

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
}
