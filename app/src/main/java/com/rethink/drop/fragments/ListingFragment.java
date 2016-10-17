package com.rethink.drop.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.rethink.drop.DataManager;
import com.rethink.drop.R;
import com.rethink.drop.models.Listing;

import static com.rethink.drop.models.Listing.KEY;

public class ListingFragment
        extends Fragment
        implements OnMapReadyCallback {

    protected Bitmap imageBitmap;
    protected ImageView imageView;
    protected Listing listing;
    protected MapView mapView;
    protected TextView title;
    protected TextView desc;
    protected TextInputEditText inputTitle;
    protected TextInputEditText inputDesc;
    protected View fragmentView;
    protected ViewGroup container;
    private GoogleMap googleMap;

    public static ListingFragment newInstance(ListingFragment fragment, String key) {
        Bundle args = new Bundle();
        args.putString(KEY, key);
        fragment.setArguments(args);
        return fragment;
    }

    public static ListingFragment newInstance(ListingFragment fragment) {

        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            String key = args.getString(KEY);
            listing = DataManager.listings.get(key);
            imageBitmap = DataManager.imageBitmaps.get(key);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        this.container = container;
        fragmentView = inflater.inflate(R.layout.fragment_listing, container, false);

        imageView = (ImageView) fragmentView.findViewById(R.id.listing_image);
        ViewCompat.setTransitionName(imageView, "image");
        setImageView();

        title = (TextView) fragmentView.findViewById(R.id.listing_title);
        desc = (TextView) fragmentView.findViewById(R.id.listing_desc);
        ViewCompat.setTransitionName(title, "title");
        ViewCompat.setTransitionName(desc, "desc");

        inputTitle = (TextInputEditText) fragmentView.findViewById(R.id.listing_input_title);
        inputDesc = (TextInputEditText) fragmentView.findViewById(R.id.listing_input_desc);
        ViewCompat.setTransitionName(inputTitle, "input_title");
        ViewCompat.setTransitionName(inputDesc, "input_desc");

        mapView = (MapView) fragmentView.findViewById(R.id.listing_map);
        mapView.onCreate(null);
        mapView.onResume();
        mapView.getMapAsync(this);

        displayListing(listing);

        return fragmentView;
    }

    public void displayListing(Listing listing) {
        this.listing = listing;
        if (listing != null) {
            title.setText(listing.getTitle());
            desc.setText(listing.getDescription());
            inputTitle.setText(listing.getTitle());
            inputDesc.setText(listing.getDescription());
            setMap();
        }
    }

    protected void setImageView() {
        if (imageBitmap != null) {
            imageView.setImageBitmap(imageBitmap);
            imageView.setPadding(0, 0, 0, 0);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        setMap();
    }

    private void setMap() {
        if (googleMap != null) {
            LatLng location = new LatLng(0.0, 0.0);
            String title = "You are here";
            if (listing != null) {
                location = new LatLng(
                        listing.getLatitude(),
                        listing.getLongitude());
                title = listing.getTitle();
            }
            googleMap.getUiSettings()
                     .setMapToolbarEnabled(false);
            googleMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title(title));
            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                    CameraPosition.builder()
                                  .target(location)
                                  .zoom(18f)
                                  .build()
            ));
        }
    }
}
