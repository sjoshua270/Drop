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
    private Bitmap image;
    private ImageView imageView;
    private ViewSwitcher descSwitcher;
    private TextView desc;
    private TextInputEditText inputDesc;
    private Boolean imageChanged;
    private CoordinatorLayout cLayout;
    private DatabaseReference ref;
    private DropChangeListener changeListener;
    private FirebaseUser user;
    private Boolean editing;
    private String imageURL;
    private DatabaseReference dropRef;

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
        Bundle args = getArguments();
        user = FirebaseAuth.getInstance()
                           .getCurrentUser();
        ref = FirebaseDatabase.getInstance()
                              .getReference()
                              .child("posts");
        imageChanged = false;
        changeListener = new DropChangeListener();
        if (args != null) {
            String key = args.getString(KEY);
            editing = key == null;
            if (key != null) {
                updateRef(key);
            }
        }
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
        ViewCompat.setTransitionName(imageView,
                                     "image");
        imageView.setOnClickListener(new ImageClickHandler());

        descSwitcher = (ViewSwitcher) fragmentView.findViewById(R.id.description_switcher);

        desc = (TextView) fragmentView.findViewById(R.id.listing_desc);
        ViewCompat.setTransitionName(desc,
                                     "desc");

        inputDesc = (TextInputEditText) fragmentView.findViewById(R.id.listing_input_desc);
        ViewCompat.setTransitionName(inputDesc,
                                     "input_desc");

        prepViews();

        return fragmentView;
    }

    private void updateRef(String key) {
        dropRef = FirebaseDatabase.getInstance()
                                  .getReference()
                                  .child("posts")
                                  .child(key);
        dropRef.addValueEventListener(changeListener);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_edit,
                         menu);
        super.onCreateOptionsMenu(menu,
                                  inflater);
    }

    private void setImageView() {
        if (image != null) {
            imageView.setImageBitmap(Utilities.squareImage(image));
            imageView.setPadding(0,
                                 0,
                                 0,
                                 0);
        }
    }

    private void prepViews() {
        if (editing) {
            if (descSwitcher.getNextView()
                            .getClass()
                            .equals(TextInputLayout.class)) {
                descSwitcher.showNext();
            }
        } else {
            if (descSwitcher.getNextView()
                            .getClass()
                            .equals(AppCompatTextView.class)) {
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
            saveListing(key,
                        new Drop(user.getUid(),
                                 Calendar.getInstance()
                                         .getTimeInMillis(),
                                 imageURL,
                                 inputDesc.getText()
                                          .toString()));
            toggleState();
        }
    }

    private void uploadImage(final String key) {
        Utilities.uploadImage(getActivity(),
                              image,
                              user.getUid() + "/" + key)
                 .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                     @Override
                     public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                         Uri downloadUrl = taskSnapshot.getDownloadUrl();
                         if (downloadUrl != null) {
                             saveListing(key,
                                         new Drop(user.getUid(),
                                                  Calendar.getInstance()
                                                          .getTimeInMillis(),
                                                  downloadUrl.toString(),
                                                  inputDesc.getText()
                                                           .toString()));
                             toggleState();
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
        new GeoFire(FirebaseDatabase.getInstance()
                                    .getReference()
                                    .child("geoFire")).setLocation(key,
                                                                   new GeoLocation(userLocation.latitude,
                                                                                   userLocation.longitude));
        ref.setValue(drop);
        Bundle args = getArguments();
        args.putString(KEY,
                       key);
        updateRef(key);
        MainActivity.getInstance()
                    .dismissKeyboard();
    }

    private void toggleState() {
        if (editing) {
            imageChanged = false;
        }
        editing = !editing;
        getArguments().putBoolean(EDITING,
                                  editing);
        prepViews();
        MainActivity.getInstance()
                    .syncUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dropRef == null) {
            String key = getArguments().getString(KEY);
            if (key != null) {
                updateRef(key);
            }
        } else {
            dropRef.addValueEventListener(changeListener);
        }
    }

    @Override
    public void onPause() {
        if (dropRef != null) {
            dropRef.removeEventListener(changeListener);
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void receiveImage(final String path) {
        imageChanged = true;
        Glide.with(getContext())
             .load(path)
             .asBitmap()
             .into(new SimpleTarget<Bitmap>() {
                 @Override
                 public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                     image = resource;
                     setImageView();
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
            Drop drop = dataSnapshot.getValue(Drop.class);
            if (drop != null) {
                imageURL = drop.getImageURL() == null ? "" : drop.getImageURL();
                if (!imageURL.equals("")) {
                    Glide.with(getContext())
                         .load(imageURL)
                         .centerCrop()
                         .placeholder(R.drawable.ic_photo_camera_white_24px)
                         .crossFade()
                         .into(imageView);
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
    }
}
