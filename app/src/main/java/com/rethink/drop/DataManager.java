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
import com.rethink.drop.adapters.PostsAdapter;
import com.rethink.drop.models.Post;

import java.util.ArrayList;
import java.util.HashMap;

public class DataManager {

    public static ArrayList<String> keys;
    private static Double scanRadius;
    private static DataListener dataListener;
    private static GeoQueryListener geoQueryListener;
    private final HashMap<String, DatabaseReference> refs;
    private final PostsAdapter postsAdapter;
    private GeoQuery geoQuery;

    public DataManager(PostsAdapter postsAdapter) {
        scanRadius = 10.0;
        keys = new ArrayList<>();
        dataListener = new DataListener();
        geoQueryListener = new GeoQueryListener();
        refs = new HashMap<>();
        this.postsAdapter = postsAdapter;
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
        postsAdapter.notifyItemRemoved(keys.indexOf(key));
        keys.remove(key);
    }

    public ArrayList<String> getKeys() {
        return keys;
    }

    public void setKeys(ArrayList<String> keys) {
        DataManager.keys = keys;
    }

    private class GeoQueryListener
            implements GeoQueryEventListener {
        @Override
        public void onKeyEntered(String key, GeoLocation location) {
            DatabaseReference ref = FirebaseDatabase.getInstance()
                                                    .getReference()
                                                    .child("posts")
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
                if (keys.indexOf(key) < 0) {
                    keys.add(key);
                    postsAdapter.notifyItemInserted(keys.indexOf(key));
                } else {
                    postsAdapter.notifyItemChanged(keys.indexOf(key));
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
