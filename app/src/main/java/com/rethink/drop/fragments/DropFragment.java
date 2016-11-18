package com.rethink.drop.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.UploadTask;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.rethink.drop.MainActivity;
import com.rethink.drop.R;
import com.rethink.drop.Utilities;
import com.rethink.drop.interfaces.ImageHandler;
import com.rethink.drop.models.Drop;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;

import static com.rethink.drop.MainActivity.EDITING;
import static com.rethink.drop.MainActivity.userLocation;
import static com.rethink.drop.models.Drop.KEY;

public class DropFragment
        extends Fragment
        implements ImageHandler {

    private static final String IMAGE = "image";
    private Bitmap image;
    private Context context;
    private ImageView imageView;
    private ViewSwitcher descSwitcher;
    private TextView desc;
    private TextInputEditText inputDesc;
    private Boolean imageChanged;
    private CoordinatorLayout cLayout;
    private DatabaseReference ref;
    private FirebaseUser user;
    private Boolean editing;
    private String imageURL;
    private DatabaseReference postRef;

    public static DropFragment newInstance(String key) {
        Bundle args = new Bundle();
        args.putString(KEY, key);
        DropFragment fragment = new DropFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static DropFragment newInstance(String key, Bitmap image) throws IOException {
        Bundle args = new Bundle();
        args.putString(KEY, key);
        if (image != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            args.putByteArray(IMAGE, byteArray);
            stream.close();
        }
        DropFragment fragment = new DropFragment();
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
            if (key != null) {
                getPostData(key);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

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
        View fragmentView = inflater.inflate(R.layout.fragment_drop, container, false);
        cLayout = (CoordinatorLayout) getActivity().findViewById(R.id.coordinator);

        imageView = (ImageView) fragmentView.findViewById(R.id.listing_image);
        ViewCompat.setTransitionName(imageView, "image");
        imageView.setOnClickListener(new ImageClickHandler());

        descSwitcher = (ViewSwitcher) fragmentView.findViewById(R.id.description_switcher);

        desc = (TextView) fragmentView.findViewById(R.id.listing_desc);
        ViewCompat.setTransitionName(desc, "desc");

        inputDesc = (TextInputEditText) fragmentView.findViewById(R.id.listing_input_desc);
        ViewCompat.setTransitionName(inputDesc, "input_desc");

        prepViews();

        return fragmentView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        String key = getArguments().getString(KEY);
        if (key != null) {
            getPostData(key);
        }
    }

    private void getPostData(String key) {
        if (postRef == null) {
            postRef = FirebaseDatabase.getInstance()
                                      .getReference()
                                      .child("posts")
                                      .child(key);
            postRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Drop drop = dataSnapshot.getValue(Drop.class);
                    if (drop != null) {
                        imageURL = drop.getImageURL() == null ? "" : drop.getImageURL();
                        if (!imageURL.equals("")) {
                            ImageLoader.getInstance().displayImage(imageURL, imageView);
                        } else if (!editing) {
                            imageView.setVisibility(View.GONE);
                        } else {
                            imageView.setVisibility(View.VISIBLE);
                        }
                        desc.setText(drop.getText());
                        inputDesc.setText(drop.getText());
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
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
            if (descSwitcher.getNextView().getClass().equals(TextInputLayout.class)) {
                descSwitcher.showNext();
            }
        } else {
            if (descSwitcher.getNextView().getClass().equals(AppCompatTextView.class)) {
                descSwitcher.showNext();
            }
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
        String key = getArguments().getString("KEY");
        if (key == null) {
            key = ref.push()
                     .getKey();
        }
        ref = ref.child(key);
        if (image != null && imageChanged) {
            uploadImage(key);
        } else {
            saveListing(key, new Drop(
                    user.getUid(),
                    Calendar.getInstance()
                            .getTimeInMillis(),
                    imageURL,
                    inputDesc.getText()
                             .toString()));
            toggleState();
        }
    }

    private void uploadImage(final String key) {
        Utilities.uploadImage(
                getActivity(),
                image,
                user.getUid() + "/" + key
        ).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                if (downloadUrl != null) {
                    saveListing(key, new Drop(
                            user.getUid(),
                            Calendar.getInstance().getTimeInMillis(),
                            downloadUrl.toString(),
                            inputDesc.getText().toString()));
                    toggleState();
                } else {
                    Snackbar.make(cLayout, R.string.unexpected_error, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void saveListing(String key, Drop drop) {
        new GeoFire(
                FirebaseDatabase.getInstance()
                                .getReference()
                                .child("geoFire"))
                .setLocation(
                        key,
                        new GeoLocation(
                                userLocation.latitude,
                                userLocation.longitude));
        ref.setValue(drop);
        Bundle args = getArguments();
        args.putString(KEY, key);
        getPostData(key);
        ((MainActivity) getActivity()).dismissKeyboard();
    }

    private void toggleState() {
        if (editing) {
            imageChanged = false;
        }
        editing = !editing;
        getArguments().putBoolean(EDITING, editing);
        prepViews();
        ((MainActivity) getActivity()).syncUI();
    }

    @Override
    public void OnImageReceived(Bitmap image) {
        this.image = image;
        imageChanged = true;
        setImageView();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
