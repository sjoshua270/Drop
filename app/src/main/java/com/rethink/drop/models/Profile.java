package com.rethink.drop.models;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.FirebaseDatabase;

public class Profile {
    public static final String PROFILE_KEY = "profile_key";
    private String imageURL;
    private String thumbnailURL;
    private String name;

    public Profile(String imageURL, String thumbnailURL, String name) {
        this.imageURL = imageURL;
        this.thumbnailURL = thumbnailURL;
        this.name = name;
    }

    public Profile() {
    }

    @Exclude
    public static DatabaseReference getRef(String profileKey) {
        return FirebaseDatabase.getInstance()
                               .getReference()
                               .child("profiles")
                               .child(profileKey);
    }

    @Exclude
    public static void addFriend(String profileKey, String friendKey, Profile friendProfile) {
        getFriendsRef(profileKey).child(friendKey)
                                 .setValue(friendProfile);
    }

    @Exclude
    public static DatabaseReference getFriendsRef(String profileKey) {
        return FirebaseDatabase.getInstance()
                               .getReference()
                               .child("profiles")
                               .child(profileKey)
                               .child("friends");
    }

    @Exclude
    public void save(String profileKey) {
        FirebaseDatabase.getInstance()
                        .getReference()
                        .child("profiles")
                        .child(profileKey)
                        .setValue(this);
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
