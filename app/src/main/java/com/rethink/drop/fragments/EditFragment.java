package com.rethink.drop.fragments;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.UploadTask;
import com.rethink.drop.R;
import com.rethink.drop.Utilities;
import com.rethink.drop.models.Listing;

import java.io.IOException;
import java.util.Calendar;

import static android.app.Activity.RESULT_OK;
import static com.rethink.drop.MainActivity.degreesPerMile;
import static com.rethink.drop.MainActivity.userLocation;

public class EditFragment
        extends ListingFragment
        implements OnMapReadyCallback {

    private static final int GALLERY_REQUEST = 1;
    private boolean imageChanged;
    private CoordinatorLayout cLayout;
    private DatabaseReference ref;
    private FirebaseUser user;
    private GoogleMap googleMap;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        imageChanged = false;
        user = FirebaseAuth
                .getInstance()
                .getCurrentUser();
        ref = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("listings");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable
            Bundle savedInstanceState) {
        fragmentView = super.onCreateView(inflater, container, savedInstanceState);

        title.setVisibility(View.GONE);
        desc.setVisibility(View.GONE);

        if (imageIcon == null) {
            imageView.setImageResource(R.drawable.ic_photo_camera_white_24px);
            float scale = getResources().getDisplayMetrics().density;
            int dpAsPixels = (int) (40 * scale + 0.5f);
            imageView.setPadding(dpAsPixels, dpAsPixels, dpAsPixels, dpAsPixels);
        }
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(
                        getContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    getImage();
                } else {
                    ActivityCompat.requestPermissions(
                            getActivity(),
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            0);
                }
            }
        });

        cLayout = (CoordinatorLayout) getActivity().findViewById(R.id.coordinator);

        return fragmentView;
    }

    public void getImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), GALLERY_REQUEST);
    }

    /**
     * Handles the response from the camera app
     *
     * @param requestCode Code to ensure that the result was meant for me
     * @param resultCode  Code to say everything went alright
     * @param data        Actual data to process into an image
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GALLERY_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                try {
                    Uri selectedImageUri = data.getData();
                    Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), selectedImageUri);
                    imageHighRes = Utilities.compressImage(imageBitmap);
                    imageIcon = Utilities.generateIcon(imageBitmap);
                    imageChanged = true;
                    setImageView();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Snackbar.make(cLayout, "No Image is selected.", Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Takes all values in the current layout and sends them off to Firebase,
     * then returns to the previous fragment
     */
    public void publishListing() {
        String filename = inputTitle.getText()
                                    .toString()
                                    .replaceAll("[^A-Za-z]+", "")
                                    .toLowerCase();
        if (filename.equals("")) {
            Snackbar.make(cLayout, R.string.title_cannot_be_empty, Snackbar
                    .LENGTH_LONG)
                    .show();
        } else {
            String key = getArguments().getString("KEY");
            /* If the listing exists, and the block Location is different than before,
            remove the original listing from its block. This ensures no leftover listings
            because of editing a listing in a different location than the original posting. */
            if (key != null) {
                int blockNumber = getBlockLocation(userLocation.latitude);
                if (blockNumber != getBlockLocation(listing.getLatitude())) {
                    ref.child(String.valueOf(getBlockLocation(listing.getLatitude())))
                       .child(String.valueOf(getBlockLocation(listing.getLongitude())))
                       .child(key)
                       .removeValue();
                }
            }
            ref = ref.child(String.valueOf(getBlockLocation(userLocation.latitude)))
                     .child(String.valueOf(getBlockLocation(userLocation.longitude)));
            if (key == null) {
                key = ref.push()
                         .getKey();
            }
            ref = ref.child(key);
            if (imageIcon != null && imageChanged) {
                uploadIcon(key, filename);
            } else {
                ref.setValue(
                        new Listing(
                                user.getUid(),
                                Calendar.getInstance()
                                        .getTimeInMillis(),
                                listing != null ? listing.getIconURL() : "",
                                listing != null ? listing.getImageURL() : "",
                                inputTitle.getText()
                                          .toString(),
                                inputDesc.getText()
                                         .toString(),
                                userLocation.latitude,
                                userLocation.longitude));
                getActivity().getSupportFragmentManager()
                             .popBackStackImmediate();
            }
        }
    }

    private void uploadIcon(final String key, final String filename) {
        UploadTask uploadIcon = Utilities.uploadImage(imageIcon, user.getUid() + "/" + key + "/" + filename + "_icon");
        uploadIcon.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Uri iconUrl = taskSnapshot.getDownloadUrl();
                if (iconUrl != null) {
                    uploadImage(key, filename, iconUrl.toString());
                } else {
                    Snackbar.make(cLayout, R.string.unexpected_error, Snackbar.LENGTH_LONG).show();
                }
            }
        });

    }

    private void uploadImage(String key, String filename, final String iconURL) {
        // Prepare a progress bar
        final ProgressDialog progressDialog = new ProgressDialog
                (getActivity());
        progressDialog.setMax(100);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setTitle(R.string.uploading);
        progressDialog.setCancelable(false);
        progressDialog.show();

        UploadTask uploadImage = Utilities.uploadImage(imageHighRes, user.getUid() + "/" + key + "/" + filename);
        uploadImage.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                          @Override
                          public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                              float progress = 100f * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount();
                              progressDialog.setProgress((int) progress);
                          }
                      })
                      .addOnFailureListener(new OnFailureListener() {
                          @Override
                          public void onFailure(@NonNull Exception e) {
                              Snackbar.make(cLayout, R.string.failed_to_upload, Snackbar.LENGTH_LONG).show();
                          }
                      })
                      .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                          @Override
                          public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                              Uri downloadUrl = taskSnapshot.getDownloadUrl();
                              if (downloadUrl != null) {
                                  ref.setValue(new Listing(
                                          user.getUid(),
                                          Calendar.getInstance().getTimeInMillis(),
                                          iconURL,
                                          downloadUrl.toString(),
                                          inputTitle.getText().toString(),
                                          inputDesc.getText().toString(),
                                          userLocation.latitude,
                                          userLocation.longitude));
                                  progressDialog.cancel();
                                  getActivity().getSupportFragmentManager().popBackStackImmediate();
                              } else {
                                  Snackbar.make(cLayout, R.string.unexpected_error, Snackbar.LENGTH_LONG).show();
                              }
                          }
                      });
    }

    private int getBlockLocation(double latitude) {
        return (int) (latitude / degreesPerMile);
    }

    public void updateMapPin() {
        googleMap.clear();
        googleMap.addMarker(new MarkerOptions()
                .position(userLocation)
                .title("You are here"));
        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder()
                              .target(userLocation)
                              .zoom(20f)
                              .build()));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        updateMapPin();
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                userLocation = latLng;
                updateMapPin();
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_edit, menu);
    }

    // Life cycle events


    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
}
