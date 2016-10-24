package com.rethink.drop;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
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
import com.rethink.drop.fragments.EditFragment;
import com.rethink.drop.fragments.LocalFragment;
import com.rethink.drop.fragments.ViewFragment;
import com.rethink.drop.models.Listing;

import java.util.ArrayList;
import java.util.List;

import static com.rethink.drop.DataManager.listings;
import static com.rethink.drop.FragmentJuggler.CURRENT;
import static com.rethink.drop.FragmentJuggler.EDIT;
import static com.rethink.drop.FragmentJuggler.LOCAL;
import static com.rethink.drop.FragmentJuggler.VIEW;

public class MainActivity
        extends AppCompatActivity
        implements OnConnectionFailedListener,
                   ConnectionCallbacks,
                   LocationListener {
    public final static float degreesPerMile = 0.01449275362f;
    public static LatLng userLocation;
    private static GoogleApiClient googleApiClient;
    private final int RC_SIGN_IN = 1;
    private final int LOCATION_REQUEST = 2;
    private List<DatabaseReference> databaseReferences;
    private DataManager dataManager;
    private FirebaseAuth firebaseAuth;
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
        firebaseAuth = FirebaseAuth.getInstance();

        fragmentJuggler = new FragmentJuggler(getSupportFragmentManager());
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            fragmentJuggler.openFragment(LOCAL, null);
        }

        dataManager = new DataManager();
        databaseReferences = new ArrayList<>();
        updateDBRef();

        fab = new FabManager(
                this,
                firebaseAuth,
                (FloatingActionButton) findViewById(R.id.fab),
                fragmentJuggler);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        setFabListener((FloatingActionButton) findViewById(R.id.fab));
        setBackStackListener();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void setFabListener(FloatingActionButton fab) {
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CURRENT == LOCAL) {
                    if (firebaseAuth.getCurrentUser() != null) {
                        fragmentJuggler.openFragment(EDIT, null);
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
                } else if (CURRENT == EDIT) {
                    ((EditFragment) fragmentJuggler.getCurrentFragment()).publishListing();
                } else if (CURRENT == VIEW) {
                    fragmentJuggler.viewToEditListing();
                }
            }
        });
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
                    if (currClass.equals(ViewFragment.class)) {
                        CURRENT = VIEW;
                    }
                    if (currClass.equals(EditFragment.class)) {
                        CURRENT = EDIT;
                    }
                    syncUpNav();
                    fab.update();
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

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                fragmentJuggler.openFragment(EDIT, null);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
            userLocation = new LatLng(location.getLatitude(),
                    location.getLongitude());
            try {
                ((EditFragment) fragmentJuggler.getCurrentFragment()).updateMapPin();
            } catch (ClassCastException ignored) {
            }

            updateDBRef();
        }
    }

    private void updateDBRef() {
        for (DatabaseReference databaseReference : databaseReferences) {
            dataManager.detachListeners(databaseReference);
        }
        databaseReferences.clear();
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
