package com.rethink.drop.models;

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
}
