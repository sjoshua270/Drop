package com.rethink.drop.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewCompat;
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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rethink.drop.GlideApp;
import com.rethink.drop.MainActivity;
import com.rethink.drop.R;
import com.rethink.drop.adapters.CommentAdapter;
import com.rethink.drop.exceptions.NullDropException;
import com.rethink.drop.interfaces.ImageRecipient;
import com.rethink.drop.models.Comment;
import com.rethink.drop.models.Drop;
import com.rethink.drop.models.Profile;
import com.rethink.drop.tools.ImageManager;
import com.rethink.drop.tools.Utilities;
import com.rethink.drop.viewholders.CommentHolder;

import java.util.Calendar;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import static com.bumptech.glide.request.RequestOptions.centerCropTransform;
import static com.bumptech.glide.request.RequestOptions.circleCropTransform;
import static com.rethink.drop.MainActivity.EDITING;
import static com.rethink.drop.MainActivity.userLocation;
import static com.rethink.drop.managers.DataManager.feedKeys;
import static com.rethink.drop.managers.DataManager.getDrop;
import static com.rethink.drop.managers.DataManager.profiles;
import static com.rethink.drop.models.Comment.COMMENT_KEY;
import static com.rethink.drop.models.Drop.KEY;

public class DropFragment extends ImageManager implements ImageRecipient {

    private static final boolean ALLOW_EDIT = false; // Saving editing for a later update
    private Drop drop; // The Drop with all the data to display
    private DatabaseReference dropRef; // This is where our Drop's data is
    private ValueEventListener dropListener; // This listens for changes to our Drop
    private ImageView dropImage; // Image for the Drop
    private ImageView profileImage; // Image of the Profile who posted it
    private Menu menu; // The menu that shows in the toolbar
    private TextView description; // The text of the Drop. Read only
    private TextInputEditText descriptionField; // The Field to enter/edit text of the Drop
    private RelativeLayout details; // The details view
    private ViewSwitcher descriptionFieldSwitcher; // Helps switch between the previous two views
    private FirebaseUser user; // The current user who will be attached to the post
    private Boolean editing; // To determine whether or not we are in edit mode
    private RecyclerView commentRecycler; // The list of comments on the post
    private TextInputEditText commentField; // This is where a comment can be typed out
    private RelativeLayout commentsList; // The surrounding layout for comments
    private LinearLayout newCommentForm; // The surrounding layout for the new comment field

    public static DropFragment newInstance(Bundle args) {
        DropFragment fragment = new DropFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Let's set everything up before we start
     *
     * @param savedInstanceState This has all the previous arguments that we want to resume with
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = FirebaseAuth.getInstance()
                           .getCurrentUser();
        setHasOptionsMenu(true);

        Bundle args = getArguments();
        if (args != null) {
            String key = args.getString(KEY);
            if (key != null) {
                drop = getDrop(key);
            } else {
                drop = new Drop(
                        getContext(),
                        user.getUid(),
                        Calendar.getInstance()
                                        .getTimeInMillis(),
                        "",
                        "",
                        ""
                );
                key = drop.save(null);
                getArguments().putString(KEY,
                                         key);
            }
            editing = !feedKeys.contains(key);
            dropRef = FirebaseDatabase.getInstance()
                                      .getReference()
                                      .child("posts")
                                      .child(key);
        }
    }

