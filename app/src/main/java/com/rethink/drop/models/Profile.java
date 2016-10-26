package com.rethink.drop.models;

public class Profile {
    private String userID;
    private String iconURL;
    private String imageURL;
    private String name;

    public Profile(String userID, String iconURL, String imageURL, String name) {
        this.userID = userID;
        this.iconURL = iconURL;
        this.imageURL = imageURL;
        this.name = name;
    }

    public Profile() {
    }

    public String getUserID() {
        return userID;
    }

    public String getIconURL() {
        return iconURL;
    }

    public String getImageURL() {
        return imageURL;
    }

    public String getName() {
        return name;
    }
}
