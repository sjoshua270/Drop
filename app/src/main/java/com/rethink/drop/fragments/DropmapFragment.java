package com.rethink.drop.fragments;

import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rethink.drop.MainActivity;
import com.rethink.drop.models.Drop;
import com.rethink.drop.tools.Utilities;

import java.util.Calendar;
import java.util.HashMap;

import static com.rethink.drop.DataManager.keyLocations;
import static com.rethink.drop.DataManager.keys;


public class DropMapFragment extends SupportMapFragment {
    private static DropMapFragment instance;
    public GoogleMap googleMap;
    private LatLng userLocation;
    private Marker userMarker;
    private HashMap<String, Marker> markers;

    public static DropMapFragment newInstance(Double lat, Double lng) {
        Bundle args = new Bundle();
        args.putDouble("LAT",
                       lat);
        args.putDouble("LNG",
                       lng);
        DropMapFragment fragment = new DropMapFragment();
        fragment.setArguments(args);
        instance = fragment;
        return fragment;
    }

    public static DropMapFragment getInstance() {
        if (instance != null) {
            return instance;
        }
        return null;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Bundle args = getArguments();
        userLocation = new LatLng(args.getDouble("LAT"),
                                  args.getDouble("LNG"));
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap gMap) {
                googleMap = gMap;
                placeMarkers();
            }
        });
        markers = new HashMap<>();
    }

    public void notifyDropInserted(String key) {
        FirebaseDatabase.getInstance()
                        .getReference()
                        .child("posts")
                        .child(key)
                        .addValueEventListener(new DropListener(key));
    }

    public void removeDrop(String key) {
        markers.get(key)
               .remove();
    }

    public void notifyLocationChanged() {
        if (userMarker != null) {
            Bundle args = getArguments();
            userMarker.setPosition(new LatLng(args.getDouble("LAT"),
                                              args.getDouble("LNG")));
        }
    }

    private void placeMarkers() {
        userMarker = googleMap.addMarker(new MarkerOptions().position(userLocation)
                                                            .title("You are here"));
        googleMap.moveCamera(CameraUpdateFactory.zoomTo(14.0f));
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(userLocation));

        for (int i = 0; i < keys.size(); i++) {
            final String key = keys.get(i);
            FirebaseDatabase.getInstance()
                            .getReference()
                            .child("posts")
                            .child(key)
                            .addValueEventListener(new DropListener(key));
        }
        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                marker.showInfoWindow();
                googleMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
                for (String key : keys) {
                    if (markers.containsKey(key) && markers.get(key)
                                                           .getId()
                                                           .equals(marker.getId())) {
                        MainActivity.scrollToDrop(key);
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private class DropListener implements ValueEventListener {
        String key;

        DropListener(String key) {
            this.key = key;
        }

        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            Drop drop = dataSnapshot.getValue(Drop.class);
            if (drop != null) {
                LatLng dropLocation = keyLocations.get(key);
                float distanceFromUser = (float) Utilities.distanceInKilometers(userLocation.latitude,
                                                                                dropLocation.latitude,
                                                                                userLocation.longitude,
                                                                                dropLocation.longitude);
                long dropAge = Calendar.getInstance()
                                       .getTimeInMillis() - drop.getTimestamp();
                float sec = 1000;
                float min = sec * 60;
                float hr = min * 60;
                float day = hr * 24;
                float fadeThreshold = day * 1;
                float expireThreshold = day * 7;
                float alpha = 1.0f;
                if (dropAge < expireThreshold) {
                    if (dropAge > fadeThreshold) {
                        alpha = 1 - dropAge / expireThreshold;
                        alpha /= distanceFromUser;
                        alpha /= 100;
                    }
                    markers.put(key,
                                googleMap.addMarker(new MarkerOptions().position(keyLocations.get(key))
                                                                       .title(drop.getText())
                                                                       .snippet("Alpha: " + alpha)
                                                                       .alpha(alpha)));
                }
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    }
}
