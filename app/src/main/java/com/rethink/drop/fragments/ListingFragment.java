package com.rethink.drop.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.UploadTask;
import com.rethink.drop.DataManager;
import com.rethink.drop.MainActivity;
import com.rethink.drop.R;
import com.rethink.drop.Utilities;
import com.rethink.drop.interfaces.ImageHandler;
import com.rethink.drop.models.Listing;

import java.util.Calendar;

import static com.rethink.drop.MainActivity.degreesPerMile;
import static com.rethink.drop.MainActivity.userLocation;
import static com.rethink.drop.models.Listing.KEY;

public class ListingFragment
        extends Fragment
        implements OnMapReadyCallback,
                   ImageHandler {

    protected Bitmap image;
    protected ImageView imageView;
    protected Listing listing;
    protected MapView mapView;
    protected TextView title;
    protected TextView desc;
    protected TextInputEditText inputTitle;
    protected TextInputEditText inputDesc;
    protected View fragmentView;
    protected ViewGroup container;
    private Boolean imageChanged;
    private CoordinatorLayout cLayout;
    private DatabaseReference ref;
    private FirebaseUser user;
    private GoogleMap googleMap;
    private Boolean editing;

    public static ListingFragment newInstance(String key) {
        Bundle args = new Bundle();
        args.putString(KEY, key);
        ListingFragment fragment = new ListingFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        user = FirebaseAuth
                .getInstance()
                .getCurrentUser();
        ref = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("listings");
        imageChanged = false;
        if (args != null) {
            String key = args.getString(KEY);
            listing = DataManager.listings.get(key);
            image = DataManager.imageBitmaps.get(key);
            editing = key == null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        this.container = container;
        fragmentView = inflater.inflate(R.layout.fragment_listing, container, false);
        cLayout = (CoordinatorLayout) getActivity().findViewById(R.id.coordinator);

        imageView = (ImageView) fragmentView.findViewById(R.id.listing_image);
        ViewCompat.setTransitionName(imageView, "image");
        setImageView();
        imageView.setOnClickListener(new ImageClickHandler());

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

        prepViews();

        return fragmentView;
    }

    public void getImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        getActivity().startActivityForResult(Intent.createChooser(intent, "Select Picture"), MainActivity.GALLERY_REQUEST);
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_edit, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    protected void setImageView() {
        if (image != null) {
            imageView.setImageBitmap(Utilities.squareImage(image));
            imageView.setPadding(0, 0, 0, 0);
        }
    }

    public void prepViews() {
        if (editing) {
            title.setVisibility(View.GONE);
            desc.setVisibility(View.GONE);
            inputTitle.setVisibility(View.VISIBLE);
            inputDesc.setVisibility(View.VISIBLE);

            if (image == null) {
                imageView.setImageResource(R.drawable.ic_photo_camera_white_24px);
                float scale = getResources().getDisplayMetrics().density;
                int dpAsPixels = (int) (40 * scale + 0.5f);
                imageView.setPadding(dpAsPixels, dpAsPixels, dpAsPixels, dpAsPixels);
            }
        } else {
            inputTitle.setVisibility(View.GONE);
            inputDesc.setVisibility(View.GONE);
            title.setVisibility(View.VISIBLE);
            desc.setVisibility(View.VISIBLE);
        }
        displayListing(listing);
        setHasOptionsMenu(editing);
    }

    public void handleFabPress() {
        if (editing) {
            publishListing();
        } else {
            toggleState();
        }
    }

    /**
     * Takes all values in the current layout and sends them off to Firebase,
     * then returns to the previous fragment
     */
    public void publishListing() {
        String filename = inputTitle.getText()
                                    .toString()
                                    .replaceAll("[^A-Za-z]+", "")
                                    .toLowerCase();
        if (filename.equals("")) {
            Snackbar.make(cLayout, R.string.title_cannot_be_empty, Snackbar
                    .LENGTH_LONG)
                    .show();
        } else {
            String key = getArguments().getString("KEY");
            /* If the listing exists, and the block Location is different than before,
            remove the original listing from its block. This ensures no leftover listings
            because of editing a listing in a different location than the original posting. */
            if (key != null) {
                int blockNumber = getBlockLocation(userLocation.latitude);
                if (blockNumber != getBlockLocation(listing.getLatitude())) {
                    ref.child(String.valueOf(getBlockLocation(listing.getLatitude())))
                       .child(String.valueOf(getBlockLocation(listing.getLongitude())))
                       .child(key)
                       .removeValue();
                }
            }
            ref = ref.child(String.valueOf(getBlockLocation(userLocation.latitude)))
                     .child(String.valueOf(getBlockLocation(userLocation.longitude)));
            if (key == null) {
                key = ref.push()
                         .getKey();
            }
            ref = ref.child(key);
            if (image != null && imageChanged) {
                uploadImage(key, filename);
            } else {
                listing = new Listing(
                        user.getUid(),
                        Calendar.getInstance()
                                .getTimeInMillis(),
                        listing != null ? listing.getImageURL() : "",
                        inputTitle.getText()
                                  .toString(),
                        inputDesc.getText()
                                 .toString(),
                        userLocation.latitude,
                        userLocation.longitude);
                ref.setValue(listing);
                toggleState();
            }
        }
    }

    private void uploadImage(String key, String filename) {
        Utilities.uploadImage(
                getActivity(),
                image,
                user.getUid() + "/" + key + "/" + filename
        ).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                if (downloadUrl != null) {
                    listing = new Listing(
                            user.getUid(),
                            Calendar.getInstance().getTimeInMillis(),
                            downloadUrl.toString(),
                            inputTitle.getText().toString(),
                            inputDesc.getText().toString(),
                            userLocation.latitude,
                            userLocation.longitude);
                    ref.setValue(listing);
                    toggleState();
                } else {
                    Snackbar.make(cLayout, R.string.unexpected_error, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    public void toggleState() {
        editing = !editing;
        prepViews();
        ((MainActivity) getActivity()).syncUI();
    }

    private int getBlockLocation(double latitude) {
        return (int) (latitude / degreesPerMile);
    }

    @Override
    public void OnImageReceived(Bitmap image) {
        this.image = image;
        imageChanged = true;
        setImageView();
    }

    public boolean isEditing() {
        return editing;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        setMap();
    }

    private void setMap() {
        if (googleMap != null) {
            if (editing) {
                googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
                        userLocation = latLng;
                        updateMapPin();
                    }
                });
            } else {
                googleMap.getUiSettings()
                         .setAllGesturesEnabled(false);
                googleMap.getUiSettings()
                         .setMyLocationButtonEnabled(false);
            }
            googleMap.getUiSettings()
                     .setMapToolbarEnabled(false);
            updateMapPin();
        }
    }

    public void updateMapPin() {
        LatLng location;
        String title = "You are here";
        if (editing) {
            location = userLocation;
        } else {
            location = listing.getLatLng();
            title = listing.getTitle();
        }
        googleMap.clear();
        googleMap.addMarker(new MarkerOptions()
                .position(location)
                .title(title));
        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder()
                              .target(userLocation)
                              .zoom(20f)
                              .build()));
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    public ImageView getImageView() {
        return imageView;
    }

    private class ImageClickHandler
            implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (editing) {
                if (ActivityCompat.checkSelfPermission(
                        getContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    getImage();
                } else {
                    ActivityCompat.requestPermissions(
                            getActivity(),
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            0);
                }
            } else {
                ((MainActivity) getActivity()).viewImage(getArguments().getString(KEY));
            }
        }
    }
}
