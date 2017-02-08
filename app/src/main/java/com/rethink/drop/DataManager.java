package com.rethink.drop;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rethink.drop.adapters.DropAdapter;

import java.util.ArrayList;
import java.util.HashMap;

public class DataManager {

    public static ArrayList<String> keys;
    public static HashMap<String, LatLng> keyLocations;
    private static Double scanRadius;
    private static LocationsListener locationsListener;
    private static GeoQueryListener geoQueryListener;
    private final DropAdapter dropAdapter;
    private GeoQuery geoQuery;
    private DatabaseReference geoFireRef;

    public DataManager(DropAdapter dropAdapter) {
        scanRadius = 10.0;
        keys = new ArrayList<>();
        keyLocations = new HashMap<>();
        locationsListener = new LocationsListener();
        geoQueryListener = new GeoQueryListener();
        this.dropAdapter = dropAdapter;
        geoFireRef = FirebaseDatabase.getInstance()
                                     .getReference()
                                     .child("geoFire");
        geoFireRef.addChildEventListener(locationsListener);
    }

    public void updateLocation(GeoLocation geoLocation) {
        if (geoQuery == null) {
            geoQuery = new GeoFire(geoFireRef).queryAtLocation(geoLocation, scanRadius);
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
        if (geoQuery != null) {
            geoQuery.removeAllListeners();
        }
    }

    public void detachLocationListener() {
        if (geoFireRef != null) {
            geoFireRef.removeEventListener(locationsListener);
        }
    }

    private void addKey(String key, LatLng location) {
        if (keys.indexOf(key) < 0) {
            keys.add(key);
            keyLocations.put(key,
                             location);
            dropAdapter.notifyItemInserted(keys.indexOf(key));
        }
    }

    private void removeKey(String key) {
        int index = keys.indexOf(key);
        keys.remove(key);
        keyLocations.remove(key);
        dropAdapter.notifyItemRemoved(index);
    }

    private class LocationsListener
            implements ChildEventListener {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            removeKey(dataSnapshot.getKey());
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    }

    private class GeoQueryListener
            implements GeoQueryEventListener {
        @Override
        public void onKeyEntered(String key, GeoLocation location) {
            addKey(key,
                   new LatLng(location.latitude,
                              location.longitude));
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
