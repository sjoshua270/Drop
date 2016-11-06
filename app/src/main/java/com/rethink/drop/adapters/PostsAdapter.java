package com.rethink.drop.adapters;

import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
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
import com.rethink.drop.models.Post;
import com.rethink.drop.models.Profile;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.rethink.drop.DataManager.keys;
import static com.rethink.drop.DataManager.listings;
import static com.rethink.drop.Utilities.distanceInKilometers;
import static com.rethink.drop.Utilities.distanceInMiles;
import static com.rethink.drop.Utilities.getDistanceString;
import static com.rethink.drop.Utilities.useMetric;

public class PostsAdapter
        extends RecyclerView.Adapter<PostsAdapter.ListingHolder> {
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
        final Post post = listings.get(key);

        holder.imageView.setPadding(0, 0, 0, 0);
        String imageUrl = post.getImageURL() == null ? "" : post.getImageURL();
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
                   .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.drawable.ic_photo_camera_white_24px);
            int padding = holder.itemView.getContext()
                                         .getResources()
                                         .getDimensionPixelSize(
                                                 R.dimen.listing_padding);
            holder.imageView.setPadding(padding, padding, padding, padding);
        }

        FirebaseDatabase.getInstance()
                        .getReference()
                        .child("profiles")
                        .child(post.getUserID())
                        .addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Profile profile = dataSnapshot.getValue(Profile.class);
                                if (profile != null) {
                                    String imageUrl = profile.getImageURL() == null ? "" : profile.getImageURL();
                                    Picasso.with(context)
                                           .load(imageUrl)
                                           .placeholder(R.drawable.ic_photo_camera_white_24px)
                                           .resize(context.getResources()
                                                          .getDimensionPixelSize(R.dimen.listing_prof_dimen),
                                                   context.getResources()
                                                          .getDimensionPixelSize(R.dimen.listing_prof_dimen))
                                           .centerCrop()
                                           .into(holder.profile);
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

        holder.title.setText(post.getTitle());
        ViewCompat.setTransitionName(holder.title, "title_" + key);

        holder.desc.setText(post.getDescription());
        ViewCompat.setTransitionName(holder.desc, "desc_" + key);

        double distance;
        if (useMetric(Locale.getDefault())) {
            distance = distanceInKilometers(
                    MainActivity.userLocation.latitude, post.getLatitude(),
                    MainActivity.userLocation.longitude, post.getLongitude()
            );
        } else {
            distance = distanceInMiles(
                    MainActivity.userLocation.latitude, post.getLatitude(),
                    MainActivity.userLocation.longitude, post.getLongitude()
            );
        }
        distance = Math.round(distance * 100);
        distance /= 100;
        String formatString = getDistanceString(Locale.getDefault());
        holder.dist.setText(String.format(formatString, String.valueOf(distance)));

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        holder.timeStampDay.setText(sdf.format(post.getTimestamp()));

        sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        holder.timeStampTime.setText(sdf.format(post.getTimestamp()));

        ViewCompat.setTransitionName(holder.imageView, "image_" + key);
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