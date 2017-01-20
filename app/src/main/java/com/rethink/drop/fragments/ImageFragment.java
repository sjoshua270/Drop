package com.rethink.drop.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rethink.drop.R;
import com.rethink.drop.models.Drop;

import static com.rethink.drop.models.Drop.KEY;

public class ImageFragment
        extends Fragment {
    private ImageView imageView;

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
        String key = getArguments().getString(KEY);
        if (key != null) {
            FirebaseDatabase.getInstance()
                            .getReference()
                            .child("posts")
                            .child(key)
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    Drop drop = dataSnapshot.getValue(Drop.class);
                                    String imageUrl = drop.getImageURL() == null ? "" : drop.getImageURL();
                                    if (!imageUrl.equals("")) {
                                        Glide.with(getContext())
                                             .load(imageUrl)
                                             .placeholder(R.drawable.ic_photo_camera_white_24px)
                                             .crossFade()
                                             .into(imageView);
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
