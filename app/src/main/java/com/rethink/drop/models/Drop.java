package com.rethink.drop.models;

public class Drop {
    public static final String KEY = "KEY";
    private String userID;
    private long timestamp;
    private String imageURL;
    private String text;

    public Drop(String userID, long timestamp, String imageURL, String text) {
        this.userID = userID;
        this.timestamp = timestamp;
        this.imageURL = imageURL;
        this.text = text;
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

    public String getText() {
        return text;
    }
}
