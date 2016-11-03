package com.rethink.drop.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rethink.drop.DataManager;
import com.rethink.drop.R;
import com.rethink.drop.adapters.ListingsAdapter;

import java.util.ArrayList;
import java.util.List;

import static com.rethink.drop.MainActivity.degreesPerMile;


public class LocalFragment
        extends Fragment {
    private ListingsAdapter listingsAdapter;
    private DataManager dataManager;
    private List<DatabaseReference> databaseReferences;

    public static LocalFragment newInstance() {

        Bundle args = new Bundle();

        LocalFragment fragment = new LocalFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        listingsAdapter = new ListingsAdapter();
        dataManager = new DataManager(listingsAdapter);
        databaseReferences = new ArrayList<>();
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup
            container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_local_listings,
                container, false);
        RecyclerView listingsRecycler = (RecyclerView) v.findViewById(R.id
                .recycler_local_listings);
        listingsRecycler.setLayoutManager(
                new LinearLayoutManager(
                        getContext(),
                        LinearLayoutManager.VERTICAL,
                        false));
        listingsRecycler.setAdapter(listingsAdapter);
        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_local, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    public void detachListeners(DatabaseReference databaseReference) {
        dataManager.detachListeners(databaseReference);
    }

    public void attachListeners(DatabaseReference databaseReference) {
        dataManager.attachListeners(databaseReference);
    }

    public void reset() {
        dataManager.reset();
    }

    public void updateDBRef(LatLng userLocation) {
        try {
            for (DatabaseReference databaseReference : databaseReferences) {
                detachListeners(databaseReference);
            }
            databaseReferences.clear();
            reset();
            for (int i = -2; i <= 2; i += 1) {
                for (int j = -2; j <= 2; j += 1) {
                    databaseReferences.add(FirebaseDatabase.getInstance()
                                                           .getReference()
                                                           .child("listings")
                                                           .child(String.valueOf((int) (userLocation.latitude / degreesPerMile) + i))
                                                           .child(String.valueOf((int) (userLocation.longitude / degreesPerMile) + j))
                    );
                }
            }
            for (DatabaseReference databaseReference : databaseReferences) {
                attachListeners(databaseReference);
            }
        } catch (ClassCastException ignored) {
        }
    }

    public void stopUpdating() {
        for (DatabaseReference databaseReference : databaseReferences) {
            detachListeners(databaseReference);
        }
    }

    @Override
    public void onPause() {
        stopUpdating();
        super.onPause();
    }
}
