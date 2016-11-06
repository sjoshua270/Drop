package com.rethink.drop;

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
import com.rethink.drop.models.Post;

import java.util.ArrayList;
import java.util.HashMap;

import static com.rethink.drop.MainActivity.userLocation;

public class DataManager {

    public static ArrayList<String> keys;
    public static HashMap<String, Post> listings;
    private static Double scanRadius;
    private static DataListener dataListener;
    private static GeoQueryListener geoQueryListener;
    private final HashMap<String, DatabaseReference> refs;
    private final ListingsAdapter listingsAdapter;
    private GeoQuery geoQuery;

    public DataManager(ListingsAdapter listingsAdapter) {
        scanRadius = 10.0;
        keys = new ArrayList<>();
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
            ).queryAtLocation(geoLocation, scanRadius);
        }
        geoQuery.setCenter(geoLocation);
    }

    public void expandRadius() {
        if (scanRadius < 100.0) {
            scanRadius += 10.0;
            geoQuery.setRadius(scanRadius);
        }
    }

    public void attachListeners() {
        if (geoQuery != null) {
            geoQuery.addGeoQueryEventListener(geoQueryListener);
        }
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
        listingsAdapter.notifyItemRemoved(keys.indexOf(key));
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
            Post post = dataSnapshot.getValue(Post.class);
            if (post != null) {
                listings.put(key, post);
                Double distance = post.getDistanceFromUser(userLocation);
                Double distanceToCompare;
                int scanIndex = keys.indexOf(key);
                if (keys.size() > 0) {
                    if (scanIndex < 0) {
                        // While the key hasn't been inserted yet
                        while (keys.indexOf(key) < 0) {
                            scanIndex += 1;
                            if (scanIndex == keys.size()) {
                                keys.add(scanIndex, key);
                            } else {
                                distanceToCompare = listings.get(keys.get(scanIndex))
                                                            .getDistanceFromUser(userLocation);
                                if (distance < distanceToCompare) {
                                    keys.add(scanIndex, key);
                                }
                            }
                        }
                        listingsAdapter.notifyItemInserted(scanIndex);
                    } else {
                        listingsAdapter.notifyItemChanged(scanIndex);
                    }
                } else {
                    keys.add(key);
                    listingsAdapter.notifyItemInserted(keys.indexOf(key));
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
