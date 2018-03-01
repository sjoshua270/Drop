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

import com.rethink.drop.R;
import com.rethink.drop.exceptions.FragmentArgsMismatch;
import com.rethink.drop.fragments.DropFragment;
import com.rethink.drop.fragments.FriendsFragment;
import com.rethink.drop.fragments.ImageFragment;
import com.rethink.drop.fragments.LocalFragment;
import com.rethink.drop.fragments.ProfileFragment;

import static com.rethink.drop.models.Drop.KEY;
import static com.rethink.drop.models.Profile.PROFILE_KEY;

public class FragmentJuggler {

    public static final int FEED = 0;
    public static final int DROP = 1;
    public static final int PROFILE = 2;
    public static final int IMAGE = 3;
    public static final int FRIENDS = 4;
    public static final String[] FRAGMENT_NAMES = {
            "Feed",
            "Drop",
            "Profile",
            "Image",
            "Friends"
    };
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
            case FEED:
                switchFragments(LocalFragment.newInstance(),
                                R.id.main_fragment_container,
                                true);
                break;
            case DROP:
                switchFragments(DropFragment.newInstance(args),
                                R.id.main_fragment_container,
                                true);
                break;
            case PROFILE:
                switchFragments(ProfileFragment.newInstance(args),
                                R.id.main_fragment_container,
                                true);
                break;
            case IMAGE:
                switchFragments(ImageFragment.newInstance(args),
                                R.id.main_fragment_container,
                                true);
                break;
            case FRIENDS:
                switchFragments(FriendsFragment.newInstance(args),
                                R.id.main_fragment_container,
                                true);

        }
        CURRENT = fragmentID;
    }

    public Fragment getCurrentFragment() {
        return fragmentManager.findFragmentById(R.id.main_fragment_container);
    }

    public void viewListing(View listingView, String key) throws FragmentArgsMismatch {
        CURRENT = DROP;
        Bundle args = new Bundle();
        args.putString(KEY,
                       key);
        Fragment listingFragment = DropFragment.newInstance(args);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            listingFragment.setSharedElementEnterTransition(new FragmentJuggler.ViewTransition());
            listingFragment.setSharedElementReturnTransition(new FragmentJuggler.ViewTransition());
            listingFragment.setEnterTransition(new Fade());
            listingFragment.setReturnTransition(new Fade());
        }
        fragmentManager.beginTransaction()
                       .addSharedElement(
                               listingView.findViewById(R.id.item_image),
                               "image_" + key
                       )
                       .addSharedElement(
                               listingView.findViewById(R.id.item_details),
                               "detail_" + key
                       )
                       .replace(R.id.main_fragment_container,
                                listingFragment)
                       .addToBackStack(null)
                       .commit();
    }

    public void viewProfile(View profileView, String userID) throws FragmentArgsMismatch {
        CURRENT = PROFILE;
        Bundle args = new Bundle();
        args.putString(PROFILE_KEY,
                       userID);
        Fragment profileFragment = ProfileFragment.newInstance(args);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            profileFragment.setSharedElementEnterTransition(new FragmentJuggler.ViewTransition());
            profileFragment.setSharedElementReturnTransition(new FragmentJuggler.ViewTransition());
            profileFragment.setEnterTransition(new Fade());
            profileFragment.setReturnTransition(new Fade());
        }
        fragmentManager.beginTransaction()
                       .addSharedElement(profileView.findViewById(R.id.drop_profile_image),
                                         "prof_" + userID
                       )
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
