package com.rethink.drop.fragments;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rethink.drop.MainActivity;
import com.rethink.drop.R;
import com.rethink.drop.adapters.DropAdapter;
import com.rethink.drop.exceptions.FragmentArgsMismatch;
import com.rethink.drop.interfaces.ImageRecipient;
import com.rethink.drop.models.Drop;
import com.rethink.drop.models.Profile;
import com.rethink.drop.tools.ImageManager;
import com.rethink.drop.tools.Utilities;
import com.rethink.drop.viewholders.DropHolder;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import static com.bumptech.glide.request.RequestOptions.circleCropTransform;
import static com.rethink.drop.MainActivity.EDITING;
import static com.rethink.drop.managers.DataManager.getProfile;
import static com.rethink.drop.models.Profile.PROFILE_KEY;

public class ProfileFragment extends ImageManager implements ImageRecipient {
    private DatabaseReference profRef;
    private ValueEventListener profListener;
    private TextView name;
    private TextView nameField;
    private ViewSwitcher nameFieldSwitcher;
    private TextView userField;
    private ViewSwitcher userFieldSwitcher;
    private Boolean editing;
    private ImageView profileImageView;
    private Profile profile;
    private Menu menu;
    private boolean userOwnsProfile;
    private RecyclerView postsRecycler;

    public static ProfileFragment newInstance(Bundle args) throws FragmentArgsMismatch {
        String userID = args.getString(PROFILE_KEY);
        if (userID == null || userID.equals("")) {
            throw new FragmentArgsMismatch("No profile key");
        }
        ProfileFragment fragment = new ProfileFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        Bundle args = getArguments();
        if (args != null) {
            String profKey = args.getString(PROFILE_KEY);
            profile = getProfile(profKey);
            profRef = getProfileReference(profKey);
        }
        FirebaseUser user = FirebaseAuth.getInstance()
                                        .getCurrentUser();
        userOwnsProfile = user != null && user.getUid()
                                              .equals(getArguments().getString(PROFILE_KEY));
        editing = false;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile,
                                  container,
                                  false);
        profileImageView = v.findViewById(R.id.prof_img);
        profileImageView.setOnClickListener(new ImageClickHandler());
        String profKey = getArguments().getString(PROFILE_KEY);
        if (profKey != null) {
            ViewCompat.setTransitionName(profileImageView,
                                         "prof_" + profKey
            );
        } else {
            Log.e("ProfileFragment",
                  "Missing profKey");
        }

        name = v.findViewById(R.id.prof_name);
        nameField = v.findViewById(R.id.prof_name_edit);
        nameFieldSwitcher = v.findViewById(R.id.name_switcher);
        userField = v.findViewById(R.id.username_edit);
        userFieldSwitcher = v.findViewById(R.id.username_switcher);

        postsRecycler = v.findViewById(R.id.recycler_view);

        if (profile != null) {
            notifyDataChanged(profile);
        }

