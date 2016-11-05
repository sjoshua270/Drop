package com.rethink.drop;

import android.graphics.Bitmap;
import android.util.Log;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rethink.drop.adapters.ListingsAdapter;
import com.rethink.drop.models.Listing;

import java.util.ArrayList;
import java.util.HashMap;

public class DataManager {

    public static ArrayList<String> keys;
    public static HashMap<String, Bitmap> imageBitmaps;
    public static HashMap<String, Listing> listings;
    private static DataListener dataListener;
    private static GeoQueryListener geoQueryListener;
    private HashMap<String, DatabaseReference> refs;
    private ListingsAdapter listingsAdapter;
    private GeoQuery geoQuery;

    public DataManager(ListingsAdapter listingsAdapter) {
        keys = new ArrayList<>();
        imageBitmaps = new HashMap<>();
        listings = new HashMap<>();
        dataListener = new DataListener();
        geoQueryListener = new GeoQueryListener();
        refs = new HashMap<>();
        this.listingsAdapter = listingsAdapter;
    }

    public void updateLocation(GeoLocation geoLocation) {
        if (geoQuery == null) {
            geoQuery = new GeoFire(FirebaseDatabase.getInstance()
                                                   .getReference()
                                                   .child("geoFire")
            ).queryAtLocation(geoLocation, 2.0);
        }
        geoQuery.setCenter(geoLocation);
    }

    public void attachListeners() {
        geoQuery.addGeoQueryEventListener(geoQueryListener);
        for (String key : keys) {
            refs.get(key).addValueEventListener(dataListener);
        }
    }

    public void detachListeners() {
        geoQuery.removeAllListeners();
        for (String key : keys) {
            refs.get(key).removeEventListener(dataListener);
        }
    }

    private void removeListing(String key) {
        refs.remove(key);
        listings.remove(key);
        keys.remove(key);
    }

    private class GeoQueryListener
            implements GeoQueryEventListener {
        @Override
        public void onKeyEntered(String key, GeoLocation location) {
            DatabaseReference ref = FirebaseDatabase.getInstance()
                                                    .getReference()
                                                    .child("listings")
                                                    .child(key);
            ref.addValueEventListener(dataListener);
            refs.put(key, ref);
        }

        @Override
        public void onKeyExited(String key) {
            removeListing(key);
        }

        @Override
        public void onKeyMoved(String key, GeoLocation location) {

        }

        @Override
        public void onGeoQueryReady() {

        }

        @Override
        public void onGeoQueryError(DatabaseError error) {

        }

    }

    private class DataListener
            implements ValueEventListener {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            final String key = dataSnapshot.getKey();
            Listing listing = dataSnapshot.getValue(Listing.class);
            if (listing != null) {
                listings.put(key, listing);
                if (keys.indexOf(key) < 0) {
                    keys.add(key);
                    listingsAdapter.notifyItemInserted(keys.indexOf(key));
                } else {
                    listingsAdapter.notifyItemChanged(keys.indexOf(key));
                }
            } else {
                removeListing(key);
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.w("DataManager", "loadPost:onCancelled", databaseError.toException());
        }
    }
}
