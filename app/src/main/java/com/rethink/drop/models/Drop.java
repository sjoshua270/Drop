package com.rethink.drop.models;

public class Drop {
    public static final String KEY = "KEY";
    private String userID;
    private long timestamp;
    private String imageURL;
    private String title;
    private String description;

    public Drop(String userID, long timestamp, String imageURL,
                String title, String description) {
        this.userID = userID;
        this.timestamp = timestamp;
        this.imageURL = imageURL;
        this.title = title;
        this.description = description;
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
}
