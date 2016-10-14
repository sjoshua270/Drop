package com.rethink.drop.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.rethink.drop.DataManager;
import com.rethink.drop.R;

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

        ViewCompat.setTransitionName(imageView, "image");
        return v;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        imageBitmap = DataManager.imageBitmaps.get(args.getString(KEY));
    }
}
