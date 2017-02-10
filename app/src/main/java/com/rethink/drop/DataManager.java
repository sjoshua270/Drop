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

import java.util.ArrayList;
import java.util.HashMap;

public class DataManager {

    public static ArrayList<String> keys;
    public static HashMap<String, LatLng> keyLocations;
    private static Double scanRadius;
    private static LocationsListener locationsListener;
    private static GeoQueryListener geoQueryListener;
    private GeoQuery geoQuery;
    private DatabaseReference geoFireRef;

    DataManager() {
        scanRadius = 10.0;
        keys = new ArrayList<>();
        keyLocations = new HashMap<>();
        locationsListener = new LocationsListener();
        geoQueryListener = new GeoQueryListener();
        geoFireRef = FirebaseDatabase.getInstance()
                                     .getReference()
                                     .child("geoFire");
        geoFireRef.addChildEventListener(locationsListener);
    }

    void updateLocation(GeoLocation geoLocation) {
        if (geoQuery == null) {
            geoQuery = new GeoFire(geoFireRef).queryAtLocation(geoLocation, scanRadius);
        } else {
            geoQuery.setCenter(geoLocation);
        }
    }

    void attachListeners() {
        if (geoQuery != null) {
            detachListeners();
            geoQuery.addGeoQueryEventListener(geoQueryListener);
        }
    }

    void detachListeners() {
        if (geoQuery != null) {
            geoQuery.removeAllListeners();
        }
    }

    void detachLocationListener() {
        if (geoFireRef != null) {
            geoFireRef.removeEventListener(locationsListener);
        }
    }

    private void addKey(String key, LatLng location) {
        if (keys.indexOf(key) < 0) {
            for (int i = 0; i < keys.size(); i++) {
                if (keys.get(i)
                        .compareTo(key) < 0) {
                    keys.add(i,
                             key);
                    break;
                }
            }
            if (keys.indexOf(key) < 0) {
                keys.add(key);
            }
            keyLocations.put(key,
                             location);
            MainActivity.getInstance()
                        .notifyDropAdded(key);
        }
    }

    private void removeKey(String key) {
        int index = keys.indexOf(key);
        keys.remove(key);
        keyLocations.remove(key);
        MainActivity.getInstance()
                    .notifyDropRemoved(key,
                                       index);
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
