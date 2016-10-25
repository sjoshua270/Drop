package com.rethink.drop;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.rethink.drop.fragments.ProfileFragment;
import com.rethink.drop.models.Listing;

import static com.rethink.drop.DataManager.listings;
import static com.rethink.drop.FragmentJuggler.CURRENT;
import static com.rethink.drop.FragmentJuggler.EDIT;
import static com.rethink.drop.FragmentJuggler.LOCAL;
import static com.rethink.drop.FragmentJuggler.PROF;
import static com.rethink.drop.FragmentJuggler.VIEW;
import static com.rethink.drop.models.Listing.KEY;

final class FabManager {

    private Context context;
    private FirebaseAuth firebaseAuth;
    private FloatingActionButton fab;
    private FragmentJuggler fragmentJuggler;

    FabManager(Context context, FirebaseAuth firebaseAuth, FloatingActionButton fab, FragmentJuggler fragmentJuggler) {
        this.context = context;
        this.fab = fab;
        this.firebaseAuth = firebaseAuth;
        this.fragmentJuggler = fragmentJuggler;
    }

    void update() {
        hide();
        if (CURRENT == LOCAL) {
            setDrawable(R.drawable.ic_add_white_24px);
            show();
        } else if (CURRENT == EDIT) {
            setDrawable(R.drawable.ic_send_white_24px);
            show();
        } else if (CURRENT == VIEW) {
            String key = fragmentJuggler.getCurrentFragment().getArguments().getString(KEY);
            Listing listing = listings.get(key);
            FirebaseUser user = firebaseAuth.getCurrentUser();

            if (user != null && user.getUid().equals(listing.getUserID())) {
                setDrawable(R.drawable.ic_mode_edit_white_24px);
                show();
            }
        } else if (CURRENT == PROF) {
            if (((ProfileFragment) fragmentJuggler.getCurrentFragment()).isEditing()) {
                setDrawable(R.drawable.ic_save_white_24dp);
            } else {
                setDrawable(R.drawable.ic_mode_edit_white_24px);
            }
            show();
        }
    }

    private void setDrawable(int drawableID) {
        fab.setImageDrawable(ContextCompat.getDrawable(
                context,
                drawableID));
    }

    private void hide() {
        Animation anim = AnimationUtils
                .loadAnimation(context, R.anim.shrink_fade_out);
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

    private void show() {
        Animation anim = AnimationUtils
                .loadAnimation(context, R.anim.grow_fade_in);
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

}
