package com.rethink.drop;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rethink.drop.fragments.ListingFragment;
import com.rethink.drop.fragments.ProfileFragment;
import com.rethink.drop.models.Post;

import static com.rethink.drop.FragmentJuggler.CURRENT;
import static com.rethink.drop.FragmentJuggler.LISTING;
import static com.rethink.drop.FragmentJuggler.LOCAL;
import static com.rethink.drop.FragmentJuggler.PROFILE;
import static com.rethink.drop.models.Post.KEY;

final class FabManager {

    private final Context context;
    private final FloatingActionButton fab;
    private final FragmentJuggler fragmentJuggler;

    FabManager(Context context, FloatingActionButton fab, FragmentJuggler fragmentJuggler) {
        this.context = context;
        this.fab = fab;
        this.fragmentJuggler = fragmentJuggler;
    }

    void update() {
        hide();
        if (CURRENT == LOCAL) {
            setDrawable(R.drawable.ic_add_white_24px);
        } else if (CURRENT == LISTING) {
            String key = fragmentJuggler.getCurrentFragment().getArguments().getString(KEY);
            if (key != null) {
                if (((ListingFragment) fragmentJuggler.getCurrentFragment()).isEditing()) {
                    setDrawable(R.drawable.ic_save_white_24dp);
                } else {
                    FirebaseDatabase.getInstance()
                                    .getReference()
                                    .child("posts")
                                    .child(key)
                                    .addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            Post post = dataSnapshot.getValue(Post.class);
                                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                            if (user != null && user.getUid().equals(post.getUserID())) {
                                                setDrawable(R.drawable.ic_mode_edit_white_24px);
                                            }

                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });

                }
            } else {
                setDrawable(R.drawable.ic_send_white_24px);
            }
            show();
        } else if (CURRENT == PROFILE) {
            if (((ProfileFragment) fragmentJuggler.getCurrentFragment()).isEditing()) {
                setDrawable(R.drawable.ic_save_white_24dp);
            } else {
                setDrawable(R.drawable.ic_mode_edit_white_24px);
            }
        }
    }

    private void setDrawable(int drawableID) {
        fab.setImageDrawable(ContextCompat.getDrawable(
                context,
                drawableID));
        show();
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
