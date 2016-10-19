package com.rethink.drop;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.AutoTransition;
import android.transition.ChangeBounds;
import android.transition.ChangeTransform;
import android.transition.Fade;
import android.transition.TransitionSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rethink.drop.fragments.EditFragment;
import com.rethink.drop.fragments.ListingFragment;
import com.rethink.drop.fragments.LocalFragment;
import com.rethink.drop.fragments.ViewFragment;
import com.rethink.drop.models.Listing;

import java.util.ArrayList;
import java.util.List;

import static com.rethink.drop.DataManager.listings;
import static com.rethink.drop.models.Listing.KEY;

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
    private FloatingActionButton fab;
    private Fragment currFragment;
    private ListingFragment viewFragment;
    private ListingFragment editFragment;
    private LocalFragment localFragment;

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
        databaseReferences = new ArrayList<>();
        dataManager = new DataManager();
        updateDBRef();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        firebaseAuth = FirebaseAuth.getInstance();

        localFragment = LocalFragment.newInstance();
        viewFragment = ViewFragment.newInstance(new ViewFragment());
        editFragment = EditFragment.newInstance(new EditFragment());

        setSupportActionBar(toolbar);
        switchFragments(localFragment);
        setFabListener(fab);
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
                if (currFragment.getClass().equals(LocalFragment.class)) {
                    if (firebaseAuth.getCurrentUser() != null) {
                        editFragment = EditFragment.newInstance(new EditFragment());
                        switchFragments(editFragment);
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
                } else if (currFragment.getClass().equals(EditFragment.class)) {
                    ((EditFragment) currFragment).publishListing();
                } else if (currFragment.getClass().equals(ViewFragment.class)) {
                    viewToEditListing();
                }
            }
        });
    }

    private void setBackStackListener() {
        getSupportFragmentManager().addOnBackStackChangedListener(new OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                currFragment = getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);
                if (currFragment != null) {
                    updateFab();
                } else {
                    finish();
                }
            }
        });
    }

    private void updateFab() {
        hideFab();
        Class currClass = currFragment.getClass();
        if (currClass.equals(LocalFragment.class)) {
            setFabDrawable(R.drawable.ic_add_white_24px);
            showFab();
        } else if (currClass.equals(EditFragment.class)) {
            setFabDrawable(R.drawable.ic_send_white_24px);
            showFab();
        } else if (currClass.equals(ViewFragment.class)) {
            String key = currFragment.getArguments().getString(KEY);
            Listing listing = listings.get(key);
            FirebaseUser user = firebaseAuth.getCurrentUser();

            if (user != null && user.getUid().equals(listing.getUserID())) {
                setFabDrawable(R.drawable.ic_mode_edit_white_24px);
                showFab();
            }
        }
    }

    private void setFabDrawable(int drawableID) {
        fab.setImageDrawable(ContextCompat.getDrawable(
                MainActivity.this,
                drawableID));
    }

    private void hideFab() {
        Animation anim = AnimationUtils
                .loadAnimation(MainActivity.this, R.anim.shrink_fade_out);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                fab.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        fab.startAnimation(anim);
    }

    private void showFab() {
        Animation anim = AnimationUtils
                .loadAnimation(MainActivity.this, R.anim.grow_fade_in);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                fab.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        fab.startAnimation(anim);

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                switchFragments(editFragment);
            }
        }
    }

    private void switchFragments(Fragment newFragment) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            newFragment.setEnterTransition(new AutoTransition());
            newFragment.setExitTransition(new AutoTransition());
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_fragment_container,
                        newFragment)
                .addToBackStack(null)
                .commit();
        currFragment = newFragment;

    }

    private void viewToEditListing() {
        String key = viewFragment.getArguments()
                                 .getString(KEY);
        editFragment = EditFragment.newInstance(
                new EditFragment(),
                key
        );
        transitionFragments(viewFragment, editFragment,
                (ImageView) findViewById(R.id.listing_image),
                (TextView) findViewById(R.id.listing_title),
                (TextView) findViewById(R.id.listing_desc));
        currFragment = editFragment;
    }

    public void viewListing(View listingView, String key) {
        viewFragment = ViewFragment.newInstance(
                new ViewFragment(),
                key
        );
        transitionFragments(localFragment, viewFragment,
                (ImageView) listingView.findViewById(R.id.item_image),
                (TextView) listingView.findViewById(R.id.item_title),
                (TextView) listingView.findViewById(R.id.item_desc));
        currFragment = viewFragment;
    }

    private void transitionFragments(Fragment frag1, Fragment frag2, ImageView image, TextView title, TextView desc) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            frag2.setSharedElementEnterTransition(new ViewTransition());
            frag2.setEnterTransition(new Fade());
            frag1.setReturnTransition(new Fade());
            frag1.setSharedElementReturnTransition(new ViewTransition());
        }
        getSupportFragmentManager()
                .beginTransaction()
                .addSharedElement(image, "image")
                .addSharedElement(title, "title")
                .addSharedElement(desc, "desc")
                .replace(R.id.main_fragment_container,
                        frag2)
                .addToBackStack(null)
                .commit();
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public class ViewTransition
            extends TransitionSet {
        ViewTransition() {
            setOrdering(ORDERING_TOGETHER);
            addTransition(new ChangeBounds());
            addTransition(new ChangeTransform());
            addTransition(new Fade());
        }
    }
}
