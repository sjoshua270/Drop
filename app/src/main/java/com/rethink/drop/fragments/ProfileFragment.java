package com.rethink.drop.fragments;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import com.rethink.drop.Utilities;
import com.rethink.drop.models.Profile;

import java.io.IOException;

import static android.app.Activity.RESULT_OK;

public class ProfileFragment
        extends Fragment {
    public static final String USER_ID = "user_id";
    private static final int GALLERY_REQUEST = 2;
    private Bitmap imageHighRes;
    private Bitmap imageIcon;
    private DatabaseReference ref;
    private TextView profName;
    private TextView profNameEdit;
    private Boolean editing;
    private Boolean imageChanged;
    private ImageView profImage;
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
            ref = FirebaseDatabase.getInstance()
                                  .getReference()
                                  .child("profiles")
                                  .child(userID);
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    profile = dataSnapshot.getValue(Profile.class);
                    editing = false;
                    prepViews();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } else {
            Toast.makeText(getContext(), "Fail", Toast.LENGTH_LONG).show();
        }
        editing = false;
        imageChanged = false;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);
        profImage = (ImageView) v.findViewById(R.id.prof_img);
        profImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getImage();
            }
        });

        profName = (TextView) v.findViewById(R.id.prof_name);
        profNameEdit = (TextView) v.findViewById(R.id.prof_name_edit);
        return v;
    }

    public void getImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), GALLERY_REQUEST);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GALLERY_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                try {
                    Uri selectedImageUri = data.getData();
                    Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), selectedImageUri);
                    imageHighRes = Utilities.compressImage(imageBitmap);
                    imageIcon = Utilities.generateIcon(imageBitmap);
                    imageChanged = true;
                    setImageView(profImage, imageIcon);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void prepViews() {
        if (!editing) {
            if (profile != null) {
                profNameEdit.setVisibility(View.GONE);
                profName.setVisibility(View.VISIBLE);
                profName.setText(profile.getName());
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
            } else {
                editing = true;
            }
        }
        if (editing) {
            profName.setVisibility(View.GONE);
            profNameEdit.setVisibility(View.VISIBLE);
            if (profile != null) {
                profNameEdit.setText(profile.getName());
            }
        }
    }

    private void setImageView(final ImageView imageView, final Bitmap image) {
        final Animation imageIn = AnimationUtils.loadAnimation(imageView.getContext(), R.anim.grow_fade_in);
        imageView.setImageBitmap(image);
        imageView.startAnimation(imageIn);
    }

    public void handleFabPress() {
        if (editing) {
            profile = new Profile(
                    getArguments().getString(USER_ID),
                    "",
                    "",
                    profNameEdit.getText().toString()
            );
            ref.setValue(profile);
        }
        toggleState();
    }

    public void toggleState() {
        editing = !editing;
        prepViews();
    }

    public boolean isEditing() {
        return editing;
    }
}
