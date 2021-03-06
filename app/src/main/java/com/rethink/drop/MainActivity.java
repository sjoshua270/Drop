package com.rethink.drop;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.firebase.geofire.GeoLocation;
import com.firebase.ui.auth.AuthUI;
import com.github.rahatarmanahmed.cpv.CircularProgressView;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.rethink.drop.exceptions.FragmentArgsMismatch;
import com.rethink.drop.fragments.DropFragment;
import com.rethink.drop.fragments.LocalFragment;
import com.rethink.drop.fragments.ProfileFragment;
import com.rethink.drop.interfaces.ImageRecipient;
import com.rethink.drop.managers.DataManager;
import com.rethink.drop.models.Comment;
import com.rethink.drop.models.Drop;
import com.rethink.drop.models.Profile;
import com.rethink.drop.tools.FabManager;
import com.rethink.drop.tools.FragmentJuggler;

import java.util.Arrays;

import io.fabric.sdk.android.Fabric;

import static com.rethink.drop.fragments.ImageFragment.IMAGE_URL;
import static com.rethink.drop.managers.DataManager.getDrop;
import static com.rethink.drop.models.Comment.COMMENT_KEY;
import static com.rethink.drop.models.Drop.KEY;
import static com.rethink.drop.models.Profile.PROFILE_KEY;
import static com.rethink.drop.tools.FragmentJuggler.CURRENT;
import static com.rethink.drop.tools.FragmentJuggler.DROP;
import static com.rethink.drop.tools.FragmentJuggler.FEED;
import static com.rethink.drop.tools.FragmentJuggler.FRAGMENT_NAMES;
import static com.rethink.drop.tools.FragmentJuggler.FRIENDS;
import static com.rethink.drop.tools.FragmentJuggler.IMAGE;
import static com.rethink.drop.tools.FragmentJuggler.PROFILE;

public class MainActivity extends AppCompatActivity {
    public static final int RC_SIGN_IN = 1;
    public static final String EDITING = "editing";
    public static final int STORAGE_REQUEST = 3;
    private static final int REQUEST_LOCATION = 2;
    private static final int REQUEST_CHECK_SETTINGS = 3830;
    public static Location userLocation;
    private static LocationRequest mLocationRequest;
    private static FragmentJuggler fragmentJuggler;
    private static LocationCallback mLocationCallback;
    private static CircularProgressView spinner;
    private static ActionBar actionBar;
    private static FragmentManager fragmentManager;
    private TextView noDropsFound;
    private FusedLocationProviderClient mFusedLocationClient;
    private FabManager fab;
    private DataManager dataManager;

    public static ImageRecipient getImageRecipient(Class recipient) {
        Fragment imageRecipient = fragmentJuggler.getCurrentFragment();
        if (imageRecipient.getClass()
                          .equals(recipient)) {
            return (ImageRecipient) imageRecipient;
        }
        return null;
    }

    public static void onDropFound() {
        spinner.setVisibility(View.GONE);
    }

    public static void notifyDropInserted(String key) {
        Fragment fragment = fragmentJuggler.getCurrentFragment();
        Class fragmentClass = fragment.getClass();
        if (fragmentClass.equals(LocalFragment.class)) {
            ((LocalFragment) fragment).notifyDropInserted(key);
        }
    }

    public static void notifyDropChanged(String key) {
        Fragment fragment = fragmentJuggler.getCurrentFragment();
        Class fragmentClass = fragment.getClass();
        if (fragmentClass.equals(LocalFragment.class)) {
            ((LocalFragment) fragment).notifyDropChanged(key);
        }
    }

    public static void notifyDropRemoved(String key) {
        Fragment fragment = fragmentJuggler.getCurrentFragment();
        Class fragmentClass = fragment.getClass();
        if (fragmentClass.equals(LocalFragment.class)) {
            ((LocalFragment) fragment).notifyDropRemoved(key);
        }
    }

