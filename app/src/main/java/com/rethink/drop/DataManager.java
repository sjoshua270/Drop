package com.rethink.drop;

import android.graphics.Bitmap;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
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

    DataManager() {
        keys = new ArrayList<>();
        imageBitmaps = new HashMap<>();
        listings = new HashMap<>();
        imageStatus = new HashMap<>();
        dataListener = new DataListener();
    }

    void attachListeners(DatabaseReference listingsRef) {
        listingsRef.addChildEventListener(dataListener);
    }

    void detachListeners(DatabaseReference listingsRef) {
        listingsRef.removeEventListener(dataListener);
    }

    void reset() {
        keys = new ArrayList<>();
        imageBitmaps = new HashMap<>();
        listings = new HashMap<>();
        imageStatus = new HashMap<>();
        listingsAdapter.notifyDataSetChanged();
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
            if (!prevImageURL.equals("") && !prevImageURL.equals(listing.getImageURL())) {
                // Delete previous image to save space
                FirebaseStorage.getInstance().getReferenceFromUrl(prevImageURL).delete();
                FirebaseStorage.getInstance().getReferenceFromUrl(prevImageURL + "_icon").delete();
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
                FirebaseStorage.getInstance().getReferenceFromUrl(imageURL).delete();
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