        return v;
    }

    public void editProfile() {
        editing = true;
        syncUI();
    }

    public void addToFriends() {
        FirebaseUser user = FirebaseAuth.getInstance()
                                        .getCurrentUser();
        if (user != null) {
            Profile.addFriend(user.getUid(),
                              getArguments().getString(PROFILE_KEY),
                              profile);
        }
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
                ref.setValue(new Profile("",
                                         "",
                                         "",
                                         ""));
            } else {
                Toast.makeText(
                        getContext(),
                        "Please log in",
                        Toast.LENGTH_LONG
                )
                     .show();
                getFragmentManager().popBackStackImmediate();
            }
        }
        if (ref == null && userID != null) {
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
             .asBitmap()
             .load(path)
             .into(new SimpleTarget<Bitmap>() {
                 @Override
                 public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                     uploadImage(resource);
                 }
             });
    }

    @Override
    public void onResume() {
        super.onResume();
        profListener = profRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                profile = dataSnapshot.getValue(Profile.class);
                if (profile != null) {
                    notifyDataChanged(profile);
                } else {
                    editing = true;
                    syncUI();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onPause() {
        profRef.removeEventListener(profListener);
        super.onPause();
    }

    private void uploadImage(Bitmap image) {
        Utilities.uploadImage(getActivity(),
                              image,
                              null,
                              getArguments().getString(PROFILE_KEY));
    }

    public void checkUsername() {
        String username = userField.getText()
                                   .toString();
        if (!username.equals(profile.getUsername())) {
            final ProgressDialog progressDialog = new ProgressDialog(getContext());
            progressDialog.setCancelable(false);
            progressDialog.setTitle("Checking username...");
            progressDialog.show();
            DatabaseReference ref = FirebaseDatabase.getInstance()
                                                    .getReference()
                                                    .child("profiles")
                                                    .child("usernames")
                                                    .child(username);
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    progressDialog.dismiss();
                    if (dataSnapshot.exists()) {
                        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext())
                                .setMessage(R.string.username_used)
                                .setPositiveButton(R.string.ok,
                                                   null);
                        dialogBuilder.create()
                                     .show();
                    } else {
                        saveProfile();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } else {
            saveProfile();
        }
    }

    public void saveProfile() {
        String imageURL = profile != null ? profile.getImageURL() : "";
        String thumbnailURL = profile != null ? profile.getThumbnailURL() : "";
        String name = nameField.getText()
                               .toString();
        String username = userField.getText()
                                   .toString();


        DatabaseReference ref = FirebaseDatabase.getInstance()
                                                .getReference()
                                                .child("profiles")
                                                .child("usernames");
        // Remove the old username
        ref.child(profile.getUsername())
           .removeValue();
        // Insert the new one
        ref.child(username)
           .setValue(username);
        profile = new Profile(imageURL,
                              thumbnailURL,
                              name,
                              username);
        profile.save(getArguments().getString(PROFILE_KEY));
        Activity activity = getActivity();
        if (activity != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            View focused = activity.getCurrentFocus();
            if (focused != null) {
                if (imm != null) {
                    imm.hideSoftInputFromWindow(
                            focused.getWindowToken(),
                            0
                    );
                }
            }
        }
        toggleState();
    }

    private void notifyDataChanged(Profile profile) {
        RequestOptions glideOptions = new RequestOptions()
                .centerCrop();
        RequestBuilder<Drawable> thumbnailRequest = Glide.with(ProfileFragment.this)
                                                         .load(profile.getThumbnailURL());
        Glide.with(ProfileFragment.this)
             .load(profile.getImageURL())
             .apply(circleCropTransform())
             .thumbnail(thumbnailRequest)
             .transition(withCrossFade())
             .into(profileImageView);
        name.setText(profile.getName());
        nameField.setText(profile.getName());
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(profile.getUsername());
            }
        }
        userField.setText(profile.getUsername());
        FirebaseRecyclerAdapter<Drop, DropHolder> dropAdapter = DropAdapter.getProfilePosts(
                getContext(),
                getArguments().getString(PROFILE_KEY)
        );
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        postsRecycler.setLayoutManager(layoutManager);
        postsRecycler.setAdapter(dropAdapter);
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
            if (userFieldSwitcher.getNextView()
                                 .getClass()
                                 .equals(TextInputLayout.class)) {
                userFieldSwitcher.showNext();
            }
        } else {
            if (nameFieldSwitcher.getNextView()
                                 .getClass()
                                 .equals(AppCompatTextView.class)) {
                nameFieldSwitcher.showNext();
            }
            if (userFieldSwitcher.getNextView()
                                 .getClass()
                                 .equals(AppCompatTextView.class)) {
                userFieldSwitcher.showNext();
            }
        }
        MenuItem save = menu.findItem(R.id.save_profile);
        MenuItem edit = menu.findItem(R.id.edit_profile);
        MenuItem logOut = menu.findItem(R.id.log_out);
        save.setVisible(userOwnsProfile && editing);
        logOut.setVisible(userOwnsProfile);
        edit.setVisible(userOwnsProfile && !editing);
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.syncUI();
        }
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
}
