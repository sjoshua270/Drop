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

import com.firebase.geofire.GeoLocation;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.rethink.drop.DataManager;
import com.rethink.drop.R;
import com.rethink.drop.adapters.ListingsAdapter;


public class LocalFragment
        extends Fragment {
    private ListingsAdapter listingsAdapter;
    private DataManager dataManager;

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
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            inflater.inflate(R.menu.menu_local, menu);
            super.onCreateOptionsMenu(menu, inflater);
        }
    }

    public void updateDBRef(LatLng userLocation) {
        dataManager.updateLocation(new GeoLocation(userLocation.latitude, userLocation.longitude));
    }

    @Override
    public void onPause() {
        dataManager.detachListeners();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        dataManager.attachListeners();
    }
}
