package com.rethink.drop.managers;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private DatabaseReference postsReference;
    private HashMap<String, ValueEventListener> dropListeners;

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
        postsReference = FirebaseDatabase.getInstance()
                                         .getReference()
                                         .child("posts");
        dropListeners = new HashMap<>();
        fetchUserInfo();
    }

    /**
     * A simple get method using the Drop key
     *
     * @param dropKey Key that corresponds to the desired Drop object
     * @return Drop object
     */
    public static Drop getDrop(String dropKey) {
        if (drops.containsKey(dropKey)) {
            return drops.get(dropKey);
        }
        return null;
    }

    /**
     * A simple get method using the Profile key
     *
     * @param profileKey Key that corresponds to the desired Profile object
     * @return Profile object
     */
    public static Profile getProfile(String profileKey) {
        return profiles.get(profileKey);
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

    private void fetchUserInfo() {
        final FirebaseUser user = FirebaseAuth.getInstance()
                                              .getCurrentUser();
        if (user == null) {
            MainActivity.getInstance()
                        .login();
        } else {
            Profile.getRef(user.getUid())
                   .addValueEventListener(new ValueEventListener() {
                       @Override
                       public void onDataChange(DataSnapshot dataSnapshot) {
                           profiles.put(user.getUid(),
                                        dataSnapshot.getValue(Profile.class));
                       }

                       @Override
                       public void onCancelled(DatabaseError databaseError) {

                       }
                   });
        }
    }

    /**
     * Update the center of our scanning radius
     *
     * @param geoLocation new location to scan
     */
    public void updateLocation(GeoLocation geoLocation) {
        if (geoQuery == null) {
            geoQuery = new GeoFire(geoFireRef).queryAtLocation(geoLocation,
                                                               scanRadius);
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
            for (String dropKey : keys) {
                postsReference.child(dropKey)
                              .removeEventListener(dropListeners.get(dropKey));
            }
        }
    }

    /**
     * Gets an existing listener or returns a new listener if it was not saved yet
     *
     * @param dropKey The key to retrieve a listener for
     * @return ValueEventListener for the desired Drop
     */
    private ValueEventListener getListener(String dropKey) {
        if (dropListeners.get(dropKey) != null) {
            return dropListeners.get(dropKey);
        }

        return postsReference.child(dropKey)
                             .addValueEventListener(new DropListener());
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
                dropListeners.put(dropKey,
                                  getListener(dropKey));
                // Inform MainActivity that we have a new Drop
                MainActivity.getInstance()
                            .notifyDropInserted(dropKey);
            }
        }

        @Override
        public void onKeyExited(String key) {
            drops.remove(key);
            dropLocations.remove(key);
            dropListeners.remove(key);
            MainActivity.getInstance()
                        .notifyDropRemoved(key);
            keys.remove(key);
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

    /**
     * Listens for data in a Drop to change on Firebase
     * When data changes, the Drop is added to our list of Drops and the MainActivity
     * is notified of the drop being changed.
     * <p>
     * If we haven't already updated the profile associated with the Drop, we begin
     * that process as well
     */
    private class DropListener implements ValueEventListener {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Add our Drop to the drops set
            final String dropKey = dataSnapshot.getKey();
            Drop drop = dataSnapshot.getValue(Drop.class);
            if (drop != null) {
                drops.put(dropKey,
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
                                    .addListenerForSingleValueEvent(new ProfileListener(dropKey));
                }
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    }

    /**
     * This one listens for changes on the Profile side of things. When a profile changes,
     * it is added to our local list of Profiles and the Drop which called on the update
     * is updated in the UI via MainActivity
     */
    private class ProfileListener implements ValueEventListener {
        String dropKey;

        private ProfileListener(String dropKey) {
            this.dropKey = dropKey;
        }

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
    }
}
