package com.rethink.drop.adapters;

import android.graphics.Bitmap;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.rethink.drop.MainActivity;
import com.rethink.drop.R;
import com.rethink.drop.models.Listing;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class ListingsAdapter
        extends RecyclerView.Adapter<ListingsAdapter.ListingHolder> {
    private ArrayList<String> keys;
    private HashMap<String, Boolean> loadedListings;
    private HashMap<String, Boolean> loadedImages;
    private HashMap<String, Bitmap> imageBitmaps;
    private HashMap<String, Listing> listings;

    public ListingsAdapter(ArrayList<String> keys,
                           HashMap<String, Listing> listings,
                           HashMap<String, Bitmap> imageBitmaps,
                           HashMap<String, Boolean> loadedListings) {
        this.keys = keys;
        this.listings = listings;
        this.imageBitmaps = imageBitmaps;
        this.loadedListings = loadedListings;
        loadedImages = new HashMap<>();
    }

    @Override
    public ListingHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                               .inflate(R.layout.listing, parent, false);
        return new ListingHolder(v);
    }

    @Override
    public void onBindViewHolder(final ListingHolder holder, final int position) {
        final String key = keys.get(position);
        final Listing listing = listings.get(key);

        holder.title.setText(listing.getTitle());
        ViewCompat.setTransitionName(holder.title, "title_" + key);

        holder.desc.setText(listing.getDescription());
        ViewCompat.setTransitionName(holder.desc, "desc_" + key);

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        holder.timeStampDay.setText(sdf.format(listing.getTimestamp()));

        sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        holder.timeStampTime.setText(sdf.format(listing.getTimestamp()));

        if (imageBitmaps.get(key) != null) {
            setImageView(holder.imageView, key);
        }
        ViewCompat.setTransitionName(holder.imageView, "image_" + key);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) holder.itemView.getContext()).viewListing(
                        holder.itemView,
                        keys.get(holder.getAdapterPosition()));
            }
        });
    }


    private void setImageView(final ImageView imageView, String key) {
        if (loadedListings.get(key)) {
            imageView.setImageBitmap(imageBitmaps.get(key));
            if (loadedImages.get(key) == null) {
                final Animation imageIn = AnimationUtils.loadAnimation(imageView.getContext(), R.anim.slide_fade_in);
                imageIn.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        imageView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                imageView.startAnimation(imageIn);
                loadedImages.put(key, true);
            } else {
                imageView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return listings.size();
    }

    static class ListingHolder
            extends RecyclerView.ViewHolder {
        final ImageView imageView;
        final TextView title;
        final TextView desc;
        final TextView timeStampTime;
        final TextView timeStampDay;

        ListingHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.item_image);
            title = (TextView) itemView.findViewById(R.id.item_title);
            desc = (TextView) itemView.findViewById(R.id.item_desc);
            timeStampTime = (TextView) itemView.findViewById(R.id.item_timestamp_time);
            timeStampDay = (TextView) itemView.findViewById(R.id.item_timestamp_day);
        }
    }
}
