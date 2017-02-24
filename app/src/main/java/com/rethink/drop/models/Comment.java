package com.rethink.drop.models;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.FirebaseDatabase;

public class Comment {
    private String commenterID;
    private String text;
    private long timeStamp;

    public Comment(String commenterID, String text, long timeStamp) {
        this.commenterID = commenterID;
        this.text = text;
        this.timeStamp = timeStamp;
    }

    public Comment() {

    }

    public String getCommenterID() {
        return commenterID;
    }

    public String getText() {
        return text;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    @Exclude
    public void save(String dropKey, String commentKey) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                                                .getReference()
                                                .child("comments")
                                                .child(dropKey);
        if (commentKey == null) {
            commentKey = ref.push()
                            .getKey();
        }
        ref = ref.child(commentKey);
        ref.setValue(this);
    }
}
