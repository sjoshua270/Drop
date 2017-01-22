package com.rethink.drop.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
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
import com.rethink.drop.MainActivity;
import com.rethink.drop.R;
import com.rethink.drop.interfaces.ImageRecipient;
import com.rethink.drop.models.Drop;
import com.rethink.drop.tools.ImageManager;
import com.rethink.drop.tools.Utilities;

import java.util.Calendar;

import static com.rethink.drop.MainActivity.EDITING;
import static com.rethink.drop.MainActivity.userLocation;
import static com.rethink.drop.models.Drop.KEY;

public class DropFragment extends ImageManager implements ImageRecipient {

    private static final String TAG = "DropFragment";
    private ImageView imageView;
    private ViewSwitcher descriptionFieldSwitcher;
    private TextView description;
    private TextInputEditText descriptionField;
    private CoordinatorLayout cLayout;
    private DatabaseReference dropReference;
    private DropChangeListener dropListener;
    private FirebaseUser user;
    private Boolean editing;
    private Drop drop;

    public static DropFragment newInstance(String key) {
        Bundle args = new Bundle();
        args.putString(KEY,
                       key);
        DropFragment fragment = new DropFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = FirebaseAuth.getInstance()
                           .getCurrentUser();
        dropListener = new DropChangeListener();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater,
                           container,
                           savedInstanceState);
        View fragmentView = inflater.inflate(R.layout.fragment_drop,
                                             container,
                                             false);
        cLayout = (CoordinatorLayout) getActivity().findViewById(R.id.coordinator);
        imageView = (ImageView) fragmentView.findViewById(R.id.listing_image);
        imageView.setOnClickListener(new ImageClickHandler());
        descriptionFieldSwitcher = (ViewSwitcher) fragmentView.findViewById(R.id.description_switcher);
        description = (TextView) fragmentView.findViewById(R.id.listing_desc);
        descriptionField = (TextInputEditText) fragmentView.findViewById(R.id.listing_input_desc);

        String key = getArguments().getString(KEY);
        if (key != null) {
            ViewCompat.setTransitionName(imageView,
                                         "image_" + key);
            ViewCompat.setTransitionName(description,
                                         "desc_" + key);
        }

        return fragmentView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_edit,
                         menu);
        super.onCreateOptionsMenu(menu,
                                  inflater);
    }

    public void handleFabPress() {
        if (editing) {
            publishListing();
        } else {
            toggleState();
        }
    }

    private DatabaseReference getDropReference(String key) {
        DatabaseReference ref;
        if (key == null) {
            ref = FirebaseDatabase.getInstance()
                                  .getReference()
                                  .child("posts")
                                  .push();
            key = ref.getKey();
            getArguments().putString(KEY,
                                     key);
            ref.setValue(new Drop(user.getUid(),
                                  Calendar.getInstance()
                                          .getTimeInMillis(),
                                  "",
                                  ""));
        } else {
            ref = FirebaseDatabase.getInstance()
                                  .getReference()
                                  .child("posts")
                                  .child(key);
        }
        return ref;
    }

    /**
     * Takes all values in the current layout and sends them off to Firebase,
     * then returns to the previous fragment
     */
    private void publishListing() {
        String key = getArguments().getString("KEY");
        saveListing(key,
                    new Drop(user.getUid(),
                             Calendar.getInstance()
                                     .getTimeInMillis(),
                             drop.getImageURL(),
                             descriptionField.getText()
                                             .toString()));
        toggleState();

    }

    private void uploadImage(Bitmap image, final String key) {
        Utilities.uploadImage(getActivity(),
                              image,
                              user.getUid() + "/" + key)
                 .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                     @Override
                     public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                         Uri downloadUrl = taskSnapshot.getDownloadUrl();
                         if (downloadUrl != null) {
                             dropReference.setValue(new Drop(user.getUid(),
                                                             Calendar.getInstance()
                                                                     .getTimeInMillis(),
                                                             downloadUrl.toString(),
                                                             descriptionField.getText()
                                                                             .toString()));
                         } else {
                             Snackbar.make(cLayout,
                                           R.string.unexpected_error,
                                           Snackbar.LENGTH_LONG)
                                     .show();
                         }
                     }
                 });
    }

    private void saveListing(String key, Drop drop) {
        GeoLocation location = new GeoLocation(userLocation.latitude,
                                               userLocation.longitude);
        new GeoFire(FirebaseDatabase.getInstance()
                                    .getReference()
                                    .child("geoFire")).setLocation(key,
                                                                   location);
        dropReference.setValue(drop);
        getArguments().putString(KEY,
                                 key);
        MainActivity.getInstance()
                    .dismissKeyboard();
    }

    private void updateData(Drop drop) {
        Glide.with(getContext())
             .load(drop.getImageURL())
             .centerCrop()
             .placeholder(R.drawable.ic_photo_camera_black_24px)
             .crossFade()
             .into(imageView);
        description.setText(drop.getText());
        descriptionField.setText(drop.getText());
    }

    private void toggleState() {
        editing = !editing;
        getArguments().putBoolean(EDITING,
                                  editing);
        syncUI();
    }

    private void syncUI() {
        if (editing) {
            imageView.setVisibility(View.VISIBLE);
            if (descriptionFieldSwitcher.getNextView()
                                        .getClass()
                                        .equals(TextInputLayout.class)) {
                descriptionFieldSwitcher.showNext();
            }
        } else {
            if (getArguments().getString(KEY) == null) {
                imageView.setVisibility(View.GONE);
            }
            if (descriptionFieldSwitcher.getNextView()
                                        .getClass()
                                        .equals(AppCompatTextView.class)) {
                descriptionFieldSwitcher.showNext();
            }
        }
        setHasOptionsMenu(editing);
        MainActivity.getInstance()
                    .syncUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dropReference == null) {
            String key = getArguments().getString(KEY);
            editing = key == null;
            dropReference = getDropReference(key);
        }
        dropReference.addValueEventListener(dropListener);
        syncUI();
    }

    @Override
    public void onPause() {
        if (dropReference != null) {
            dropReference.removeEventListener(dropListener);
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void receiveImage(final String path) {
        Glide.with(getContext())
             .load(path)
             .asBitmap()
             .into(new SimpleTarget<Bitmap>() {
                 @Override
                 public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                     uploadImage(resource,
                                 getArguments().getString(KEY));
                 }
             });
    }

    private class ImageClickHandler implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (editing) {
                if (ActivityCompat.checkSelfPermission(getContext(),
                                                       Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    requestImage(DropFragment.this);
                } else {
                    ActivityCompat.requestPermissions(getActivity(),
                                                      new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                                      0);
                }
            } else {
                ((MainActivity) getActivity()).viewImage(getArguments().getString(KEY));
            }
        }
    }

    private class DropChangeListener implements ValueEventListener {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            drop = dataSnapshot.getValue(Drop.class);
            if (drop != null) {
                updateData(drop);
            } else {
                MainActivity.getInstance()
                            .showMessage("Drop deleted");
                getFragmentManager().popBackStackImmediate();
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    }
}
