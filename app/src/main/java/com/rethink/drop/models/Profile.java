package com.rethink.drop.models;

import com.google.firebase.database.Exclude;

public class Profile {
    public static final String PROFILE_KEY = "profile_key";
    private String userID;
    private String imageURL;
    private String thumbnailURL;
    private String name;

    public Profile(String userID, String imageURL, String thumbnailURL, String name) {
        this.userID = userID;
        this.imageURL = imageURL;
        this.thumbnailURL = thumbnailURL;
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

    public String getThumbnailURL() {
        return thumbnailURL;
    }

    public String getName() {
        return name;
    }
}
