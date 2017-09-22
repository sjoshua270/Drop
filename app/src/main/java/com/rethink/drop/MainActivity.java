package com.rethink.drop;

import android.app.Dialog;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.geofire.GeoLocation;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.rethink.drop.exceptions.FragmentArgsMismatch;
import com.rethink.drop.fragments.DropFragment;
import com.rethink.drop.fragments.LocalFragment;
import com.rethink.drop.fragments.ProfileFragment;
import com.rethink.drop.managers.DataManager;
import com.rethink.drop.models.Comment;
import com.rethink.drop.models.Drop;
import com.rethink.drop.models.Profile;
import com.rethink.drop.tools.FabManager;
import com.rethink.drop.tools.FragmentJuggler;

import java.util.Arrays;

import static com.rethink.drop.fragments.ImageFragment.IMAGE_URL;
import static com.rethink.drop.managers.DataManager.getDrop;
import static com.rethink.drop.models.Comment.COMMENT_KEY;
import static com.rethink.drop.models.Drop.KEY;
import static com.rethink.drop.models.Profile.PROFILE_KEY;
import static com.rethink.drop.tools.FragmentJuggler.CURRENT;
import static com.rethink.drop.tools.FragmentJuggler.FRAGMENT_NAMES;
import static com.rethink.drop.tools.FragmentJuggler.FRIENDS;
import static com.rethink.drop.tools.FragmentJuggler.IMAGE;
import static com.rethink.drop.tools.FragmentJuggler.LISTING;
import static com.rethink.drop.tools.FragmentJuggler.LOCAL;
import static com.rethink.drop.tools.FragmentJuggler.PROFILE;

