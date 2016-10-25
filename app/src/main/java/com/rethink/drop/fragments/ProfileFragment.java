package com.rethink.drop.fragments;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.rethink.drop.R;
import com.rethink.drop.models.Profile;

public class ProfileFragment
        extends Fragment {
    public static final String USER_ID = "user_id";
    ImageView profImage;
    TextView profName;
    TextView profNameEdit;
    private Profile profile;

    public static ProfileFragment newInstance(@Nullable String userID) {

        Bundle args = new Bundle();
        args.putString(USER_ID, userID);
        ProfileFragment fragment = new ProfileFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String userID = getArguments().getString(USER_ID);
        if (userID != null) {
            DatabaseReference ref = FirebaseDatabase.getInstance()
                                                    .getReference()
                                                    .child("profiles")
                                                    .child(userID);
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    profile = dataSnapshot.getValue(Profile.class);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } else {
            Toast.makeText(getContext(), "Fail", Toast.LENGTH_LONG).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);
        profImage = (ImageView) v.findViewById(R.id.prof_img);
        profName = (TextView) v.findViewById(R.id.prof_name);
        profNameEdit = (TextView) v.findViewById(R.id.prof_name_edit);

        if (profile != null) {
            profNameEdit.setVisibility(View.GONE);
            if (!profile.getIconURL().equals("")) {
                FirebaseStorage.getInstance().getReferenceFromUrl(profile.getIconURL())
                               .getBytes(1024 * 1024)
                               .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                   @Override
                                   public void onSuccess(byte[] bytes) {
                                       Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                       setImageView(profImage, bmp);
                                   }
                               });
            }
            profName.setText(profile.getName());
        } else {
            profName.setVisibility(View.GONE);
        }

        return v;
    }

    private void setImageView(final ImageView imageView, final Bitmap image) {
        final Animation imageIn = AnimationUtils.loadAnimation(imageView.getContext(), R.anim.grow_fade_in);
        imageView.setImageBitmap(image);
        imageView.startAnimation(imageIn);
    }
}
