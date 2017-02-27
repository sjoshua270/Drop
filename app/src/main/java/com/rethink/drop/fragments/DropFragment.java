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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
import com.rethink.drop.adapters.CommentAdapter;
import com.rethink.drop.interfaces.ImageRecipient;
import com.rethink.drop.models.Comment;
import com.rethink.drop.models.Drop;
import com.rethink.drop.models.Profile;
import com.rethink.drop.tools.ImageManager;
import com.rethink.drop.tools.Utilities;
import com.rethink.drop.viewholders.CommentHolder;

import java.util.Calendar;

import static com.rethink.drop.MainActivity.EDITING;
import static com.rethink.drop.MainActivity.userLocation;
import static com.rethink.drop.models.Drop.KEY;

public class DropFragment extends ImageManager implements ImageRecipient {

    private ImageView imageView;
    private ImageView profileImage;
    private Menu menu;
    private ViewSwitcher descriptionFieldSwitcher;
    private TextView description;
    private TextInputEditText descriptionField;
    private CoordinatorLayout cLayout;
    private DatabaseReference dropReference;
    private DropChangeListener dropListener;
    private FirebaseUser user;
    private Boolean editing;
    private Drop drop;
    private boolean userOwnsDrop;
    private CommentAdapter commentAdapter;
    private TextInputEditText textField;
    private RelativeLayout commentsList;
    private LinearLayout newCommentForm;

    public static DropFragment newInstance(Bundle args) {
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
        setHasOptionsMenu(true);

        // Comments
        Bundle args = getArguments();
        if (args != null) {
            String key = args.getString(KEY);
            editing = key == null;

            if (key != null) {
                DatabaseReference ref = FirebaseDatabase.getInstance()
                                                        .getReference()
                                                        .child("comments")
                                                        .child(key);
                commentAdapter = new CommentAdapter(Comment.class,
                                                    R.layout.comment,
                                                    CommentHolder.class,
                                                    ref);
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
        imageView.setOnClickListener(new ImageClickHandler());
        profileImage = (ImageView) fragmentView.findViewById(R.id.drop_profile_image);
        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.getInstance().openProfile(profileImage, drop.getUserID());
            }
        });
        descriptionFieldSwitcher = (ViewSwitcher) fragmentView.findViewById(R.id.description_switcher);
        description = (TextView) fragmentView.findViewById(R.id.listing_desc);
        descriptionField = (TextInputEditText) fragmentView.findViewById(R.id.listing_input_desc);

        String key = getArguments().getString(KEY);
        if (key != null) {
            ViewCompat.setTransitionName(imageView,
                                         "image_" + key);
            ViewCompat.setTransitionName(description,
                                         "desc_" + key);
            ViewCompat.setTransitionName(profileImage,
                                         "prof_" + key);
        }

        // Comments ============================
        final RecyclerView commentRecycler = (RecyclerView) fragmentView.findViewById(R.id.recycler_view);
        textField = (TextInputEditText) fragmentView.findViewById(R.id.comment_edit_text);
        commentsList = (RelativeLayout) fragmentView.findViewById(R.id.comments_list);
        newCommentForm = (LinearLayout) fragmentView.findViewById(R.id.new_comment_form);

        fragmentView.findViewById(R.id.comment_submit)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Comment comment = new Comment(user.getUid(),
                                                          textField.getText()
                                                                   .toString(),
                                                          Calendar.getInstance()
                                                                  .getTimeInMillis());
                            comment.save(getArguments().getString(KEY),
                                         null);
                            textField.setText("");
                            MainActivity.getInstance()
                                        .dismissKeyboard();
                        }
                    });
        commentRecycler.setLayoutManager(new LinearLayoutManager(getContext(),
                                                                 LinearLayoutManager.VERTICAL,
                                                                 false));
        commentRecycler.setAdapter(commentAdapter);

        return fragmentView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        this.menu = menu;
        inflater.inflate(R.menu.menu_edit,
                         menu);
        syncUI();
        super.onCreateOptionsMenu(menu,
                                  inflater);
    }

    private void toggleComments(String key) {
        if (key != null && !editing) {
            commentsList.setVisibility(View.VISIBLE);
            newCommentForm.setVisibility(View.VISIBLE);
        } else {
            commentsList.setVisibility(View.GONE);
            newCommentForm.setVisibility(View.GONE);
        }
    }

    public void editDrop() {
        editing = true;
        syncUI();
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
            new Drop(user.getUid(),
                     Calendar.getInstance()
                             .getTimeInMillis(),
                     "",
                     "").save(key);
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
    public void publishDrop() {
        String key = getArguments().getString(KEY);
        new Drop(user.getUid(),
                 Calendar.getInstance()
                         .getTimeInMillis(),
                 drop.getImageURL(),
                 descriptionField.getText()
                                 .toString()).save(key);
        saveListing(key);
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
                             new Drop(user.getUid(),
                                      Calendar.getInstance()
                                              .getTimeInMillis(),
                                      downloadUrl.toString(),
                                      descriptionField.getText()
                                                      .toString()).save(key);
                         } else {
                             Snackbar.make(cLayout,
                                           R.string.unexpected_error,
                                           Snackbar.LENGTH_LONG)
                                     .show();
                         }
                     }
                 });
    }

    private void saveListing(String key) {
        GeoLocation location = new GeoLocation(userLocation.latitude,
                                               userLocation.longitude);
        new GeoFire(FirebaseDatabase.getInstance()
                                    .getReference()
                                    .child("geoFire")).setLocation(key,
                                                                   location);
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
        getProfileImage(drop,
                        profileImage);
        description.setText(drop.getText());
        descriptionField.setText(drop.getText());
        userOwnsDrop = user != null && user.getUid()
                                           .equals(drop.getUserID());
        syncUI();
    }

    private void getProfileImage(Drop drop, final ImageView profImageView) {
        final String userID = drop.getUserID();
        FirebaseDatabase.getInstance()
                        .getReference()
                        .child("profiles")
                        .child(userID)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Profile profile = dataSnapshot.getValue(Profile.class);
                                if (profile != null) {
                                    Glide.with(getContext())
                                         .load(profile.getImageURL())
                                         .centerCrop()
                                         .placeholder(R.drawable.ic_face_white_24px)
                                         .crossFade()
                                         .into(profImageView);
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
    }

    private void toggleState() {
        editing = !editing;
        getArguments().putBoolean(EDITING,
                                  editing);
        syncUI();
    }

    private void syncUI() {
        String key = getArguments().getString(KEY);
        toggleComments(key);
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
        MenuItem submit = menu.findItem(R.id.submit_drop);
        MenuItem save = menu.findItem(R.id.save_drop);
        MenuItem edit = menu.findItem(R.id.edit_drop);
        MenuItem delete = menu.findItem(R.id.delete_drop);
        submit.setVisible(key == null);
        save.setVisible(userOwnsDrop && key != null && editing);
        delete.setVisible(userOwnsDrop && key != null);
        edit.setVisible(userOwnsDrop && !editing);
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
                new Drop(user.getUid(),
                         Calendar.getInstance()
                                 .getTimeInMillis(),
                         drop.getImageURL(),
                         descriptionField.getText()
                                         .toString()).save(dropReference.getKey());
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
