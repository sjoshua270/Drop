package com.rethink.drop.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.rethink.drop.R;
import com.rethink.drop.exceptions.FragmentArgsMismatch;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

public class ImageFragment extends Fragment {
    public final static String IMAGE_URL = "image_url";

    public static ImageFragment newInstance(Bundle args) throws FragmentArgsMismatch {
        String imageUrl = args.getString(IMAGE_URL);
        if (imageUrl == null || imageUrl.equals("")) {
            throw new FragmentArgsMismatch("imageUrl was null or empty");
        }
        ImageFragment fragment = new ImageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater,
                           container,
                           savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_image,
                                  container,
                                  false);
        ImageView imageView = v.findViewById(R.id.full_image);
        String imageUrl = getArguments().getString(IMAGE_URL);
        if (imageUrl != null && !imageUrl.equals("")) {
            Glide.with(ImageFragment.this)
                 .load(imageUrl)
                 .transition(withCrossFade())
                 .into(imageView);
        }
        return v;
    }
}
