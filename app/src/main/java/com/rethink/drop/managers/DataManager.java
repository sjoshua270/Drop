package com.rethink.drop.managers;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rethink.drop.MainActivity;
import com.rethink.drop.models.Drop;
import com.rethink.drop.models.Profile;

import java.util.ArrayList;
import java.util.HashMap;

public class DataManager {

    public static ArrayList<String> keys; // Determines the order that drops are displayed
    public static HashMap<String, LatLng> dropLocations; // Holds the LatLng for each drop
    public static HashMap<String, Profile> profiles; // Holds Profiles for each loaded drop
    private static HashMap<String, Drop> drops; // These are our drops
    private static Double scanRadius; // The width of our radius for scanning for drops
    private static GeoQueryListener geoQueryListener; // This is the listener that tells us when a drop enters or leaves our radius
    private DatabaseReference geoFireRef; // This is where our GeoQuery can find its data
    private GeoQuery geoQuery; // This is how we access the data in a radius

    public DataManager() {
        scanRadius = 10.0;
        keys = new ArrayList<>();
        drops = new HashMap<>();
        dropLocations = new HashMap<>();
        profiles = new HashMap<>();
        geoQueryListener = new GeoQueryListener();
        geoFireRef = FirebaseDatabase.getInstance()
                                     .getReference()
                                     .child("geoFire");
    }

    /**
     * A simple get method using the Drop key
     *
     * @param dropKey Key that corresponds to the desired Drop object
     * @return Drop object
     */
    public static Drop getDrop(String dropKey) {
        return drops.get(dropKey);
    }

    /**
     * Get the current index of the Key specified
     * This is used to tell adapters where the newest drop was inserted
     *
     * @param key Key for Drop whose index we require
     * @return Integer which indicates the Drop's position in our current order
     */
    public static int getDropIndex(String key) {
        return keys.indexOf(key);
    }

    /**
     * Update the center of our scanning radius
     *
     * @param geoLocation new location to scan
     */
    public void updateLocation(GeoLocation geoLocation) {
        if (geoQuery == null) {
            geoQuery = new GeoFire(geoFireRef).queryAtLocation(geoLocation, scanRadius);
        } else {
            geoQuery.setCenter(geoLocation);
        }
    }

    /**
     * Method called when user resumes app
     */
    public void onResume() {
        if (geoQuery != null) {
            onPause();
            geoQuery.addGeoQueryEventListener(geoQueryListener);
        }
    }

    /**
     * Method called when user leaves app
     */
    public void onPause() {
        if (geoQuery != null) {
            geoQuery.removeAllListeners();
        }
    }

    /**
     * Manages what happens when a Drop enters and exits our scanning radius
     */
    private class GeoQueryListener implements GeoQueryEventListener {
        @Override
        public void onKeyEntered(final String dropKey, GeoLocation location) {
            // If we don't already have the dropKey...
            if (keys.indexOf(dropKey) < 0) {
                // ...put it in the list in chronological order
                for (int i = 0; i < keys.size(); i++) {
                    // Keys are created by Firebase in a chronological fashion, so comparing these
                    // alphabetically works!
                    if (keys.get(i)
                            .compareTo(dropKey) < 0) {
                        keys.add(i,
                                 dropKey);
                        break;
                    }
                }
                if (keys.indexOf(dropKey) < 0) {
                    keys.add(dropKey);
                }
                dropLocations.put(dropKey,
                                  new LatLng(location.latitude,
                                             location.longitude));
                // Retrieve the Drop whose dropKey was just added
                FirebaseDatabase.getInstance()
                                .getReference()
                                .child("posts")
                                .child(dropKey)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        // Add our Drop to the drops set
                                        String key = dataSnapshot.getKey();
                                        Drop drop = dataSnapshot.getValue(Drop.class);
                                        drops.put(key,
                                                  drop);
                                        MainActivity.getInstance()
                                                    .notifyDropChanged(dropKey);
                                        // If we haven't already stored the Profile which created this Drop...
                                        if (!profiles.containsKey(drop.getUserID())) {
                                            // ...then let's add it!
                                            FirebaseDatabase.getInstance()
                                                            .getReference()
                                                            .child("profiles")
                                                            .child(drop.getUserID())
                                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                @Override
                                                                public void onDataChange(DataSnapshot dataSnapshot) {
                                                                    // Add our Profile to the profiles set
                                                                    String key = dataSnapshot.getKey();
                                                                    Profile profile = dataSnapshot.getValue(Profile.class);
                                                                    profiles.put(key,
                                                                                 profile);
                                                                    MainActivity.getInstance()
                                                                                .notifyDropChanged(dropKey);
                                                                }

                                                                @Override
                                                                public void onCancelled(DatabaseError databaseError) {

                                                                }
                                                            });
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                // Inform MainActivity that we have a new Drop
                MainActivity.getInstance()
                            .notifyDropInserted(dropKey);
            }
        }

        @Override
        public void onKeyExited(String key) {
            keys.remove(key);
            dropLocations.remove(key);
            MainActivity.getInstance()
                        .notifyDropRemoved(key);
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