    private static void syncUpNav() {
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(fragmentManager.getBackStackEntryCount() > 1);
        }
    }

    public static void openListing(View listingView, String key) {
        try {
            fragmentJuggler.viewListing(
                    listingView,
                    key
            );
        } catch (FragmentArgsMismatch fam) {
            Log.e(
                    "openListing",
                    fam.getMessage()
            );
        }
    }

    public static void openProfile(View profile, String userID) {
        try {

            fragmentJuggler.viewProfile(
                    profile,
                    userID
            );
        } catch (FragmentArgsMismatch fam) {
            Log.e(
                    "openProfile",
                    fam.getMessage()
            );
        }
    }

    public static void openProfile(String userID) {
        Bundle args = new Bundle();
        args.putString(PROFILE_KEY,
                       userID);
        try {

            fragmentJuggler.setMainFragment(PROFILE,
                                            args);
        } catch (FragmentArgsMismatch fam) {
            Log.e("openProfile",
                  fam.getMessage());
        }
    }

    public static void editComment(String commentKey, Comment comment) {
        Fragment dropFragment = fragmentJuggler.getCurrentFragment();
        if (dropFragment.getClass()
                        .equals(DropFragment.class)) {
            dropFragment.getArguments()
                        .putString(
                                COMMENT_KEY,
                                commentKey
                        );
            ((DropFragment) dropFragment).editComment(comment.getText());
        }
    }

    public void askForLocationPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_LOCATION
        );
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        spinner = findViewById(R.id.progress_view);
        spinner.setVisibility(View.VISIBLE);
        noDropsFound = findViewById(R.id.no_drops);
        noDropsFound.setVisibility(View.GONE);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        actionBar = getSupportActionBar();
        fragmentManager = getSupportFragmentManager();
        Fabric.with(this,
                    new Crashlytics());

        dataManager = new DataManager(this);
        fragmentJuggler = new FragmentJuggler(getSupportFragmentManager());
        // Let's handle what happens when we do get our location
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                userLocation = locationResult.getLastLocation();
                applyLocation(userLocation);
                super.onLocationResult(locationResult);
            }
        };

        fab = new FabManager(
                this,
                (FloatingActionButton) findViewById(R.id.fab)
        );
        setFabListener((FloatingActionButton) findViewById(R.id.fab));
        setBackStackListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        dataManager.onPause();
        // Let's stop listening for updates
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    private void setFabListener(final FloatingActionButton fab) {
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (CURRENT) {
                    case FEED:
                        final FirebaseUser user = FirebaseAuth.getInstance()
                                                              .getCurrentUser();
                        if (user != null) {
                            Profile.getRef(user.getUid())
                                   .addListenerForSingleValueEvent(new ValueEventListener() {
                                       @Override
                                       public void onDataChange(DataSnapshot dataSnapshot) {
                                           if (dataSnapshot.getValue(Profile.class) != null) {
                                               Bundle args = new Bundle();
                                               openFragment(
                                                       DROP,
                                                       args
                                               );
                                           } else {
                                               Bundle args = new Bundle();
                                               args.putString(
                                                       PROFILE_KEY,
                                                       user.getUid()
                                               );
                                               try {
                                                   fragmentJuggler.setMainFragment(
                                                           PROFILE,
                                                           args
                                                   );
                                                   showMessage("Please set up a profile in order to make a Drop");
                                               } catch (FragmentArgsMismatch e) {
                                                   showMessage(getString(R.string.unexpected_error));
                                               }
                                           }
                                       }

                                       @Override
                                       public void onCancelled(DatabaseError databaseError) {

                                       }
                                   });
                        } else {
                            login();
                        }
                        break;
                    case PROFILE:
                        Fragment profileFragment = fragmentJuggler.getCurrentFragment();
                        if (profileFragment.getClass()
                                           .equals(ProfileFragment.class)) {
                            ((ProfileFragment) profileFragment).addToFriends();
                        }
                        break;
                }
            }
        });
    }

    public void login() {
        startActivityForResult(
                // Get an instance of AuthUI based on the default app
                AuthUI.getInstance()
                      .createSignInIntentBuilder()
                      .setIsSmartLockEnabled(!BuildConfig.DEBUG)
                      .setProviders(Arrays.asList(
                              new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                              new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()
                      ))
                      .setTheme(R.style.AppTheme)
                      .build(),
                RC_SIGN_IN
        );
    }

    private void openFragment(int id, Bundle args) {
        try {
            fragmentJuggler.setMainFragment(
                    id,
                    args
            );
        } catch (FragmentArgsMismatch fam) {
            Log.e(
                    "openFragment",
                    FRAGMENT_NAMES[id] + " Fragment - " + fam.getMessage()
            );
            showMessage(getString(R.string.unexpected_error));
        }
        if (findViewById(R.id.sub_fragment_container).getVisibility() == View.VISIBLE) {
            findViewById(R.id.sub_fragment_container).setVisibility(View.GONE);
        }
    }

    public void showMessage(final String message) {
        Snackbar.make(
                findViewById(R.id.fab),
                message,
                Snackbar.LENGTH_LONG
        )
                .show();
    }

    public void syncUI() {
        syncUpNav();
        Fragment curr = fragmentJuggler.getCurrentFragment();
        Bundle currArgs = curr.getArguments();
        fab.update(currArgs);
        if (!curr.getClass()
                 .equals(ProfileFragment.class)) {
            actionBar.setTitle(FRAGMENT_NAMES[CURRENT]);
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
                            CURRENT = FEED;
                        }
                        if (currClass.equals(DropFragment.class)) {
                            CURRENT = DROP;
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

    public void viewImage(String key) {
        Bundle args = new Bundle();
        Drop drop = getDrop(key);
        if (drop != null) {
            args.putString(
                    IMAGE_URL,
                    drop.getImageURL()
            );
            openFragment(
                    IMAGE,
                    args
            );
        } else {
            Log.e(
                    "viewImage",
                    "Drop is not cached/saved"
            );
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Bundle args = new Bundle();
                openFragment(
                        FEED,
                        args
                );
            }
        }
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                updateLocation();
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

        //        if (optionID == R.id.toggle_map) {
        //            Bundle args = new Bundle();
        //            args.putDouble("LAT",
        //                           userLocation.latitude);
        //            args.putDouble("LNG",
        //                           userLocation.longitude);
        //            openFragment(MAP,
        //                         args);
        //        }

        switch (optionID) {
            case R.id.search:
                onSearchRequested();
                break;
        }
        if (fragmentClass.equals(LocalFragment.class)) {
            switch (optionID) {
                case R.id.open_profile:
                    FirebaseUser user = FirebaseAuth.getInstance()
                                                    .getCurrentUser();
                    if (user != null) {
                        Bundle args = new Bundle();
                        args.putString(
                                PROFILE_KEY,
                                user.getUid()
                        );
                        openFragment(
                                PROFILE,
                                args
                        );
                    } else {
                        login();
                    }
                    break;
            }
        }
        if (fragmentClass.equals(DropFragment.class)) {
            DropFragment dropFragment = (DropFragment) fragment;
            String dropKey = getSupportFragmentManager().findFragmentById(R.id.main_fragment_container)
                                                        .getArguments()
                                                        .getString(KEY);
            switch (optionID) {
                case R.id.delete_drop:
                    Drop drop = getDrop(dropKey);
                    if (drop != null) {
                        drop.delete(dropKey);
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
                    profileFragment.checkUsername();
                    break;
                case R.id.edit_profile:
                    profileFragment.editProfile();
                    break;
                case R.id.friends:
                    openFragment(
                            FRIENDS,
                            profileFragment.getArguments()
                    );
                    break;
            }
        }
        if (optionID == android.R.id.home) {
            getSupportFragmentManager().popBackStackImmediate();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void updateLocation() {
        // Do we have Location permissions?
        if (locationPermissionsGranted()) {
            checkLocationSettings(); // Yes
        } else {
            askForLocationPermission(); // No
        }
    }

    private Boolean locationPermissionsGranted() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (
                ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                        &&
                        ActivityCompat.checkSelfPermission(
                                this,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED);
    }

    private void checkLocationSettings() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        // Do the system settings match what we want?
        task.addOnSuccessListener(
                this,
                new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        // Yes! Let's ask for location
                        checkForLastLocation();
                    }
                }
        );

        task.addOnFailureListener(
                this,
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case CommonStatusCodes.RESOLUTION_REQUIRED:
                                // Location settings are not satisfied, but this can be fixed
                                // by showing the user a dialog.
                                try {
                                    // Show the dialog by calling startResolutionForResult(),
                                    // and check the result in onActivityResult().
                                    ResolvableApiException resolvable = (ResolvableApiException) e;
                                    resolvable.startResolutionForResult(
                                            MainActivity.this,
                                            MainActivity.REQUEST_CHECK_SETTINGS
                                    );
                                } catch (IntentSender.SendIntentException sendEx) {
                                    // Ignore the error.
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                // Location settings are not satisfied. However, we have no way
                                // to fix the settings so we won't show the dialog.
                                break;
                        }
                    }
                }
        );
    }

    @SuppressLint("MissingPermission")
    private void checkForLastLocation() {
        if (locationPermissionsGranted()) {
            Task<Location> lastLocationTask = mFusedLocationClient.getLastLocation();
            lastLocationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        userLocation = location;
                        applyLocation(userLocation);
                    } else {
                        startLocationUpdates();
                    }
                }
            });
            lastLocationTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    startLocationUpdates();
                }
            });
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (!locationPermissionsGranted()) {
            askForLocationPermission();
        } else {
            // Start asking for updates
            mFusedLocationClient.requestLocationUpdates(
                    mLocationRequest,
                    mLocationCallback,
                    null
            );
        }
    }

    private void applyLocation(Location location) {
//        noDropsFound.setVisibility(View.VISIBLE);
        dataManager.updateLocation(new GeoLocation(
                location.getLatitude(),
                location.getLongitude()
        ));
        dataManager.onResume();
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            openFragment(
                    FEED,
                    null
            );
        }
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateLocation();
            }
        }
    }
}
