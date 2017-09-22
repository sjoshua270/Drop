package com.rethink.drop.models;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.rethink.drop.MainActivity;
import com.rethink.drop.managers.DataManager;

import static com.rethink.drop.tools.StringUtilities.parseHashTags;

public class Drop {
    public static final String KEY = "KEY";
    private String userID;
    private long timestamp;
    private String imageURL;
    private String thumbnailURL;
    private String text;

    public Drop(String userID, long timestamp, String imageURL, String thumbnailURL, String text) {
        this.userID = userID;
        this.timestamp = timestamp;
        this.imageURL = imageURL;
        this.thumbnailURL = thumbnailURL;
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

    public String getThumbnailURL() {
       return thumbnailURL;
    }

    public String getText() {
        return text;
    }

    @Exclude
    public String save(String dropKey) {
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
        DataManager.addDrop(dropKey,
                            this);
        return dropKey;
    }

    @Exclude
    public void publish(String dropKey) {
        FirebaseDatabase.getInstance()
                        .getReference()
                        .child("drops_by_profile")
                        .child(getUserID())
                        .child(dropKey)
                        .setValue(this);
    }

    @Exclude
    public void delete(final String dropKey) {
        if (getImageURL() != null && !getImageURL().equals("")) {
            final FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();

            firebaseStorage.getReferenceFromUrl(getImageURL())
                           .delete()
                           .addOnSuccessListener(new OnSuccessListener<Void>() {
                               @Override
                               public void onSuccess(Void aVoid) {
                                   if (getThumbnailURL() != null && !getThumbnailURL().equals("")) {
                                       firebaseStorage.getReferenceFromUrl(getThumbnailURL())
                                                      .delete()
                                                      .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                          @Override
                                                          public void onSuccess(Void aVoid) {
                                                              deleteDropInfo(dropKey);
                                                          }
                                                      })
                                                      .addOnFailureListener(new OnFailureListener() {
                                                          @Override
                                                          public void onFailure(@NonNull Exception e) {
                                                              MainActivity.getInstance()
                                                                          .showMessage("Failed to delete Drop. Please try again later");
                                                              Log.e("Drop.delete_thumbnail",
                                                                    e.getMessage());
                                                          }
                                                      });
                                   }
                               }
                           })
                           .addOnFailureListener(new OnFailureListener() {
                               @Override
                               public void onFailure(@NonNull Exception e) {
                                   MainActivity.getInstance()
                                               .showMessage("Failed to delete Drop. Please try again later");
                                   Log.e("Drop.delete",
                                         e.getMessage());
                               }
                           });
        } else {
            deleteDropInfo(dropKey);
        }

    }

    @Exclude
    private void deleteDropInfo(String dropKey) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                                                .getReference();
        ref.child("posts")
           .child(dropKey)
           .removeValue();
        ref.child("geoFire")
           .child(dropKey)
           .removeValue();
        ref.child("comments")
           .child(dropKey)
           .removeValue();
        ref.child("profiles")
           .child(getUserID())
           .child("posts")
           .child(dropKey)
           .removeValue();
        ref.child("drops_by_profile")
           .child(getUserID())
           .child(dropKey)
           .removeValue();
    }
}
