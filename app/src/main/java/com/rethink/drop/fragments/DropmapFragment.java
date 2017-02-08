package com.rethink.drop.fragments;

import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rethink.drop.models.Drop;

import static com.rethink.drop.DataManager.keyLocations;
import static com.rethink.drop.DataManager.keys;


public class DropMapFragment extends SupportMapFragment {
    public GoogleMap googleMap;
    private LatLng userLocation;

    public static DropMapFragment newInstance(Double lat, Double lng) {
        Bundle args = new Bundle();
        args.putDouble("LAT",
                       lat);
        args.putDouble("LNG",
                       lng);
        DropMapFragment fragment = new DropMapFragment();
        fragment.setArguments(args);
        return fragment;
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
    }

    private void placeMarkers() {
        googleMap.addMarker(new MarkerOptions().position(userLocation)
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
                googleMap.addMarker(new MarkerOptions().position(keyLocations.get(key))
                                                       .title(drop.getText()));
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    }
}
