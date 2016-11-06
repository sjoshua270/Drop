package com.rethink.drop.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
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

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.UploadTask;
import com.rethink.drop.MainActivity;
import com.rethink.drop.R;
import com.rethink.drop.Utilities;
import com.rethink.drop.interfaces.ImageHandler;
import com.rethink.drop.models.Post;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;

import static com.rethink.drop.MainActivity.userLocation;
import static com.rethink.drop.models.Post.KEY;

public class ListingFragment
        extends Fragment
        implements OnMapReadyCallback,
                   ImageHandler {

    private static final String IMAGE = "image";
    private Bitmap image;
    private ImageView imageView;
    private MapView mapView;
    private TextView title;
    private TextView desc;
    private TextInputEditText inputTitle;
    private TextInputEditText inputDesc;
    private Boolean imageChanged;
    private CoordinatorLayout cLayout;
    private DatabaseReference ref;
    private FirebaseUser user;
    private GoogleMap googleMap;
    private Boolean editing;
    private String imageURL;

    public static ListingFragment newInstance(String key) {
        Bundle args = new Bundle();
        args.putString(KEY, key);
        ListingFragment fragment = new ListingFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static ListingFragment newInstance(String key, Bitmap image) throws IOException {
        Bundle args = new Bundle();
        args.putString(KEY, key);
        if (image != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            args.putByteArray(IMAGE, byteArray);
            stream.close();
        }
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
                .child("posts");
        imageChanged = false;
        if (args != null) {
            byte[] imageBytes = args.getByteArray(IMAGE);
            if (imageBytes != null) {
                image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            }
            String key = args.getString(KEY);
            editing = key == null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        String key = getArguments().getString(KEY);
        if (key != null) {
            getPostData(key);
        }

        // TODO: Merge in this code
//            double distance;
//            if (useMetric(Locale.getDefault())) {
//                distance = distanceInKilometers(
//                        MainActivity.userLocation.latitude, post.getLatitude(),
//                        MainActivity.userLocation.longitude, post.getLongitude()
//                );
//            } else {
//                distance = distanceInMiles(
//                        MainActivity.userLocation.latitude, post.getLatitude(),
//                        MainActivity.userLocation.longitude, post.getLongitude()
//                );
//            }
//            distance = Math.round(distance * 100);
//            distance /= 100;
//            String formatString = getDistanceString(Locale.getDefault());
//            holder.dist.setText(String.format(formatString, String.valueOf(distance)));
//
//            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
//            holder.timeStampDay.setText(sdf.format(post.getTimestamp()));
//
//            sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
//            holder.timeStampTime.setText(sdf.format(post.getTimestamp()));
        View fragmentView = inflater.inflate(R.layout.fragment_listing, container, false);
        cLayout = (CoordinatorLayout) getActivity().findViewById(R.id.coordinator);

        imageView = (ImageView) fragmentView.findViewById(R.id.listing_image);
        ViewCompat.setTransitionName(imageView, "image");
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

    private void getPostData(String key) {

        FirebaseDatabase.getInstance()
                        .getReference()
                        .child("posts")
                        .child(key).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Post post = dataSnapshot.getValue(Post.class);
                if (post != null) {
                    imageURL = post.getImageURL() == null ? "" : post.getImageURL();
                    if (!imageURL.equals("")) {
                        Picasso.with(getContext())
                               .load(imageURL)
                               .placeholder(new BitmapDrawable(getResources(), image))
                               .resize(getContext().getResources()
                                                   .getDimensionPixelSize(R.dimen.listing_image_dimen),
                                       getContext().getResources()
                                                   .getDimensionPixelSize(R.dimen.listing_image_dimen))
                               .centerCrop()
                               .into(imageView);
                    } else if (!editing) {
                        imageView.setVisibility(View.GONE);
                    } else {
                        imageView.setVisibility(View.VISIBLE);
                    }
                    title.setText(post.getTitle());
                    desc.setText(post.getDescription());
                    inputTitle.setText(post.getTitle());
                    inputDesc.setText(post.getDescription());
                    setMap(post);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        getActivity().startActivityForResult(Intent.createChooser(intent, "Select Picture"), MainActivity.GALLERY_REQUEST);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_edit, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void setImageView() {
        if (image != null) {
            imageView.setImageBitmap(Utilities.squareImage(image));
            imageView.setPadding(0, 0, 0, 0);
        }
    }

    private void prepViews() {
        if (editing) {
            title.setVisibility(View.GONE);
            desc.setVisibility(View.GONE);
            inputTitle.setVisibility(View.VISIBLE);
            inputDesc.setVisibility(View.VISIBLE);
        } else {
            inputTitle.setVisibility(View.GONE);
            inputDesc.setVisibility(View.GONE);
            title.setVisibility(View.VISIBLE);
            desc.setVisibility(View.VISIBLE);
        }
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
    private void publishListing() {
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
            if (key == null) {
                key = ref.push()
                         .getKey();
            }
            ref = ref.child(key);
            if (image != null && imageChanged) {
                uploadImage(key, filename);
            } else {
                saveListing(key, new Post(
                        user.getUid(),
                        Calendar.getInstance()
                                .getTimeInMillis(),
                        imageURL,
                        inputTitle.getText()
                                  .toString(),
                        inputDesc.getText()
                                 .toString(),
                        userLocation.latitude,
                        userLocation.longitude));
                toggleState();
            }
        }
    }

    private void uploadImage(final String key, String filename) {
        Utilities.uploadImage(
                getActivity(),
                image,
                user.getUid() + "/" + key + "/" + filename
        ).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                if (downloadUrl != null) {
                    saveListing(key, new Post(
                            user.getUid(),
                            Calendar.getInstance().getTimeInMillis(),
                            downloadUrl.toString(),
                            inputTitle.getText().toString(),
                            inputDesc.getText().toString(),
                            userLocation.latitude,
                            userLocation.longitude));
                    toggleState();
                } else {
                    Snackbar.make(cLayout, R.string.unexpected_error, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void saveListing(String key, Post post) {
        new GeoFire(
                FirebaseDatabase.getInstance()
                                .getReference()
                                .child("geoFire"))
                .setLocation(
                        key,
                        new GeoLocation(
                                userLocation.latitude,
                                userLocation.longitude));
        ref.setValue(post);
        Bundle args = getArguments();
        args.putString(KEY, key);
        ((MainActivity) getActivity()).dismissKeyboard();
        getPostData(key);
    }

    private void toggleState() {
        if (editing) {
            imageChanged = false;
        }
        editing = !editing;
        prepViews();
        ((MainActivity) getActivity()).syncUI();
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
    }

    private void setMap(final Post post) {
        if (googleMap != null) {
            if (editing) {
                googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
                        userLocation = latLng;
                        updateMapPin(post);
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
            updateMapPin(post);
        }
    }

    public void updateMapPin() {
        updateMapPin(userLocation, "You are here");
    }

    public void updateMapPin(Post post) {
        updateMapPin(post.getLatLng(), post.getTitle());
    }

    private void updateMapPin(LatLng location, String title) {
        googleMap.clear();
        googleMap.addMarker(new MarkerOptions()
                .position(location)
                .title(title));
        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder()
                              .target(location)
                              .zoom(20f)
                              .build()));

    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
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