    /**
     * Let's set all of our view variables and attach the listeners we need
     * @return Our prepared view to display
     */
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater,
                           container,
                           savedInstanceState);
        View fragmentView = inflater.inflate(R.layout.fragment_drop,
                                             container,
                                             false);
        dropImage = fragmentView.findViewById(R.id.drop_image);
        dropImage.setOnClickListener(new ImageClickHandler());
        profileImage = fragmentView.findViewById(R.id.drop_profile_image);
        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.openProfile(
                        profileImage,
                        drop.getUserID()
                );
            }
        });
        descriptionFieldSwitcher = fragmentView.findViewById(R.id.description_switcher);
        description = fragmentView.findViewById(R.id.drop_description);
        descriptionField = fragmentView.findViewById(R.id.drop_description_field);
        details = fragmentView.findViewById(R.id.drop_details);

        // Comments ============================
        commentRecycler = fragmentView.findViewById(R.id.recycler_view);
        commentField = fragmentView.findViewById(R.id.comment_edit_text);
        commentsList = fragmentView.findViewById(R.id.comments_list);
        newCommentForm = fragmentView.findViewById(R.id.new_comment_form);

        // Handle Submit button for new comment
        fragmentView.findViewById(R.id.comment_submit)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String commentKey = getArguments().getString(COMMENT_KEY);
                            boolean edited = commentKey != null;
                            new Comment(user.getUid(),
                                        commentField.getText()
                                                    .toString(),
                                        Calendar.getInstance()
                                                .getTimeInMillis(),
                                        edited).save(getArguments().getString(KEY),
                                                     commentKey);
                            // Reset everything for a new comment
                            commentField.setText("");
                            getArguments().putString(COMMENT_KEY,
                                                     null);
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
                        }
                    });
        String dropKey = getArguments().getString(KEY);
        if (dropKey != null) {
            ViewCompat.setTransitionName(dropImage,
                                         "image_" + dropKey);
            ViewCompat.setTransitionName(
                    profileImage,
                    "prof_" + drop.getUserID()
            );
            ViewCompat.setTransitionName(
                    details,
                    "detail_" + dropKey
            );
        }

        try {
            notifyDataChanged(drop);
        } catch (NullDropException nde) {
            Log.e("onCreateView",
                  nde.getMessage());
        }

        return fragmentView;
    }

    /**
     * Attach and remember our new Drop listener
     */
    @Override
    public void onResume() {
        super.onResume();
        dropListener = dropRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                drop = dataSnapshot.getValue(Drop.class);
                if (drop != null) {
                    try {
                        notifyDataChanged(drop);
                    } catch (NullDropException nde) {
                        Log.e("onResume",
                              nde.getMessage());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /**
     * Set our options menu. Here, we have a menu with many options that are switched out in syncUI()
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        this.menu = menu;
        inflater.inflate(R.menu.menu_drop,
                         menu);
        syncUI();
        super.onCreateOptionsMenu(menu,
                                  inflater);
    }

    /**
     * Remove the Drop listener so we don't get calls back to the app
     */
    @Override
    public void onPause() {
        dropRef.removeEventListener(dropListener);
        super.onPause();
    }

    /**
     * Using the dropKey, fetch the relevant comments thread. This method hides or shows our
     * Comments Views and calls prepComments() if the adapter is not set yet
     *
     * @param dropKey The key of our current Drop
     */
    private void toggleComments(String dropKey) {
        if (dropKey != null && !editing) {
            if (commentRecycler.getAdapter() == null) {
                DatabaseReference ref = FirebaseDatabase.getInstance()
                                                        .getReference()
                                                        .child("comments")
                                                        .child(dropKey);
                CommentAdapter commentAdapter = new CommentAdapter(
                        getContext(),
                        user.getUid(),
                        Comment.class,
                        R.layout.item_comment,
                        CommentHolder.class,
                        ref
                );
                commentRecycler.setLayoutManager(new LinearLayoutManager(getContext(),
                                                                         LinearLayoutManager.VERTICAL,
                                                                         false));
                commentRecycler.setAdapter(commentAdapter);
            }
            commentsList.setVisibility(View.VISIBLE);
            newCommentForm.setVisibility(View.VISIBLE);
        } else {
            commentsList.setVisibility(View.GONE);
            newCommentForm.setVisibility(View.GONE);
        }
    }

    public void editComment(String commentText) {
        commentField.setText(commentText);
        commentField.requestFocus();
        Activity activity = getActivity();
        if (activity != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(
                        commentField,
                        InputMethodManager.SHOW_IMPLICIT
                );
            }
        }
    }

    /**
     * Allows a quick switch to Edit mode
     */
    public void editDrop() {
        editing = true;
        syncUI();
    }

    /**
     * Saves the descriptionField text and puts the current key into GeoFire for location tracking
     */
    public void publishDrop() {
        drop = new Drop(
                getContext(),
                user.getUid(),
                drop.getTimestamp(),
                drop.getImageURL(),
                drop.getThumbnailURL(),
                descriptionField.getText()
                                        .toString());

        String key = getArguments().getString(KEY);
        drop.save(key);
        drop.publish(key);
        // If we don't yet have this key...
        if (!feedKeys.contains(key)) {
            // ...then it is new and we can add it to GeoFire, our locations record
            GeoLocation location = new GeoLocation(userLocation.getLatitude(),
                                                   userLocation.getLongitude());
            new GeoFire(FirebaseDatabase.getInstance()
                                        .getReference()
                                        .child("geoFire")).setLocation(key,
                                                                       location);
        }
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

    /**
     * Take in a new Drop and update the relevant data in the UI
     *
     * @param drop The Drop that will replace our current data
     */
    public void notifyDataChanged(Drop drop) throws NullDropException {
        if (drop == null) {
            throw new NullDropException("Drop cannot be null here");
        }
        RequestBuilder<Drawable> thumbnailRequest = Glide.with(DropFragment.this)
                                                         .load(drop.getThumbnailURL());
        int placeholder;
        if (editing) {
            placeholder = R.drawable.ic_add_a_photo_black_24px;
        } else {
            placeholder = R.drawable.ic_image_black_24dp;
        }
        GlideApp.with(DropFragment.this)
                .load(drop.getImageURL())
                .apply(centerCropTransform())
                .placeholder(placeholder)
                .thumbnail(thumbnailRequest)
                .transition(withCrossFade())
                .into(dropImage);

        description.setText(drop.getText());
        descriptionField.setText(drop.getText());

        Profile profile = profiles.get(drop.getUserID());
        if (profile != null) {
            RequestBuilder<Drawable> profThumbnailRequest = Glide.with(DropFragment.this)
                                                                 .load(profile.getThumbnailURL());
            GlideApp.with(DropFragment.this)
                    .load(profile.getImageURL())
                    .apply(circleCropTransform())
                    .thumbnail(profThumbnailRequest)
                    .transition(withCrossFade())
                    .into(profileImage);
        }
    }

    /**
     * Quickly toggle our mode
     */
    private void toggleState() {
        editing = !editing;
        getArguments().putBoolean(EDITING,
                                  editing);
        syncUI();
    }

    /**
     * Sync up the UI with the current state of our variables. This method hides/shows our menu
     * items and switches the description fields
     */
    private void syncUI() {
        String key = getArguments().getString(KEY);
        toggleComments(key);
        boolean userOwnsDrop = user != null && user.getUid()
                                                   .equals(drop.getUserID());
        if (editing) {
            dropImage.setVisibility(View.VISIBLE);
            if (descriptionFieldSwitcher.getNextView()
                                        .getClass()
                                        .equals(TextInputLayout.class)) {
                descriptionFieldSwitcher.showNext();
            }
        } else {
            if (getArguments().getString(KEY) == null) {
                dropImage.setVisibility(View.GONE);
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
        edit.setVisible(userOwnsDrop && !editing && ALLOW_EDIT);
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.syncUI();
        }
    }

    /**
     * Handles receiving an image from the user. In this case, we load it from our Uri and send
     * it to be uploaded
     *
     * @param path The path (or Uri) where our image is
     */
    @Override
    public void receiveImage(final String path) {
        Glide.with(getContext())
             .asBitmap()
             .load(path)
             .into(new SimpleTarget<Bitmap>() {
                 @Override
                 public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                     Utilities.uploadImage(getActivity(),
                                           resource,
                                           getArguments().getString(KEY),
                                           user.getUid());
                 }
             });
    }

    /**
     * This handles what happens when the user taps on the ImageView
     */
    private class ImageClickHandler implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (editing) {
                String dropKey = getArguments().getString(KEY);
                drop = new Drop(
                        getContext(),
                        user.getUid(),
                        drop.getTimestamp(),
                        drop.getImageURL(),
                        drop.getThumbnailURL(),
                        descriptionField.getText()
                                                .toString());
                drop.save(dropKey);

                if (ActivityCompat.checkSelfPermission(getContext(),
                                                       Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(),
                                                                                                                                                                             Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    requestImage(DropFragment.this);
                } else {
                    ActivityCompat.requestPermissions(getActivity(),
                                                      new String[]{
                                                              Manifest.permission.READ_EXTERNAL_STORAGE,
                                                              Manifest.permission.WRITE_EXTERNAL_STORAGE
                                                      },
                                                      0);
                }
            } else {
                ((MainActivity) getActivity()).viewImage(getArguments().getString(KEY));
            }
        }
    }
}