public class MainActivity extends AppCompatActivity implements OnConnectionFailedListener,
                                                               ConnectionCallbacks,
                                                               LocationListener {
    public static final int RC_SIGN_IN = 1;
    public static final String EDITING = "editing";
    public static final int STORAGE_REQUEST = 3;
    public static MainActivity instance;
    public static LatLng userLocation;
    private static GoogleApiClient googleApiClient;
    private static FragmentJuggler fragmentJuggler;
    private final int LOCATION_REQUEST = 2;
    private FabManager fab;
    private DataManager dataManager;

    public static MainActivity getInstance() {
        if (instance != null) {
            return instance;
        } else {
            return new MainActivity();
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;

        int status = GoogleApiAvailability.getInstance()
                                          .isGooglePlayServicesAvailable(this);
        if (status == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED) {
            Dialog dialog = GoogleApiAvailability.getInstance()
                                                 .getErrorDialog(MainActivity.getInstance(),
                                                                 status,
                                                                 1);
            dialog.show();
        } else {
            googleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                                                               .addOnConnectionFailedListener(this)
                                                               .addApi(LocationServices.API)
                                                               .build();
        }
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

    private void setFabListener(final FloatingActionButton fab) {
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (CURRENT) {
                    case LOCAL:
                        final FirebaseUser user = FirebaseAuth.getInstance()
                                                              .getCurrentUser();
                        if (user != null) {
                            Profile.getRef(user.getUid())
                                   .addListenerForSingleValueEvent(new ValueEventListener() {
                                       @Override
                                       public void onDataChange(DataSnapshot dataSnapshot) {
                                           if (dataSnapshot.getValue(Profile.class) != null) {
                                               Bundle args = new Bundle();
                                               openFragment(LISTING,
                                                            args);
                                           } else {
                                               Bundle args = new Bundle();
                                               args.putString(PROFILE_KEY,
                                                              user.getUid());
                                               try {
                                                   fragmentJuggler.setMainFragment(PROFILE,
                                                                                   args);
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
                      .setProviders(Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                                                  new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                      .setTheme(R.style.AppTheme)
                      .build(),
                RC_SIGN_IN);
    }

    private void openFragment(int id, Bundle args) {
        try {
            fragmentJuggler.setMainFragment(id,
                                            args);
        } catch (FragmentArgsMismatch fam) {
            Log.e("openFragment",
                  FRAGMENT_NAMES[id] + " Fragment - " +
                  fam.getMessage());
            showMessage(getString(R.string.unexpected_error));
        }
        if (findViewById(R.id.sub_fragment_container).getVisibility() == View.VISIBLE) {
            findViewById(R.id.sub_fragment_container).setVisibility(View.GONE);
        }
    }

    public void showMessage(final String message) {
        Snackbar.make(findViewById(R.id.fab),
                      message,
                      Snackbar.LENGTH_LONG)
                .show();
    }

    public void syncUI() {
        syncUpNav();
        fab.update(fragmentJuggler.getCurrentFragment()
                                  .getArguments());
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

    public void notifyDropInserted(String key) {
        Fragment fragment = fragmentJuggler.getCurrentFragment();
        Class fragmentClass = fragment.getClass();
        if (fragmentClass.equals(LocalFragment.class)) {
            ((LocalFragment) fragment).notifyDropInserted(key);
        }
    }

    public void notifyDropChanged(String key) {
        Fragment fragment = fragmentJuggler.getCurrentFragment();
        Class fragmentClass = fragment.getClass();
        if (fragmentClass.equals(LocalFragment.class)) {
            ((LocalFragment) fragment).notifyDropChanged(key);
        }
    }

    public void notifyDropRemoved(String key) {
        Fragment fragment = fragmentJuggler.getCurrentFragment();
        Class fragmentClass = fragment.getClass();
        if (fragmentClass.equals(LocalFragment.class)) {
            ((LocalFragment) fragment).notifyDropRemoved(key);
        }
    }

    private void syncUpNav() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount() > 1);
        }
    }

    public void openListing(View listingView, String key) {
        try {
            fragmentJuggler.viewListing(listingView,
                                        key);
        } catch (FragmentArgsMismatch fam) {
            Log.e("openListing",
                  fam.getMessage());
        }
    }

    public void openProfile(View profile, String userID) {
        try {

            fragmentJuggler.viewProfile(profile,
                                        userID);
        } catch (FragmentArgsMismatch fam) {
            Log.e("openProfile",
                  fam.getMessage());
        }
    }

    public void viewImage(String key) {
        Bundle args = new Bundle();
        Drop drop = getDrop(key);
        if (drop != null) {
            args.putString(IMAGE_URL,
                           drop.getImageURL());
            openFragment(IMAGE,
                         args);
        } else {
            Log.e("viewImage",
                  "Drop is not cached/saved");
        }
    }

    public void editComment(String commentKey, Comment comment) {
        Fragment dropFragment = fragmentJuggler.getCurrentFragment();
        if (dropFragment.getClass()
                        .equals(DropFragment.class)) {
            dropFragment.getArguments()
                        .putString(COMMENT_KEY,
                                   commentKey);
            ((DropFragment) dropFragment).editComment(comment.getText());
        }
    }

    public void showKeyboard(EditText editText) {
        editText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editText,
                          InputMethodManager.SHOW_IMPLICIT);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,
                               resultCode,
                               data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Bundle args = new Bundle();
                openFragment(LOCAL,
                             args);
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

        if (fragmentClass.equals(LocalFragment.class)) {
            switch (optionID) {
                case R.id.open_profile:
                    FirebaseUser user = FirebaseAuth.getInstance()
                                                    .getCurrentUser();
                    if (user != null) {
                        Bundle args = new Bundle();
                        args.putString(PROFILE_KEY,
                                       user.getUid());
                        openFragment(PROFILE,
                                     args);
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
                    profileFragment.saveProfile();
                    break;
                case R.id.edit_profile:
                    profileFragment.editProfile();
                    break;
                case R.id.friends:
                    openFragment(FRIENDS,
                                 profileFragment.getArguments());
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
        Toast.makeText(this,
                       "Failed to connect to location service: " + connectionResult.getErrorCode(),
                       Toast.LENGTH_LONG)
             .show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this,
                       "Connection suspended",
                       Toast.LENGTH_LONG)
             .show();
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
            dataManager.onResume();
        }
    }

    private void stopLocationUpdates() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
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
        if (googleApiClient != null && googleApiClient.isConnected()) {
            startLocationUpdates();
        }
        updateLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        dataManager.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (googleApiClient != null) {
            googleApiClient.disconnect();
        }
    }
}
