package com.rethink.drop.tools;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rethink.drop.R;
import com.rethink.drop.interfaces.ImageRecipient;
import com.rethink.drop.models.Profile;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;
import static com.rethink.drop.MainActivity.STORAGE_REQUEST;

public class ImageManager extends Fragment {
    private static final String TAG = "ImageManager";
    public static final int GALLERY_REQUEST = 3;
    private static final int CAMERA_CAPTURE = 1;
    private ImageRecipient recipient;
    private String picPath;

    public void requestImage(ImageRecipient recipient) {
        this.recipient = recipient;
        if (ActivityCompat.checkSelfPermission(getContext(),
                                               Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(getContext()).setMessage("Select image source")
                                                 .setNegativeButton("Gallery",
                                                                    new DialogInterface.OnClickListener() {
                                                                        @Override
                                                                        public void onClick(DialogInterface dialog, int which) {
                                                                            dispatchGalleryPictureIntent();
                                                                        }
                                                                    })
                                                 .setPositiveButton("Camera",
                                                                    new DialogInterface.OnClickListener() {
                                                                        @Override
                                                                        public void onClick(DialogInterface dialog, int which) {
                                                                            dispatchTakePictureIntent();
                                                                        }
                                                                    })
                                                 .show();
        } else {
            ActivityCompat.requestPermissions(getActivity(),
                                              new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                              STORAGE_REQUEST);
        }
    }

    private void dispatchGalleryPictureIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,
                                                    "Select Picture"),
                               GALLERY_REQUEST);
    }

    private void dispatchTakePictureIntent() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.i("TakePicture",
                      "IOException");
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                      Uri.fromFile(photoFile));
                startActivityForResult(cameraIntent,
                                       CAMERA_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                                                Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName,
                                         ".jpg",
                                         storageDir);

        // Save a file: path for use with ACTION_VIEW intents
        picPath = "file://" + image.getAbsolutePath();
        return image;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_CAPTURE) {
            if (resultCode == RESULT_OK) {
                performCrop(Uri.parse(picPath));
            }
        }
        if (requestCode == GALLERY_REQUEST) {
            if (resultCode == RESULT_OK) {
                Uri imageUri = data.getData();
                performCrop(imageUri);
            }
        }
        // user is returning from cropping the image
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                recipient.receiveImage(resultUri.toString());
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Log.e(TAG,
                      "Error cropping image: ",
                      error);
            }
        }
    }

    /**
     * this function does the crop operation.
     */
    private void performCrop(Uri imageUri) {
        CropImage.activity(imageUri)
                 .start(getContext(),
                        this);
    }

    public static void setProfileImage(final Context context, String userID, final ImageView imageView) {
        FirebaseDatabase.getInstance()
                        .getReference()
                        .child("profiles")
                        .child(userID)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Profile profile = dataSnapshot.getValue(Profile.class);
                                if (profile != null) {
                                    Glide.with(context)
                                         .load(profile.getImageURL())
                                         .centerCrop()
                                         .placeholder(R.drawable.ic_face_white_24px)
                                         .crossFade()
                                         .into(imageView);
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
    }
}