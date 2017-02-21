package com.rethink.drop.models;

import com.google.firebase.database.Exclude;
import com.rethink.drop.tools.Utilities;

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

    private long getTimeStamp() {
        return timeStamp;
    }

    @Exclude
    public String getTimeStampString() {
        return Utilities.getTimeStampString(getTimeStamp());
    }
}
