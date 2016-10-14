package com.rethink.drop.models;

public class Listing {
    public static final String KEY = "KEY";
    public static final String USER = "USER";
    public static final String TIME = "TIME";
    public static final String IMAGE = "IMAGE";
    public static final String TITLE = "TITLE";
    public static final String DESC = "DESC";
    public static final String LAT = "LAT";
    public static final String LNG = "LNG";
    public static final String BYTES = "BYTES";
    private String userID;
    private long timestamp;
    private String imageURL;
    private String title;
    private String description;
    private Double latitude;
    private Double longitude;

    public Listing(String userID, long timestamp, String imageURL, String title, String
            description, Double latitude, Double longitude) {
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
