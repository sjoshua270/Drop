package com.rethink.drop.models;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.FirebaseDatabase;

import static com.rethink.drop.tools.StringUtilities.parseHashTags;

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

    @Exclude
    public void save(String dropKey) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                                                .getReference()
                                                .child("posts");
        if (dropKey == null) {
            dropKey = ref.push()
                         .getKey();
        }
        ref.child(dropKey)
           .setValue(this);

        ref = FirebaseDatabase.getInstance()
                              .getReference();
        for (String hashTag : parseHashTags(text)) {
            ref.child("hashtags")
               .child(hashTag)
               .child(dropKey)
               .setValue(dropKey);
            ref.child("posts")
               .child(dropKey)
               .child("hashtags")
               .push()
               .setValue(hashTag);
        }
    }
}
