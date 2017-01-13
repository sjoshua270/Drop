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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.UploadTask;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.rethink.drop.MainActivity;
import com.rethink.drop.R;
import com.rethink.drop.interfaces.ImageRecipient;
import com.rethink.drop.models.Profile;
import com.rethink.drop.tools.ImageManager;
import com.rethink.drop.tools.Utilities;

import static com.rethink.drop.MainActivity.EDITING;
import static com.rethink.drop.models.Drop.KEY;

public class ProfileFragment extends ImageManager implements ImageRecipient {
    private static final String USER_ID = "user_id";
    private DatabaseReference ref;
    private ProfileListener profileListener;
    private TextView profName;
    private TextView profNameEdit;
    private Boolean editing;
    private ImageView profImage;
    private Profile profile;
    private View container;
    private ViewSwitcher nameSwitcher;

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
        String userID = getArguments().getString(USER_ID);
        if (userID != null) {
            ref = FirebaseDatabase.getInstance()
                                  .getReference()
                                  .child("profiles")
                                  .child(userID);
            profileListener = new ProfileListener();
        } else {
            Toast.makeText(getContext(),
                           "No userID",
                           Toast.LENGTH_LONG)
                 .show();
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
        profImage = (ImageView) v.findViewById(R.id.prof_img);
        profImage.setOnClickListener(new ImageClickHandler());

        profName = (TextView) v.findViewById(R.id.prof_name);
        profNameEdit = (TextView) v.findViewById(R.id.prof_name_edit);
        nameSwitcher = (ViewSwitcher) v.findViewById(R.id.username_switcher);
        return v;
    }

    @Override
    public void receiveImage(String path) {
        ImageLoader.getInstance()
                   .loadImage(path,
                              new ImageLoadingListener() {
                                  @Override
                                  public void onLoadingStarted(String imageUri, View view) {

                                  }

                                  @Override
                                  public void onLoadingFailed(String imageUri, View view, FailReason failReason) {

                                  }

                                  @Override
                                  public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                                      uploadImage(loadedImage);
                                  }

                                  @Override
                                  public void onLoadingCancelled(String imageUri, View view) {

                                  }
                              });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (profileListener != null) {
            ref.addValueEventListener(profileListener);
        }
        syncUI();
    }

    @Override
    public void onPause() {
        if (profileListener != null) {
            ref.removeEventListener(profileListener);
        }
        super.onPause();
    }

    public void handleFabPress() {
        if (editing) {
            String imageURL = (profile != null && profile.getImageURL() != null) ? profile.getImageURL() : "";
            saveProfile(new Profile(getArguments().getString(USER_ID),
                                    imageURL,
                                    profNameEdit.getText()
                                                .toString()));
            toggleState();
        } else {
            toggleState();
        }
    }

    private void uploadImage(Bitmap image) {
        String filename = profNameEdit.getText()
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
                             String userName = profNameEdit.getText()
                                                           .toString();
                             if (profile != null) {
                                 userName = profile.getName();
                             }
                             saveProfile(new Profile(getArguments().getString(USER_ID),
                                                     downloadUrl.toString(),
                                                     userName));
                         } else {
                             Snackbar.make(container,
                                           R.string.unexpected_error,
                                           Snackbar.LENGTH_LONG)
                                     .show();
                         }
                     }
                 });
    }

    private void saveProfile(Profile profile) {
        ref.setValue(profile);
    }

    private void syncUI() {
        if (editing) {
            if (nameSwitcher.getNextView()
                            .getClass()
                            .equals(TextInputLayout.class)) {
                nameSwitcher.showNext();
            }
        } else {
            if (nameSwitcher.getNextView()
                            .getClass()
                            .equals(AppCompatTextView.class)) {
                nameSwitcher.showNext();
            }
        }
        MainActivity.getInstance()
                    .syncUI();
    }

    private void toggleState() {
        editing = !editing;
        getArguments().putBoolean(EDITING,
                                  editing);
        syncUI();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_profile,
                         menu);
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
            } else {
                MainActivity.getInstance()
                            .viewImage(getArguments().getString(KEY));
            }
        }
    }

    private class ProfileListener implements ValueEventListener {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            profile = dataSnapshot.getValue(Profile.class);
            if (profile == null) {
                editing = true;
            } else {
                profName.setText(profile.getName());
                profNameEdit.setText(profile.getName());
                String imageURL = profile.getImageURL() == null ? "" : profile.getImageURL();
                if (!imageURL.equals("")) {
                    ImageLoader.getInstance()
                               .displayImage(imageURL,
                                             profImage);
                }
            }
            syncUI();
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    }
}
