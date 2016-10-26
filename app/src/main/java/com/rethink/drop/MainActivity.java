package com.rethink.drop;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rethink.drop.fragments.ListingFragment;
import com.rethink.drop.fragments.LocalFragment;
import com.rethink.drop.fragments.ProfileFragment;
import com.rethink.drop.interfaces.ImageHandler;
import com.rethink.drop.models.Listing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.rethink.drop.DataManager.listings;
import static com.rethink.drop.FragmentJuggler.CURRENT;
import static com.rethink.drop.FragmentJuggler.LISTING;
import static com.rethink.drop.FragmentJuggler.LOCAL;
import static com.rethink.drop.FragmentJuggler.PROFILE;
import static com.rethink.drop.models.Listing.KEY;

public class MainActivity
        extends AppCompatActivity
        implements OnConnectionFailedListener,
                   ConnectionCallbacks,
                   LocationListener {
    public final static float degreesPerMile = 0.01449275362f;
    public static final int GALLERY_REQUEST = 3;
    public static LatLng userLocation;
    private static GoogleApiClient googleApiClient;
    private final int RC_SIGN_IN = 1;
    private final int LOCATION_REQUEST = 2;
    private final String STATE_FRAGMENT = "state_fragment";
    private final String STATE_KEY = "state_fragment";
    private final String STATE_LAT = "state_latitude";
    private final String STATE_LON = "state_longitude";
    private List<DatabaseReference> databaseReferences;
    private DataManager dataManager;
    private FabManager fab;
    private FragmentJuggler fragmentJuggler;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userLocation = new LatLng(0.0, 0.0);
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        fragmentJuggler = new FragmentJuggler(getSupportFragmentManager());
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            fragmentJuggler.openFragment(LOCAL, null);
        }

        dataManager = new DataManager();
        databaseReferences = new ArrayList<>();

        fab = new FabManager(
                this,
                (FloatingActionButton) findViewById(R.id.fab),
                fragmentJuggler);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        setFabListener((FloatingActionButton) findViewById(R.id.fab));
        setBackStackListener();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        fragmentJuggler.openFragment(
                savedInstanceState.getInt(STATE_FRAGMENT),
                savedInstanceState.getString(STATE_KEY));
        userLocation = new LatLng(savedInstanceState.getDouble(STATE_LAT),
                savedInstanceState.getDouble(STATE_LON));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_FRAGMENT, CURRENT);
        outState.putString(STATE_KEY, fragmentJuggler.getCurrentFragment().getArguments().getString(KEY));
        outState.putDouble(STATE_LAT, userLocation.latitude);
        outState.putDouble(STATE_LON, userLocation.longitude);
        super.onSaveInstanceState(outState);
    }

    private void setFabListener(final FloatingActionButton fab) {
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CURRENT == LOCAL) {
                    if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                        fragmentJuggler.openFragment(LISTING, null);
                    } else {
                        startActivityForResult(
                                // Get an instance of AuthUI based on the default app
                                AuthUI.getInstance()
                                      .createSignInIntentBuilder()
                                      .setProviders(
                                              AuthUI.EMAIL_PROVIDER
                                              // AuthUI.GOOGLE_PROVIDER
                                      )
                                      .build(),
                                RC_SIGN_IN);
                    }
                } else if (CURRENT == LISTING) {
                    ((ListingFragment) fragmentJuggler.getCurrentFragment()).handleFabPress();
                } else if (CURRENT == PROFILE) {
                    ((ProfileFragment) fragmentJuggler.getCurrentFragment()).handleFabPress();
                }
            }
        });
    }

    public void syncUI() {
        syncUpNav();
        fab.update();
    }

    private void setBackStackListener() {
        getSupportFragmentManager().addOnBackStackChangedListener(new OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    Class currClass = getSupportFragmentManager()
                            .findFragmentById(R.id.main_fragment_container)
                            .getClass();
                    if (currClass.equals(LocalFragment.class)) {
                        CURRENT = LOCAL;
                    }
                    if (currClass.equals(ListingFragment.class)) {
                        CURRENT = LISTING;
                    }
                    if (currClass.equals(ProfileFragment.class)) {
                        CURRENT = PROFILE;
                    }
                    syncUI();
                } else {
                    finish();
                }
            }
        });
    }

    void syncUpNav() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(
                    getSupportFragmentManager().getBackStackEntryCount() > 1);
        }
    }

    public void openListing(View listingView, String key) {
        fragmentJuggler.viewListing(listingView, key);
    }

    public void viewImage(String key) {
        fragmentJuggler.viewImage(key);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                fragmentJuggler.openFragment(LISTING, null);
            }
        }
        if (requestCode == GALLERY_REQUEST) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    try {
                        Uri selectedImageUri = data.getData();
                        Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                        if (fragmentJuggler.getCurrentFragment().getClass().equals(ListingFragment.class)) {
                            ((ImageHandler) fragmentJuggler.getCurrentFragment()).OnImageReceived(imageBitmap);
                        }
                    } catch (IOException e) {
                        Snackbar.make(
                                findViewById(R.id.fab),
                                getResources().getString(R.string.unexpected_error),
                                Snackbar.LENGTH_SHORT
                        ).show();
                    }
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.open_profile:
                fragmentJuggler.openFragment(PROFILE, null);
                break;
            case R.id.delete_listing:
                String key = getSupportFragmentManager()
                        .findFragmentById(R.id.main_fragment_container)
                        .getArguments()
                        .getString("KEY");
                if (key != null) {
                    getReference(key).removeValue();
                }
                while (getSupportFragmentManager().getBackStackEntryCount() > 1) {
                    getSupportFragmentManager().popBackStackImmediate();
                }
                break;
            case android.R.id.home:
                getSupportFragmentManager().popBackStackImmediate();
                return true;
        }
        return super.onOptionsItemSelected(item);

    }

    public DatabaseReference getReference(String key) {
        Listing listing = listings.get(key);
        return FirebaseDatabase.getInstance()
                               .getReference()
                               .child("listings")
                               .child(String.valueOf((int) (listing.getLatitude() / degreesPerMile)))
                               .child(String.valueOf((int) (listing.getLongitude() / degreesPerMile)))
                               .child(key);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            askForLocationPermission();
        } else {
            startLocationUpdates();
        }
    }

    private void askForLocationPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[]
            permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_REQUEST) {
            if (grantResults.length > 0) {
                startLocationUpdates();
            }
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Location loc = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if (loc != null) {
                userLocation = new LatLng(loc.getLatitude(), loc.getLongitude());
                updateDBRef();
            }
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(5000);
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    googleApiClient,
                    locationRequest,
                    this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // If the location has changed by more than half a mile
            if (Math.abs(location.getLatitude() - userLocation.latitude) > degreesPerMile / 2 ||
                    Math.abs(location.getLongitude() - userLocation.longitude) > degreesPerMile / 2) {
                userLocation = new LatLng(
                        location.getLatitude(),
                        location.getLongitude());

                try {
                    ((ListingFragment) fragmentJuggler.getCurrentFragment()).updateMapPin();
                } catch (ClassCastException ignored) {
                }

                updateDBRef();
            }
        }
    }

    private void updateDBRef() {
        for (DatabaseReference databaseReference : databaseReferences) {
            dataManager.detachListeners(databaseReference);
        }
        databaseReferences.clear();
        dataManager.reset();
        for (int i = -2; i <= 2; i += 1) {
            for (int j = -2; j <= 2; j += 1) {
                databaseReferences.add(FirebaseDatabase.getInstance()
                                                       .getReference()
                                                       .child("listings")
                                                       .child(String.valueOf((int) (userLocation.latitude / degreesPerMile) + i))
                                                       .child(String.valueOf((int) (userLocation.longitude / degreesPerMile) + j))
                );
            }
        }
        for (DatabaseReference databaseReference : databaseReferences) {
            dataManager.attachListeners(databaseReference);
        }
    }

    private void detachAllListeners() {
        for (DatabaseReference databaseReference : databaseReferences) {
            dataManager.detachListeners(databaseReference);
        }
    }

    private void stopLocationUpdates() {
        if (googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates
                    (googleApiClient, this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (googleApiClient.isConnected()) {
            startLocationUpdates();
        }
        updateDBRef();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
        detachAllListeners();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (googleApiClient != null) {
            googleApiClient.disconnect();
        }
    }
}
