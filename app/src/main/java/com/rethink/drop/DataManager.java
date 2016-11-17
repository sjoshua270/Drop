package com.rethink.drop;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.rethink.drop.adapters.DropAdapter;

import java.util.ArrayList;

public class DataManager {

    public static ArrayList<String> keys;
    private static Double scanRadius;
    private static GeoQueryListener geoQueryListener;
    private final DropAdapter dropAdapter;
    private GeoQuery geoQuery;

    public DataManager(DropAdapter dropAdapter) {
        scanRadius = 10.0;
        keys = new ArrayList<>();
        geoQueryListener = new GeoQueryListener();
        this.dropAdapter = dropAdapter;
    }

    public void updateLocation(GeoLocation geoLocation) {
        if (geoQuery == null) {
            geoQuery = new GeoFire(FirebaseDatabase.getInstance()
                                                   .getReference()
                                                   .child("geoFire")
            ).queryAtLocation(geoLocation, scanRadius);
        } else {
            geoQuery.setCenter(geoLocation);
        }
    }

    public void attachListeners() {
        if (geoQuery != null) {
            geoQuery.addGeoQueryEventListener(geoQueryListener);
        }
    }

    public void detachListeners() {
        geoQuery.removeAllListeners();
    }

    private void addKey(String key) {
        if (keys.indexOf(key) < 0) {
            keys.add(key);
            dropAdapter.notifyItemInserted(keys.indexOf(key));
        }
    }

    private void removeKey(String key) {
        dropAdapter.notifyItemRemoved(keys.indexOf(key));
        keys.remove(key);
    }

    private class GeoQueryListener
            implements GeoQueryEventListener {
        @Override
        public void onKeyEntered(String key, GeoLocation location) {
            addKey(key);
        }

        @Override
        public void onKeyExited(String key) {
            removeKey(key);
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
}
