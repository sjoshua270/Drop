package com.rethink.drop.tools;

import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.transition.AutoTransition;
import android.transition.ChangeBounds;
import android.transition.ChangeTransform;
import android.transition.Fade;
import android.transition.TransitionSet;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.rethink.drop.R;
import com.rethink.drop.fragments.DropFragment;
import com.rethink.drop.fragments.ImageFragment;
import com.rethink.drop.fragments.LocalFragment;
import com.rethink.drop.fragments.ProfileFragment;

import java.io.IOException;

public class FragmentJuggler {

    public static final int LOCAL = 0;
    public static final int LISTING = 1;
    public static final int PROFILE = 2;
    public static final int IMAGE = 3;
    public static int CURRENT;
    private final FragmentManager fragmentManager;

    public FragmentJuggler(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    public void openFragment(int fragmentID, @Nullable String key) {
        switch (fragmentID) {
            case LOCAL:
                switchFragments(LocalFragment.newInstance());
                break;
            case LISTING:
                switchFragments(DropFragment.newInstance(key));
                break;
            case PROFILE:
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    switchFragments(ProfileFragment.newInstance(user.getUid()));
                } else {
                    Toast.makeText(getCurrentFragment().getContext(), "No userID", Toast.LENGTH_SHORT).show();
                }
                break;
            case IMAGE:
                switchFragments(ImageFragment.newInstance(key));
                break;
        }
        CURRENT = fragmentID;
    }

    public Fragment getCurrentFragment() {
        return fragmentManager.findFragmentById(R.id.main_fragment_container);
    }

    private void switchFragments(Fragment newFragment) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            newFragment.setEnterTransition(new AutoTransition());
            newFragment.setExitTransition(new AutoTransition());
        }
        fragmentManager.beginTransaction()
                       .replace(R.id.main_fragment_container, newFragment)
                       .addToBackStack(null)
                       .commit();
    }

    public void viewListing(View listingView, String key) throws IOException {
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
                       .replace(R.id.main_fragment_container,
                                listingFragment)
                       .addToBackStack(null)
                       .commit();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static class ViewTransition
            extends TransitionSet {
        ViewTransition() {
            setOrdering(ORDERING_TOGETHER);
            addTransition(new ChangeBounds());
            addTransition(new ChangeTransform());
            addTransition(new Fade());
        }
    }
}
