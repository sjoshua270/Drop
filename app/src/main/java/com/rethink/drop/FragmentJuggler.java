package com.rethink.drop;

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
import com.rethink.drop.fragments.ListingFragment;
import com.rethink.drop.fragments.LocalFragment;
import com.rethink.drop.fragments.ProfileFragment;

class FragmentJuggler {

    static final int LOCAL = 0;
    static final int LISTING = 1;
    static final int PROFILE = 2;
    static int CURRENT;
    private FragmentManager fragmentManager;

    FragmentJuggler(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    void openFragment(int fragmentID, @Nullable String key) {
        switch (fragmentID) {
            case LOCAL:
                switchFragments(LocalFragment.newInstance());
                break;
            case LISTING:
                switchFragments(ListingFragment.newInstance(key));
                break;
            case PROFILE:
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    switchFragments(ProfileFragment.newInstance(user.getUid()));
                } else {
                    Toast.makeText(getCurrentFragment().getContext(), "No userID", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        CURRENT = fragmentID;
    }

    Fragment getCurrentFragment() {
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

    void viewListing(View listingView, String key) {
        Fragment listingFragment = ListingFragment.newInstance(key);
        transitionFragments(getCurrentFragment(), listingFragment,
                new View[]{
                        listingView.findViewById(R.id.item_image),
                        listingView.findViewById(R.id.item_title),
                        listingView.findViewById(R.id.item_desc)});
        CURRENT = LISTING;
    }

    private void transitionFragments(Fragment frag1, Fragment frag2, View[] views) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            frag2.setSharedElementEnterTransition(new FragmentJuggler.ViewTransition());
            frag2.setEnterTransition(new Fade());
            frag1.setReturnTransition(new Fade());
            frag1.setSharedElementReturnTransition(new FragmentJuggler.ViewTransition());
        }
        fragmentManager.beginTransaction()
                       .addSharedElement(views[0], "image")
                       .addSharedElement(views[1], "title")
                       .addSharedElement(views[2], "desc")
                       .replace(R.id.main_fragment_container,
                               frag2)
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
