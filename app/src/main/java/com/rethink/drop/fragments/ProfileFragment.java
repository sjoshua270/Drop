package com.rethink.drop.fragments;


import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
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
import com.rethink.drop.models.Profile;
import com.rethink.drop.tools.ImageManager;
import com.rethink.drop.tools.Utilities;

import static com.rethink.drop.MainActivity.EDITING;

public class ProfileFragment extends ImageManager implements ImageRecipient {
    private static final String TAG = "ProfileFragment";
    private static final String USER_ID = "user_id";
    private DatabaseReference profileReference;
    private ProfileListener profileListener;
    private TextView name;
    private TextView nameField;
    private ViewSwitcher nameFieldSwitcher;
    private Boolean editing;
    private ImageView profileImageView;
    private Profile profile;
    private View container;
    private Menu menu;
    private boolean userOwnsProfile;
    private String userID;

    public static ProfileFragment newInstance(@Nullable String userID) {
        Bundle args = new Bundle();
        args.putString(USER_ID,
                       userID);
        ProfileFragment fragment = new ProfileFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userID = getArguments().getString(USER_ID);
        if (userID != null) {
            profileListener = new ProfileListener();
        } else {
            MainActivity.getInstance()
                        .showMessage("No user");
        }
        editing = false;
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.container = container;
        View v = inflater.inflate(R.layout.fragment_profile,
                                  container,
                                  false);
        profileImageView = (ImageView) v.findViewById(R.id.prof_img);
        profileImageView.setOnClickListener(new ImageClickHandler());

        name = (TextView) v.findViewById(R.id.prof_name);
        nameField = (TextView) v.findViewById(R.id.prof_name_edit);
        nameFieldSwitcher = (ViewSwitcher) v.findViewById(R.id.username_switcher);
        return v;
    }

    public void editProfile() {
        editing = true;
        syncUI();
    }

    private DatabaseReference getProfileReference(String userID) {
        DatabaseReference ref = null;
        if (userID == null) {
            FirebaseUser user = FirebaseAuth.getInstance()
                                            .getCurrentUser();
            if (user != null) {
                userID = user.getUid();
                ref = FirebaseDatabase.getInstance()
                                      .getReference()
                                      .child("profiles")
                                      .child(userID);
                ref.setValue(new Profile(userID,
                                         "",
                                         ""));
            } else {
                MainActivity.getInstance()
                            .showMessage("Please log in");
                getFragmentManager().popBackStackImmediate();
            }
        }
        if (ref == null) {
            ref = FirebaseDatabase.getInstance()
                                  .getReference()
                                  .child("profiles")
                                  .child(userID);
        }
        return ref;
    }

    @Override
    public void receiveImage(String path) {
        Glide.with(getContext())
             .load(path)
             .asBitmap()
             .into(new SimpleTarget<Bitmap>() {
                 @Override
                 public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                     uploadImage(resource);
                 }
             });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (profileReference == null) {
            String userID = getArguments().getString(USER_ID);
            editing = false;
            profileReference = getProfileReference(userID);
        }
        profileReference.addValueEventListener(profileListener);
    }

    @Override
    public void onPause() {
        if (profileListener != null) {
            profileReference.removeEventListener(profileListener);
        }
        super.onPause();
    }

    public void handleFabPress() {
        if (editing) {
            String imageURL = (profile != null && profile.getImageURL() != null) ? profile.getImageURL() : "";
            profile = new Profile(getArguments().getString(USER_ID),
                                  imageURL,
                                  nameField.getText()
                                           .toString());
            saveProfile();
            toggleState();
        } else {
            toggleState();
        }
    }

    private void uploadImage(Bitmap image) {
        String filename = nameField.getText()
                                   .toString()
                                   .replaceAll("[^A-Za-z]+",
                                                  "")
                                   .toLowerCase();
        Utilities.uploadImage(getActivity(),
                              image,
                              "profile_images/" + getArguments().getString(USER_ID) + "/" + filename)
                 .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                     @Override
                     public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                         Uri downloadUrl = taskSnapshot.getDownloadUrl();
                         if (downloadUrl != null) {
                             String userName = nameField.getText()
                                                        .toString();
                             if (profile != null) {
                                 userName = profile.getName();
                             }
                             profile = new Profile(getArguments().getString(USER_ID),
                                                   downloadUrl.toString(),
                                                   userName);
                             saveProfile();
                         } else {
                             Snackbar.make(container,
                                           R.string.unexpected_error,
                                           Snackbar.LENGTH_LONG)
                                     .show();
                         }
                     }
                 });
    }

    public void saveProfile() {
        profileReference.setValue(new Profile(userID,
                                              profile.getImageURL(),
                                              nameField.getText()
                                                       .toString()));
        MainActivity.getInstance()
                    .dismissKeyboard();
        toggleState();
    }

    private void updateData(Profile profile) {
        Glide.with(getContext())
             .load(profile.getImageURL())
             .centerCrop()
             .placeholder(R.drawable.ic_photo_camera_white_24px)
             .crossFade()
             .into(profileImageView);
        name.setText(profile.getName());
        nameField.setText(profile.getName());
        FirebaseUser user = FirebaseAuth.getInstance()
                                        .getCurrentUser();
        userOwnsProfile = user != null && user.getUid()
                                              .equals(userID);
        syncUI();
    }

    private void toggleState() {
        editing = !editing;
        getArguments().putBoolean(EDITING,
                                  editing);
        syncUI();
    }

    private void syncUI() {
        if (editing) {
            if (nameFieldSwitcher.getNextView()
                                 .getClass()
                                 .equals(TextInputLayout.class)) {
                nameFieldSwitcher.showNext();
            }
        } else {
            if (nameFieldSwitcher.getNextView()
                                 .getClass()
                                 .equals(AppCompatTextView.class)) {
                nameFieldSwitcher.showNext();
            }
        }
        MenuItem save = menu.findItem(R.id.save_profile);
        MenuItem edit = menu.findItem(R.id.edit_profile);
        MenuItem logOut = menu.findItem(R.id.log_out);
        save.setVisible(userOwnsProfile && editing);
        logOut.setVisible(userOwnsProfile);
        edit.setVisible(userOwnsProfile && !editing);
        MainActivity.getInstance()
                    .syncUI();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        this.menu = menu;
        inflater.inflate(R.menu.menu_profile,
                         menu);
        syncUI();
        super.onCreateOptionsMenu(menu,
                                  inflater);
    }

    private class ImageClickHandler implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (editing) {
                if (ActivityCompat.checkSelfPermission(getContext(),
                                                       Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    requestImage(ProfileFragment.this);
                } else {
                    ActivityCompat.requestPermissions(getActivity(),
                                                      new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                                      0);
                }
            }
        }
    }

    private class ProfileListener implements ValueEventListener {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            profile = dataSnapshot.getValue(Profile.class);
            if (profile != null) {
                updateData(profile);
            } else {
                editing = true;
                syncUI();
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    }
}
