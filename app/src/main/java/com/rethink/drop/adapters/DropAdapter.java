package com.rethink.drop.adapters;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rethink.drop.MainActivity;
import com.rethink.drop.R;
import com.rethink.drop.models.Drop;
import com.rethink.drop.models.Profile;
import com.rethink.drop.viewholders.DropHolder;

import static com.rethink.drop.managers.DataManager.addDrop;
import static com.rethink.drop.managers.DataManager.feedKeys;
import static com.rethink.drop.managers.DataManager.getDrop;
import static com.rethink.drop.managers.DataManager.profiles;


/**
 * Fills in the UI elements for our list of local Drops
 */
public class DropAdapter extends RecyclerView.Adapter<DropHolder> {
    private Context context;

    public static FirebaseRecyclerAdapter<Drop, DropHolder> getProfilePosts(String profileKey) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                                                .getReference()
                                                .child("drops_by_profile")
                                                .child(profileKey);
        return new FirebaseRecyclerAdapter<Drop, DropHolder>(Drop.class,
                                                             R.layout.item_drop,
                                                             DropHolder.class,
                                                             ref) {
            @Override
            protected void populateViewHolder(final DropHolder dropHolder, Drop drop, int position) {
                final String key = getRef(position).getKey();
                if (drop != null) {
                    addDrop(key,
                            drop);
                    // Set the Drop image
                    String dropImageUrl = drop.getImageURL();
                    if (dropImageUrl != null) {
                        Glide.with(MainActivity.getInstance())
                             .load(dropImageUrl)
                             .centerCrop()
                             .placeholder(R.drawable.ic_photo_camera_black_24px)
                             .crossFade()
                             .into(dropHolder.imageView);
                    }
                    // Set the Drop text
                    dropHolder.desc.setText(drop.getText());

                    // Set the Profile image
                    Profile profile = profiles.get(drop.getUserID());
                    if (profile != null) {
                        String profileImageUrl = profile.getImageURL();
                        if (profileImageUrl != null) {
                            Glide.with(MainActivity.getInstance())
                                 .load(profileImageUrl)
                                 .centerCrop()
                                 .placeholder(R.drawable.ic_face_white_24px)
                                 .crossFade()
                                 .into(dropHolder.profile);
                        }
                    }
                }
                ViewCompat.setTransitionName(dropHolder.imageView,
                                             "image_" + key);
                ViewCompat.setTransitionName(dropHolder.desc,
                                             "desc_" + key);
                ViewCompat.setTransitionName(dropHolder.profile,
                                             "prof_" + key);
                dropHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MainActivity.getInstance()
                                    .openListing(dropHolder.itemView,
                                                 key);
                    }
                });
            }
        };
    }

    @Override
    public DropHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View v = LayoutInflater.from(parent.getContext())
                               .inflate(R.layout.item_drop, parent, false);
        return new DropHolder(v);
    }

    @Override
    public void onBindViewHolder(final DropHolder holder, final int position) {
        final String key = feedKeys.get(position);
        Drop drop = getDrop(key);
        if (drop != null) {
            // Set the Drop image
            String dropImageUrl = drop.getImageURL();
            if (dropImageUrl != null && !dropImageUrl.equals("")) {
                holder.imageView.setVisibility(View.VISIBLE);
                Glide.with(context)
                     .load(dropImageUrl)
                     .centerCrop()
                     .placeholder(R.drawable.ic_photo_camera_black_24px)
                     .crossFade()
                     .into(holder.imageView);
            } else {
                holder.imageView.setVisibility(View.GONE);
            }
            // Set the Drop text
            holder.desc.setText(drop.getText());

            // Set the Profile image
            Profile profile = profiles.get(drop.getUserID());
            if (profile != null) {
                String profileImageUrl = profile.getImageURL();
                if (profileImageUrl != null) {
                    Glide.with(context)
                         .load(profileImageUrl)
                         .centerCrop()
                         .placeholder(R.drawable.ic_face_white_24px)
                         .crossFade()
                         .into(holder.profile);
                }
            }
        }
        ViewCompat.setTransitionName(holder.imageView, "image_" + key);
        ViewCompat.setTransitionName(holder.desc, "desc_" + key);
        ViewCompat.setTransitionName(holder.profile, "prof_" + key);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.getInstance()
                            .openListing(holder.itemView,
                                         key);
            }
        });
    }

    @Override
    public int getItemCount() {
        return feedKeys.size();
    }
}
