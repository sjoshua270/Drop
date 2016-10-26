package com.rethink.drop.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.rethink.drop.R;
import com.rethink.drop.models.Listing;

import static com.rethink.drop.DataManager.imageBitmaps;
import static com.rethink.drop.DataManager.listings;
import static com.rethink.drop.models.Listing.KEY;

public class ImageFragment
        extends Fragment {
    Bitmap imageBitmap;
    ImageView imageView;

    public static ImageFragment newInstance(String key) {
        Bundle args = new Bundle();
        args.putString(KEY, key);
        ImageFragment fragment = new ImageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_image, container, false);
        imageView = (ImageView) v.findViewById(R.id.full_image);
        imageView.setImageBitmap(imageBitmap);
        getPhoto(getArguments().getString(KEY));

        ViewCompat.setTransitionName(imageView, "image");
        return v;
    }

    private void getPhoto(final String key) {
        Listing listing = listings.get(key);
        FirebaseStorage.getInstance().getReferenceFromUrl(listing.getImageURL())
                       .getBytes(4 * (1024 * 1024))
                       .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                           @Override
                           public void onSuccess(final byte[] bytes) {
                               imageView.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                           }
                       });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        imageBitmap = imageBitmaps.get(args.getString(KEY));
    }
}
