package com.rethink.drop.adapters;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rethink.drop.MainActivity;
import com.rethink.drop.R;
import com.rethink.drop.models.Drop;
import com.rethink.drop.models.Profile;

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
        ViewCompat.setTransitionName(holder.desc, "desc_" + key);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    MainActivity.getInstance().openListing(
                            holder.itemView,
                            ((BitmapDrawable) holder.imageView.getDrawable()).getBitmap(),
                            keys.get(holder.getAdapterPosition()));
                } catch (ClassCastException | NullPointerException e) {
                    MainActivity.getInstance().openListing(
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
                                    holder.desc.setText(drop.getText());
                                } else {
                                    GeoFire geoFire = new GeoFire(
                                            FirebaseDatabase.getInstance()
                                                            .getReference()
                                                            .child("geoFire"));
                                    geoFire.removeLocation(key);
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
            Glide.with(context)
                 .load(imageUrl)
                 .centerCrop()
                 .placeholder(R.drawable.ic_photo_camera_white_24px)
                 .crossFade()
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

    private void getProfileImage(Drop drop, final ImageView profImageView) {
        final String userID = drop.getUserID();
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
                                        Glide.with(context)
                                             .load(imageUrl)
                                             .centerCrop()
                                             .placeholder(R.drawable.ic_photo_camera_white_24px)
                                             .crossFade()
                                             .into(profImageView);
                                    } else {
                                        setDefaultProfileImage(userID, profImageView);
                                    }
                                } else {
                                    setDefaultProfileImage(userID, profImageView);
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
    }

    private void setDefaultProfileImage(String userID, ImageView profImageView) {
        int[] profColors = context.getResources().getIntArray(R.array.prof_colors);
        int firstCharValue = (int) userID.charAt(0);
        if (47 < firstCharValue && firstCharValue <= 57){
            firstCharValue -= 48;
        }
        else if (64 < firstCharValue && firstCharValue <= 90){
            firstCharValue -= 55;
        }
        else if (96 < firstCharValue && firstCharValue <= 122){
            firstCharValue -= 87;
        }
        int colorIndex = firstCharValue / profColors.length;
        profImageView.setBackgroundColor(profColors[colorIndex]);
        profImageView.setImageDrawable(
                ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_face_white_24px));
    }

    @Override
    public int getItemCount() {
        return keys.size();
    }

    static class ListingHolder
            extends RecyclerView.ViewHolder {
        final ImageView profile;
        final ImageView imageView;
        final TextView desc;
        final TextView dist;
        final TextView timeStampTime;
        final TextView timeStampDay;

        ListingHolder(View itemView) {
            super(itemView);
            profile = (ImageView) itemView.findViewById(R.id.item_prof_img);
            imageView = (ImageView) itemView.findViewById(R.id.item_image);
            desc = (TextView) itemView.findViewById(R.id.item_desc);
            dist = (TextView) itemView.findViewById(R.id.item_distance);
            timeStampTime = (TextView) itemView.findViewById(R.id.item_timestamp_time);
            timeStampDay = (TextView) itemView.findViewById(R.id.item_timestamp_day);
        }
    }
}
