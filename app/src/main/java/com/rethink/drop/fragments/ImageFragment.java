package com.rethink.drop.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.rethink.drop.R;
import com.rethink.drop.models.Post;

import static com.rethink.drop.models.Post.KEY;

public class ImageFragment
        extends Fragment {
    private SubsamplingScaleImageView imageView;

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
        imageView = (SubsamplingScaleImageView) v.findViewById(R.id.full_image);
        String key = getArguments().getString(KEY);
        if (key != null) {
            FirebaseDatabase.getInstance()
                            .getReference()
                            .child("posts")
                            .child(key)
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    Post post = dataSnapshot.getValue(Post.class);
                                    String imageUrl = post.getImageURL() == null ? "" : post.getImageURL();
                                    if (!imageUrl.equals("")) {
                                        ImageLoader imageLoader = ImageLoader.getInstance();
                                        imageLoader.init(ImageLoaderConfiguration.createDefault(getContext()));
                                        imageLoader.loadImage(imageUrl, new SimpleImageLoadingListener() {
                                            @Override
                                            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                                                imageView.setImage(ImageSource.bitmap(loadedImage));
                                            }
                                        });
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
        }
        return v;
    }

}
