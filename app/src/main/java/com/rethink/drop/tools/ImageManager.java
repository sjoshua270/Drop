package com.rethink.drop.tools;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.rethink.drop.MainActivity;
import com.rethink.drop.R;
import com.rethink.drop.interfaces.ImageRecipient;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;
import static com.rethink.drop.MainActivity.STORAGE_REQUEST;

public class ImageManager extends Fragment {
    public static final int GALLERY_REQUEST = 3;
    private static final String TAG = "ImageManager";
    private static final int CAMERA_CAPTURE = 1;

    public void requestImage(ImageRecipient recipient) {
        getArguments().putSerializable("recipient",
                                       recipient.getClass());
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
                MainActivity.getInstance()
                            .showMessage(getString(R.string.unexpected_error));
                Log.e("ImageManager.takePic",
                      ex.getMessage());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                      FileProvider.getUriForFile(MainActivity.getInstance(),
                                                                 MainActivity.getInstance()
                                                                             .getApplicationContext()
                                                                             .getPackageName() + ".provider",
                                                                 photoFile));
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
        getArguments().putString("pic_path",
                                 "file://" + image.getAbsolutePath());
        return image;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_CAPTURE) {
            if (resultCode == RESULT_OK) {
                performCrop(Uri.parse(getArguments().getString("pic_path")));
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
                Class recipient = (Class) getArguments().getSerializable("recipient");
                ImageRecipient imageRecipient = MainActivity.getImageRecipient(recipient);
                if (imageRecipient != null) {
                    imageRecipient.receiveImage(resultUri.toString());
                } else {
                    Log.e("onActivityResult",
                          "No ImageRecipient");
                }
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
}