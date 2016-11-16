package com.rethink.drop.adapters;

import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rethink.drop.MainActivity;
import com.rethink.drop.R;
import com.rethink.drop.models.Drop;
import com.rethink.drop.models.Profile;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.rethink.drop.DataManager.keys;

public class DropAdapter
        extends RecyclerView.Adapter<DropAdapter.ListingHolder> {
    private Context context;

    @Override
    public ListingHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View v = LayoutInflater.from(parent.getContext())
                               .inflate(R.layout.listing, parent, false);
        return new ListingHolder(v);
    }

    @Override
    public void onBindViewHolder(final ListingHolder holder, final int position) {
        final String key = keys.get(position);
        getPostData(key, holder);
        ViewCompat.setTransitionName(holder.imageView, "image_" + key);
        ViewCompat.setTransitionName(holder.title, "title_" + key);
        ViewCompat.setTransitionName(holder.desc, "desc_" + key);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    ((MainActivity) holder.itemView.getContext()).openListing(
                            holder.itemView,
                            ((BitmapDrawable) holder.imageView.getDrawable()).getBitmap(),
                            keys.get(holder.getAdapterPosition()));
                } catch (ClassCastException | NullPointerException e) {
                    ((MainActivity) holder.itemView.getContext()).openListing(
                            holder.itemView,
                            null,
                            keys.get(holder.getAdapterPosition()));
                }
            }
        });
    }

    private void getPostData(final String key, final ListingHolder holder) {
        FirebaseDatabase.getInstance()
                        .getReference()
                        .child("posts")
                        .child(key)
                        .addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Drop drop = dataSnapshot.getValue(Drop.class);
                                if (drop != null) {
                                    getPostImage(drop, holder.imageView);
                                    getProfileImage(drop, holder.profile);

                                    holder.imageView.setPadding(0, 0, 0, 0);
                                    holder.title.setText(drop.getTitle());
                                    holder.desc.setText(drop.getDescription());
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
    }

    private void getPostImage(Drop drop, ImageView imageView) {
        String imageUrl = drop.getImageURL() == null ? "" : drop.getImageURL();
        if (!imageUrl.equals("")) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            Picasso.with(context)
                   .load(imageUrl)
                   .placeholder(R.drawable.ic_photo_camera_white_24px)
                   .resize(width,
                           context.getResources()
                                  .getDimensionPixelSize(R.dimen.item_image_height))
                   .centerCrop()
                   .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.ic_photo_camera_white_24px);
            int padding = context
                    .getResources()
                    .getDimensionPixelSize(
                            R.dimen.listing_padding);
            imageView.setPadding(padding, padding, padding, padding);
        }
    }

    private void getProfileImage(Drop drop, final CircleImageView circleImageView) {
        String userID = drop.getUserID();
        FirebaseDatabase.getInstance()
                        .getReference()
                        .child("profiles")
                        .child(userID)
                        .addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Profile profile = dataSnapshot.getValue(Profile.class);
                                if (profile != null) {
                                    String imageUrl = profile.getImageURL() == null ? "" : profile.getImageURL();
                                    if (!imageUrl.equals("")) {
                                        setProfileImage(imageUrl, circleImageView);
                                    } else {
                                        setDefaultProfileImage(circleImageView);
                                    }
                                } else {
                                    setDefaultProfileImage(circleImageView);
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
    }

    private void setProfileImage(String imageUrl, CircleImageView circleImageView) {
        Picasso.with(context)
               .load(imageUrl)
               .placeholder(R.drawable.ic_photo_camera_white_24px)
               .resize(context.getResources()
                              .getDimensionPixelSize(R.dimen.listing_prof_dimen),
                       context.getResources()
                              .getDimensionPixelSize(R.dimen.listing_prof_dimen))
               .centerCrop()
               .into(circleImageView);
    }

    private void setDefaultProfileImage(CircleImageView circleImageView) {
        circleImageView.setImageDrawable(
                ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_person_black_24dp));
    }

    @Override
    public int getItemCount() {
        return keys.size();
    }

    static class ListingHolder
            extends RecyclerView.ViewHolder {
        final CircleImageView profile;
        final ImageView imageView;
        final TextView title;
        final TextView desc;
        final TextView dist;
        final TextView timeStampTime;
        final TextView timeStampDay;

        ListingHolder(View itemView) {
            super(itemView);
            profile = (CircleImageView) itemView.findViewById(R.id.item_prof_img);
            imageView = (ImageView) itemView.findViewById(R.id.item_image);
            title = (TextView) itemView.findViewById(R.id.item_title);
            desc = (TextView) itemView.findViewById(R.id.item_desc);
            dist = (TextView) itemView.findViewById(R.id.item_distance);
            timeStampTime = (TextView) itemView.findViewById(R.id.item_timestamp_time);
            timeStampDay = (TextView) itemView.findViewById(R.id.item_timestamp_day);
        }
    }
}
