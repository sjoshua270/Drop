package com.rethink.drop.models;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.FirebaseDatabase;

import static com.rethink.drop.tools.StringUtilities.parseHashTags;

public class Comment {
    public static final String COMMENT_KEY = "comment_key";
    private String commenterID;
    private String text;
    private long timeStamp;
    private boolean edited;

    public Comment(String commenterID, String text, long timeStamp, boolean edited) {
        this.commenterID = commenterID;
        this.text = text;
        this.timeStamp = timeStamp;
        this.edited = edited;

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

    public boolean isEdited() {
        return edited;
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
        ref.child(commentKey)
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
