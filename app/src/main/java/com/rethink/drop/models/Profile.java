package com.rethink.drop.models;

import com.google.firebase.database.Exclude;

public class Profile {
    private String userID;
    private String imageURL;
    private String name;

    public Profile(String userID, String imageURL, String name) {
        this.userID = userID;
        this.imageURL = imageURL;
        this.name = name;
    }

    public Profile() {
    }

    public String getUserID() {
        return userID;
    }

    @Exclude
    public String getIconURL() {
        return imageURL + "_icon";
    }

    public String getImageURL() {
        return imageURL;
    }

    public String getName() {
        return name;
    }
}
