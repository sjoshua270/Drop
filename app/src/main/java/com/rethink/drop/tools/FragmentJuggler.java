package com.rethink.drop.tools;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.transition.ChangeBounds;
import android.transition.ChangeTransform;
import android.transition.Fade;
import android.transition.TransitionSet;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.rethink.drop.R;
import com.rethink.drop.exceptions.FragmentArgsMismatch;
import com.rethink.drop.fragments.DropFragment;
import com.rethink.drop.fragments.FriendsFragment;
import com.rethink.drop.fragments.ImageFragment;
import com.rethink.drop.fragments.LocalFragment;
import com.rethink.drop.fragments.ProfileFragment;

import static com.rethink.drop.fragments.ImageFragment.IMAGE_URL;
import static com.rethink.drop.models.Drop.KEY;
import static com.rethink.drop.models.Profile.PROFILE_KEY;

public class FragmentJuggler {

    public static final int LOCAL = 0;
    public static final int LISTING = 1;
    public static final int PROFILE = 2;
    public static final int IMAGE = 3;
    public static final int FRIENDS = 6;
    public static int CURRENT;
    private static FragmentManager fragmentManager;

    public FragmentJuggler(FragmentManager fManager) {
        fragmentManager = fManager;
    }

    private static void switchFragments(Fragment newFragment, int container, Boolean addToBackstack) {
        FragmentTransaction ft = fragmentManager.beginTransaction()
                                                .replace(container,
                                                         newFragment);
        if (addToBackstack) {
            ft.addToBackStack(null);
        }
        ft.commit();
    }

    public void setMainFragment(int fragmentID, Bundle args) throws FragmentArgsMismatch {
        switch (fragmentID) {
            case LOCAL:
                switchFragments(LocalFragment.newInstance(),
                                R.id.main_fragment_container,
                                true);
                break;
            case LISTING:
                String dropKey = args.getString(KEY);
                switchFragments(DropFragment.newInstance(dropKey),
                                R.id.main_fragment_container,
                                true);
                break;
            case PROFILE:
                FirebaseUser user = FirebaseAuth.getInstance()
                                                .getCurrentUser();
                if (user != null) {
                    switchFragments(ProfileFragment.newInstance(user.getUid()),
                                    R.id.main_fragment_container,
                                    true);
                } else {
                    Toast.makeText(getCurrentFragment().getContext(),
                                   "No userID",
                                   Toast.LENGTH_SHORT)
                         .show();
                }
                break;
            case IMAGE:
                String imageUrl = args.getString(IMAGE_URL);
                if (imageUrl != null && !imageUrl.equals("")) {
                    switchFragments(ImageFragment.newInstance(imageUrl),
                                    R.id.main_fragment_container,
                                    true);
                } else {
                    throw new FragmentArgsMismatch("imageUrl was null or empty");
                }
                break;
            case FRIENDS:
                String profileKey = args.getString(PROFILE_KEY);
                if (profileKey != null) {
                    switchFragments(FriendsFragment.newInstance(profileKey),
                                    R.id.main_fragment_container,
                                    true);
                } else {
                    throw new FragmentArgsMismatch("profileKey not provided for FriendsFragment");
                }
        }
        CURRENT = fragmentID;
    }

    public Fragment getCurrentFragment() {
        return fragmentManager.findFragmentById(R.id.main_fragment_container);
    }

    public void viewListing(View listingView, String key) {
        CURRENT = LISTING;
        Fragment listingFragment = DropFragment.newInstance(key);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            listingFragment.setSharedElementEnterTransition(new FragmentJuggler.ViewTransition());
            listingFragment.setSharedElementReturnTransition(new FragmentJuggler.ViewTransition());
            listingFragment.setEnterTransition(new Fade());
            listingFragment.setReturnTransition(new Fade());
        }
        fragmentManager.beginTransaction()
                       .addSharedElement(listingView.findViewById(R.id.item_image),
                                         "image_" + key)
                       .addSharedElement(listingView.findViewById(R.id.item_desc),
                                         "desc_" + key)
                       .addSharedElement(listingView.findViewById(R.id.item_prof_img),
                                         "prof_" + key)
                       .replace(R.id.main_fragment_container,
                                listingFragment)
                       .addToBackStack(null)
                       .commit();
    }

    public void viewProfile(View profileView, String userID) {
        CURRENT = PROFILE;
        Fragment profileFragment = ProfileFragment.newInstance(userID);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            profileFragment.setSharedElementEnterTransition(new FragmentJuggler.ViewTransition());
            profileFragment.setSharedElementReturnTransition(new FragmentJuggler.ViewTransition());
            profileFragment.setEnterTransition(new Fade());
            profileFragment.setReturnTransition(new Fade());
        }
        fragmentManager.beginTransaction()
                       .addSharedElement(profileView.findViewById(R.id.drop_profile_image),
                                         "image_" + userID)
                       .replace(R.id.main_fragment_container,
                                profileFragment)
                       .addToBackStack(null)
                       .commit();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static class ViewTransition extends TransitionSet {
        ViewTransition() {
            setOrdering(ORDERING_TOGETHER);
            addTransition(new ChangeBounds());
            addTransition(new ChangeTransform());
            addTransition(new Fade());
        }
    }
}
