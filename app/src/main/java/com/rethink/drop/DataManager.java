package com.rethink.drop;

import android.graphics.Bitmap;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.rethink.drop.models.Listing;

import java.util.ArrayList;
import java.util.HashMap;

import static com.rethink.drop.adapters.ListingsAdapter.NOT_DOWNLOADED;
import static com.rethink.drop.adapters.ListingsAdapter.NO_IMAGE;
import static com.rethink.drop.fragments.LocalFragment.listingsAdapter;

public class DataManager {

    public static ArrayList<String> keys;
    public static HashMap<String, Bitmap> imageBitmaps;
    public static HashMap<String, Listing> listings;
    public static HashMap<String, Integer> imageStatus;
    private static DataListener dataListener;
    private static DatabaseReference listingsRef;
    private float degreesPerMile = 0.01449275362f;

    DataManager() {
        keys = new ArrayList<>();
        imageBitmaps = new HashMap<>();
        listings = new HashMap<>();
        imageStatus = new HashMap<>();
        listingsRef = FirebaseDatabase.getInstance()
                                      .getReference()
                                      .child("listings");
        dataListener = new DataListener();
        listingsRef.addChildEventListener(dataListener);
    }

    static void detachListeners() {
        listingsRef.removeEventListener(dataListener);
    }

    private class DataListener
            implements ChildEventListener {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            final String key = dataSnapshot.getKey();
            Listing listing = dataSnapshot.getValue(Listing.class);
            keys.add(key);
            listings.put(key, listing);
            imageStatus.put(key, listing.getImageURL().equals("") ? NO_IMAGE : NOT_DOWNLOADED);
            listingsAdapter.notifyItemInserted(keys.indexOf(key));
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            final String key = dataSnapshot.getKey();
            Listing listing = dataSnapshot.getValue(Listing.class);
            String prevImageURL = listings.get(key).getImageURL();
            if (!prevImageURL.equals(listing.getImageURL())) {
                // Delete previous image to save space
                FirebaseStorage.getInstance().getReferenceFromUrl(prevImageURL).delete();
                imageStatus.put(key, NOT_DOWNLOADED);
            }
            listings.put(key, listing);
            listingsAdapter.notifyItemChanged(keys.indexOf(key));
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            final String key = dataSnapshot.getKey();
            String imageURL = listings.get(key).getImageURL();
            if (imageURL != null && !imageURL.equals("")) {
                FirebaseStorage.getInstance().getReferenceFromUrl(listings.get(key).getImageURL()).delete();
            }
            listings.remove(key);
            imageBitmaps.remove(key);
            listingsAdapter.notifyItemRemoved(keys.indexOf(key));
            keys.remove(key);
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
        }
    }
}
