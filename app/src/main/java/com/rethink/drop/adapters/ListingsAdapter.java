package com.rethink.drop.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
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
    private FirebaseStorage firebaseStorage;
    private HashMap<String, Boolean> imageDownloaded;
    private HashMap<String, Boolean> imageDisplayed;
    private HashMap<String, Bitmap> imageBitmaps;
    private HashMap<String, Listing> listings;

    public ListingsAdapter(ArrayList<String> keys,
                           HashMap<String, Listing> listings,
                           HashMap<String, Bitmap> imageBitmaps,
                           HashMap<String, Boolean> imageDownloaded) {
        this.keys = keys;
        this.listings = listings;
        this.imageBitmaps = imageBitmaps;
        this.imageDownloaded = imageDownloaded;
        imageDisplayed = new HashMap<>();
        firebaseStorage = FirebaseStorage.getInstance();
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

        if (imageDownloaded.get(key) == null) {
            getPhoto(key, listing);
        }

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

    private void getPhoto(final String key, Listing listing) {
        if (listing.getImageURL() != null) {
            imageDownloaded.put(key, false);
            if (!listing.getImageURL().equals("")) {
                firebaseStorage.getReferenceFromUrl(listing.getImageURL())
                               .getBytes(4 * (1024 * 1024))
                               .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                   @Override
                                   public void onSuccess(final byte[] bytes) {
                                       Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                       int dimens = Math.min(bmp.getWidth(), bmp.getHeight());
                                       imageBitmaps.put(key, Bitmap.createBitmap(bmp, 0, 0, dimens, dimens));
                                       imageDownloaded.put(key, true);
                                       ListingsAdapter.this.notifyDataSetChanged();
                                   }
                               });
            }
        }
    }


    private void setImageView(final ImageView imageView, String key) {
        if (imageDownloaded.get(key)) {
            imageView.setImageBitmap(imageBitmaps.get(key));
            if (imageDisplayed.get(key) == null) {
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
                imageDisplayed.put(key, true);
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
