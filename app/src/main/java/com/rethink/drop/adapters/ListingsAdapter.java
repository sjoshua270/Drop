package com.rethink.drop.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.content.ContextCompat;
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
    public static final int NO_IMAGE = 0;
    public static final int NOT_DOWNLOADED = 1;
    private static final int DOWNLOADED = 2;
    private static final int DISPLAYED = 3;
    private ArrayList<String> keys;
    private FirebaseStorage firebaseStorage;
    private HashMap<String, Integer> imageStatus;
    private HashMap<String, Bitmap> imageBitmaps;
    private HashMap<String, Listing> listings;

    public ListingsAdapter(ArrayList<String> keys,
                           HashMap<String, Listing> listings,
                           HashMap<String, Bitmap> imageBitmaps,
                           HashMap<String, Integer> imageStatus) {
        this.keys = keys;
        this.listings = listings;
        this.imageBitmaps = imageBitmaps;
        this.imageStatus = imageStatus;
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

        getPhoto(key, listing);

        holder.title.setText(listing.getTitle());
        ViewCompat.setTransitionName(holder.title, "title_" + key);

        holder.desc.setText(listing.getDescription());
        ViewCompat.setTransitionName(holder.desc, "desc_" + key);

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        holder.timeStampDay.setText(sdf.format(listing.getTimestamp()));

        sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        holder.timeStampTime.setText(sdf.format(listing.getTimestamp()));

        setImageView(holder.imageView, key);

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
        if (imageStatus.get(key) == NOT_DOWNLOADED) {
            firebaseStorage.getReferenceFromUrl(listing.getImageURL())
                           .getBytes(4 * (1024 * 1024))
                           .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                               @Override
                               public void onSuccess(final byte[] bytes) {
                                   Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                   int dimens = Math.min(bmp.getWidth(), bmp.getHeight());
                                   imageBitmaps.put(key, Bitmap.createBitmap(bmp, 0, 0, dimens, dimens));
                                   imageStatus.put(key, DOWNLOADED);
                                   ListingsAdapter.this.notifyItemChanged(keys.indexOf(key));
                               }
                           });
        }
    }


    private void setImageView(final ImageView imageView, final String key) {
        switch (imageStatus.get(key)) {
            case NO_IMAGE:
                setImagePlaceholder(imageView);
                break;
            case NOT_DOWNLOADED:
                setImagePlaceholder(imageView);
                break;
            case DOWNLOADED:
                final Animation imageIn = AnimationUtils.loadAnimation(imageView.getContext(), R.anim.slide_fade_in);
                final Animation imageOut = AnimationUtils.loadAnimation(imageView.getContext(), R.anim.shrink_fade_out);
                imageIn.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                imageOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        imageView.setPadding(0, 0, 0, 0);
                        imageView.setImageBitmap(imageBitmaps.get(key));
                        imageView.startAnimation(imageIn);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                imageView.startAnimation(imageOut);
                imageStatus.put(key, DISPLAYED);
                break;
            case DISPLAYED:
                imageView.setPadding(0, 0, 0, 0);
                imageView.setImageBitmap(imageBitmaps.get(key));
                break;
        }
    }

    private void setImagePlaceholder(ImageView imageView) {
        Context context = imageView.getContext();
        int padding = (int) context.getResources()
                                   .getDimension(R.dimen.item_image_padding);
        int color = ContextCompat.getColor(context, R.color.primary);
        imageView.setPadding(padding, padding, padding, padding);
        imageView.setBackgroundColor(color);
        imageView.setImageDrawable(
                ContextCompat.getDrawable(
                        imageView.getContext(),
                        R.drawable.ic_photo_camera_white_24px));
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
