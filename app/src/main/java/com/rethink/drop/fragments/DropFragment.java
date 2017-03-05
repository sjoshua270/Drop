package com.rethink.drop.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
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

import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
import static com.rethink.drop.managers.DataManager.getDrop;
import static com.rethink.drop.managers.DataManager.profiles;
import static com.rethink.drop.models.Drop.KEY;

public class DropFragment extends ImageManager implements ImageRecipient {

    private Drop drop; // The Drop with all the data to display
    private DatabaseReference dropRef; // This is where our Drop's data is
    private ImageView dropImage; // Image for the Drop
    private ImageView profileImage; // Image of the Profile who posted it
    private Menu menu; // The menu that shows in the toolbar
    private TextView description; // The text of the Drop. Read only
    private TextInputEditText descriptionField; // The Field to enter/edit text of the Drop
    private ViewSwitcher descriptionFieldSwitcher; // Helps switch between the previous two views
    private FirebaseUser user; // The current user who will be attached to the post
    private Boolean editing; // To determine whether or not we are in edit mode
    private boolean userOwnsDrop; // To decide whether or not to show editing options
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
            drop = getDrop(key);
            editing = drop == null;
            if (editing) {
                drop = new Drop(user.getUid(),
                                Calendar.getInstance()
                                        .getTimeInMillis(),
                                "",
                                "",
                                "");
                key = drop.save(key);
                getArguments().putString(KEY,
                                         key);
            }
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
        dropImage = (ImageView) fragmentView.findViewById(R.id.drop_image);
        dropImage.setOnClickListener(new ImageClickHandler());
        profileImage = (ImageView) fragmentView.findViewById(R.id.drop_profile_image);
        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.getInstance()
                            .openProfile(profileImage,
                                         drop.getUserID());
            }
        });
        descriptionFieldSwitcher = (ViewSwitcher) fragmentView.findViewById(R.id.description_switcher);
        description = (TextView) fragmentView.findViewById(R.id.drop_description);
        descriptionField = (TextInputEditText) fragmentView.findViewById(R.id.drop_description_field);

        // Comments ============================
        commentRecycler = (RecyclerView) fragmentView.findViewById(R.id.recycler_view);
        commentField = (TextInputEditText) fragmentView.findViewById(R.id.comment_edit_text);
        commentsList = (RelativeLayout) fragmentView.findViewById(R.id.comments_list);
        newCommentForm = (LinearLayout) fragmentView.findViewById(R.id.new_comment_form);

        // Handle Submit button for new comment
        fragmentView.findViewById(R.id.comment_submit)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            new Comment(user.getUid(),
                                        commentField.getText()
                                                    .toString(),
                                        Calendar.getInstance()
                                                .getTimeInMillis()).save(getArguments().getString(KEY),
                                                                         null);
                            commentField.setText("");
                            MainActivity.getInstance()
                                        .dismissKeyboard();
                        }
                    });


        String key = getArguments().getString(KEY);
        if (key != null) {
            ViewCompat.setTransitionName(dropImage,
                                         "image_" + key);
            ViewCompat.setTransitionName(description,
                                         "desc_" + key);
            ViewCompat.setTransitionName(profileImage,
                                         "prof_" + key);
        }

        notifyDataChanged(drop);

        dropRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                drop = dataSnapshot.getValue(Drop.class);
                notifyDataChanged(drop);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

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
            if (commentRecycler.getAdapter() == null) {
                prepComments(key);
            }
            commentsList.setVisibility(View.VISIBLE);
            newCommentForm.setVisibility(View.VISIBLE);
        } else {
            commentsList.setVisibility(View.GONE);
            newCommentForm.setVisibility(View.GONE);
        }
    }

    private void prepComments(String dropKey) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                                                .getReference()
                                                .child("comments")
                                                .child(dropKey);
        CommentAdapter commentAdapter = new CommentAdapter(Comment.class,
                                                           R.layout.item_comment,
                                                           CommentHolder.class,
                                                           ref);
        commentRecycler.setLayoutManager(new LinearLayoutManager(getContext(),
                                                                 LinearLayoutManager.VERTICAL,
                                                                 false));
        commentRecycler.setAdapter(commentAdapter);
    }

    public void editDrop() {
        editing = true;
        syncUI();
    }

    /**
     * Takes all values in the current layout and sends them off to Firebase
     */
    public void publishDrop() {
        if (drop != null) {
            drop = new Drop(user.getUid(),
                            Calendar.getInstance()
                                    .getTimeInMillis(),
                            drop.getImageURL(),
                            drop.getThumbnailURL(),
                            descriptionField.getText()
                                            .toString());
        } else {
            drop = new Drop(user.getUid(),
                            Calendar.getInstance()
                                    .getTimeInMillis(),
                            "",
                            "",
                            descriptionField.getText()
                                            .toString());
        }
        String key = getArguments().getString(KEY);
        // If the key is null, the Drop.save() function saves with a new key and returns the new key
        key = drop.save(key);
        GeoLocation location = new GeoLocation(userLocation.latitude,
                                               userLocation.longitude);
        new GeoFire(FirebaseDatabase.getInstance()
                                    .getReference()
                                    .child("geoFire")).setLocation(key,
                                                                   location);
        MainActivity.getInstance()
                    .dismissKeyboard();
        toggleState();
    }

    /**
     * Take in a new Drop and update the relevant data in the UI
     *
     * @param drop The Drop that will replace our current data
     */
    public void notifyDataChanged(Drop drop) {
        DrawableRequestBuilder<String> thumbnailRequest = Glide.with(DropFragment.this)
                                                               .load(drop.getThumbnailURL());
        Glide.with(DropFragment.this)
             .load(drop.getImageURL())
             .centerCrop()
             .placeholder(R.drawable.ic_photo_camera_black_24px)
             .crossFade()
             .thumbnail(thumbnailRequest)
             .into(dropImage);

        description.setText(drop.getText());
        descriptionField.setText(drop.getText());

        Profile profile = profiles.get(drop.getUserID());
        if (profile != null) {
            DrawableRequestBuilder<String> profThumbnailRequest = Glide.with(DropFragment.this)
                                                                       .load(profile.getThumbnailURL());
            Glide.with(DropFragment.this)
                 .load(profile.getImageURL())
                 .centerCrop()
                 .placeholder(R.drawable.ic_face_white_24px)
                 .crossFade()
                 .thumbnail(profThumbnailRequest)
                 .into(profileImage);
        }
        userOwnsDrop = user != null && user.getUid()
                                           .equals(drop.getUserID());
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
        edit.setVisible(userOwnsDrop && !editing);
        MainActivity.getInstance()
                    .syncUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (drop == null) {
            String key = getArguments().getString(KEY);
            drop = getDrop(key);
            editing = drop == null;
        }
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

    private void uploadImage(Bitmap image, final String key) {
        Utilities.uploadImage(getActivity(),
                              image,
                              key,
                              user.getUid());
    }

    private class ImageClickHandler implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (editing) {
                String dropKey = getArguments().getString(KEY);
                if (drop != null) {
                    drop = new Drop(user.getUid(),
                                    Calendar.getInstance()
                                            .getTimeInMillis(),
                                    drop.getImageURL(),
                                    drop.getThumbnailURL(),
                                    descriptionField.getText()
                                                    .toString());
                } else {
                    drop = new Drop(user.getUid(),
                                    Calendar.getInstance()
                                            .getTimeInMillis(),
                                    "",
                                    "",
                                    descriptionField.getText()
                                                    .toString());

                }
                getArguments().putString(KEY,
                                         drop.save(dropKey));
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
}
