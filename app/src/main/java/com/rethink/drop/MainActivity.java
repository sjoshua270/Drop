package com.rethink.drop;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.firebase.geofire.GeoLocation;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rethink.drop.fragments.DropFragment;
import com.rethink.drop.fragments.DropMapFragment;
import com.rethink.drop.fragments.LocalFragment;
import com.rethink.drop.fragments.ProfileFragment;
import com.rethink.drop.tools.FragmentJuggler;

import static com.rethink.drop.models.Drop.KEY;
import static com.rethink.drop.tools.FragmentJuggler.CURRENT;
import static com.rethink.drop.tools.FragmentJuggler.IMAGE;
import static com.rethink.drop.tools.FragmentJuggler.LISTING;
import static com.rethink.drop.tools.FragmentJuggler.LOCAL;
import static com.rethink.drop.tools.FragmentJuggler.MAP;
import static com.rethink.drop.tools.FragmentJuggler.PROFILE;

public class MainActivity extends AppCompatActivity implements OnConnectionFailedListener,
                                                               ConnectionCallbacks,
                                                               LocationListener {
    public static final int RC_SIGN_IN = 1;
    public static final String EDITING = "editing";
    private final static float degreesPerMile = 0.01449275362f;
    public static MainActivity instance;
    public static LatLng userLocation;
    private static GoogleApiClient googleApiClient;
    private final int LOCATION_REQUEST = 2;
    public static final int STORAGE_REQUEST = 3;
    private final String STATE_FRAGMENT = "state_fragment";
    private final String STATE_KEY = "state_key";
    private final String STATE_LAT = "state_latitude";
    private final String STATE_LON = "state_longitude";
    private FabManager fab;
    private static FragmentJuggler fragmentJuggler;
    private DataManager dataManager;

    public static MainActivity getInstance() {
        if (instance != null) {
            return instance;
        } else {
            return new MainActivity();
        }
    }

    public static LatLng getUserLocation() {
        return userLocation;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;

        googleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                                                           .addOnConnectionFailedListener(this)
                                                           .addApi(LocationServices.API)
                                                           .build();
        dataManager = new DataManager();
        fragmentJuggler = new FragmentJuggler(getSupportFragmentManager());
        fab = new FabManager(this,
                             (FloatingActionButton) findViewById(R.id.fab));
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            openFragment(LOCAL,
                         null);
        }

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        setFabListener((FloatingActionButton) findViewById(R.id.fab));
        setBackStackListener();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Bundle args = new Bundle();
        args.putString(KEY,
                       savedInstanceState.getString(STATE_KEY));
        openFragment(savedInstanceState.getInt(STATE_FRAGMENT),
                     args);
        userLocation = new LatLng(savedInstanceState.getDouble(STATE_LAT),
                                  savedInstanceState.getDouble(STATE_LON));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_FRAGMENT,
                        CURRENT);
        outState.putString(STATE_KEY,
                           fragmentJuggler.getCurrentFragment()
                                          .getArguments()
                                          .getString(KEY));
        outState.putDouble(STATE_LAT,
                           userLocation.latitude);
        outState.putDouble(STATE_LON,
                           userLocation.longitude);
        super.onSaveInstanceState(outState);
    }

    private void setFabListener(final FloatingActionButton fab) {
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CURRENT == LOCAL) {
                    if (FirebaseAuth.getInstance()
                                    .getCurrentUser() != null) {
                        openFragment(LISTING,
                                     null);
                    } else {
                        ((LocalFragment) fragmentJuggler.getCurrentFragment()).handleFabPress();
                    }
                } else if (CURRENT == LISTING) {
                    ((DropFragment) fragmentJuggler.getCurrentFragment()).editDrop();
                } else if (CURRENT == PROFILE) {
                    ((ProfileFragment) fragmentJuggler.getCurrentFragment()).handleFabPress();
                }
            }
        });
    }

    private void openFragment(int id, Bundle args) {
        fragmentJuggler.setMainFragment(id,
                                        args);
        if (findViewById(R.id.sub_fragment_container).getVisibility() == View.VISIBLE) {
            findViewById(R.id.sub_fragment_container).setVisibility(View.GONE);
        }
    }

    public void showMessage(final String message){
        Snackbar.make(findViewById(R.id.fab), message, Snackbar.LENGTH_LONG).show();
    }

    public void syncUI() {
        syncUpNav();
        fab.update();
    }

    public void dismissKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View focused = getCurrentFocus();
        if (focused != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                                        0);
        }
    }

    private void setBackStackListener() {
        getSupportFragmentManager().addOnBackStackChangedListener(new OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    Fragment frag = getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);
                    if (frag != null) {
                        Class currClass = frag.getClass();
                        if (currClass.equals(LocalFragment.class)) {
                            CURRENT = LOCAL;
                        }
                        if (currClass.equals(DropFragment.class)) {
                            CURRENT = LISTING;
                        }
                        if (currClass.equals(ProfileFragment.class)) {
                            CURRENT = PROFILE;
                        }
                        syncUI();
                    }
                } else {
                    finish();
                }
            }
        });
    }

    public void notifyDropAdded(String key) {
        try {
            Fragment fragment = fragmentJuggler.getCurrentFragment();
            Class fClass = fragment.getClass();
            if (fClass.equals(LocalFragment.class)) {
                ((LocalFragment) fragment).notifyDropInserted(key);
            } else if (fClass.equals(DropMapFragment.class)) {
                ((DropMapFragment) fragment).notifyDropInserted(key);
            }
        } catch (ClassCastException ignored) {
        }

    }

    public void notifyDropRemoved(String key) {
        Fragment fragment = fragmentJuggler.getCurrentFragment();
        Class fragmentClass = fragment.getClass();
        if (fragmentClass.equals(LocalFragment.class)) {
            ((LocalFragment) fragment).notifyDropRemoved(key);
        } else if (fragmentClass.equals(DropMapFragment.class)) {
            ((DropMapFragment) fragment).removeDrop(key);
        }
    }

    private void syncUpNav() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount() > 1);
        }
    }

    public void openListing(View listingView, String key) {
        fragmentJuggler.viewListing(listingView,
                                    key);
        if (findViewById(R.id.sub_fragment_container).getVisibility() != View.VISIBLE) {
            findViewById(R.id.sub_fragment_container).setVisibility(View.VISIBLE);
        }
    }

    public void openProfile(View profile, String userID) {
        fragmentJuggler.viewProfile(profile,
                                    userID);
    }

    public void viewImage(String key) {
        Bundle args = new Bundle();
        args.putString(KEY,
                       key);
        openFragment(IMAGE,
                     args);
    }

    public static void scrollToDrop(String key) {
        LocalFragment localFragment = (LocalFragment) fragmentJuggler.getCurrentFragment();
        localFragment.scrollToDrop(key);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,
                               resultCode,
                               data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                openFragment(LISTING,
                             null);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);
        Class fragmentClass = fragment.getClass();
        int optionID = item.getItemId();

        if (optionID == R.id.toggle_map) {
            Bundle args = new Bundle();
            args.putDouble("LAT",
                           userLocation.latitude);
            args.putDouble("LNG",
                           userLocation.longitude);
            openFragment(MAP,
                         args);
        }

        if (fragmentClass.equals(LocalFragment.class)) {
            switch (optionID) {
                case R.id.open_profile:
                    openFragment(PROFILE,
                                 null);
                    break;
            }
        }
        if (fragmentClass.equals(DropFragment.class)) {
            DropFragment dropFragment = (DropFragment) fragment;
            String key = getSupportFragmentManager().findFragmentById(R.id.main_fragment_container)
                                                    .getArguments()
                                                    .getString(KEY);
            switch (optionID) {
                case R.id.delete_drop:
                    if (key != null) {
                        DatabaseReference ref = FirebaseDatabase.getInstance()
                                                                .getReference();
                        ref.child("posts")
                           .child(key)
                           .removeValue();
                        ref.child("geoFire")
                           .child(key)
                           .removeValue();
                    }
                    while (getSupportFragmentManager().getBackStackEntryCount() > 1) {
                        getSupportFragmentManager().popBackStackImmediate();
                    }
                    break;
                case R.id.submit_drop:
                    dropFragment.publishDrop();
                    break;
                case R.id.save_drop:
                    dropFragment.publishDrop();
                    break;
                case R.id.edit_drop:
                    dropFragment.editDrop();
                    break;
            }
        } else if (fragmentClass.equals(ProfileFragment.class)) {
            ProfileFragment profileFragment = (ProfileFragment) fragment;
            switch (optionID) {
                case R.id.log_out:
                    AuthUI.getInstance()
                          .signOut(this)
                          .addOnCompleteListener(new OnCompleteListener<Void>() {
                              @Override
                              public void onComplete(@NonNull Task<Void> task) {
                                  while (getSupportFragmentManager().getBackStackEntryCount() > 1) {
                                      getSupportFragmentManager().popBackStackImmediate();
                                  }
                              }
                          });
                    break;
                case R.id.save_profile:
                    profileFragment.saveProfile();
                    break;
                case R.id.edit_profile:
                    profileFragment.editProfile();
                    break;
                }
        }
        if (optionID == android.R.id.home) {
                    getSupportFragmentManager().popBackStackImmediate();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this,
                                               android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            askForLocationPermission();
        } else {
            startLocationUpdates();
        }
    }

    private void askForLocationPermission() {
        ActivityCompat.requestPermissions(this,
                                          new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                          LOCATION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_REQUEST) {
            if (grantResults.length > 0) {
                startLocationUpdates();
            }
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                                               android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location loc = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if (loc != null) {
                userLocation = new LatLng(loc.getLatitude(),
                                          loc.getLongitude());
                updateLocation();
            }
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(5000);
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,
                                                                     locationRequest,
                                                                     this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (ActivityCompat.checkSelfPermission(this,
                                               android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            userLocation = new LatLng(location.getLatitude(),
                                      location.getLongitude());
            updateLocation();
        }
    }

    private void updateLocation() {
        if (userLocation != null) {
            dataManager.updateLocation(new GeoLocation(userLocation.latitude,
                                                       userLocation.longitude));
            dataManager.attachListeners();
        }
    }

    private void stopLocationUpdates() {
        if (googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient,
                                                                    this);
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
        updateLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        dataManager.detachListeners();
        stopLocationUpdates();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (googleApiClient != null) {
            googleApiClient.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        dataManager.detachLocationListener();
        super.onDestroy();
    }
}
