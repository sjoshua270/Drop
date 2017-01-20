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
import com.rethink.drop.models.Drop;

import static com.rethink.drop.tools.FragmentJuggler.CURRENT;
import static com.rethink.drop.tools.FragmentJuggler.LISTING;
import static com.rethink.drop.tools.FragmentJuggler.LOCAL;
import static com.rethink.drop.tools.FragmentJuggler.PROFILE;

final class FabManager {

    private final Context context;
    private final FloatingActionButton fab;

    FabManager(Context context, FloatingActionButton fab) {
        this.context = context;
        this.fab = fab;
    }

    void update(String key, boolean isEditing) {
        switch (CURRENT) {
            case LOCAL:
                setDrawable(R.drawable.ic_add_white_24px);
                break;
            case LISTING:
                if (key != null) {
                    if (isEditing) {
                        setDrawable(R.drawable.ic_save_white_24dp);
                    } else {
                        FirebaseDatabase.getInstance()
                                        .getReference()
                                        .child("posts")
                                        .child(key)
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                Drop drop = dataSnapshot.getValue(Drop.class);
                                                if (drop != null) {
                                                    FirebaseUser user = FirebaseAuth.getInstance()
                                                                                    .getCurrentUser();
                                                    if (user != null && user.getUid()
                                                                            .equals(drop.getUserID())) {
                                                        setDrawable(R.drawable.ic_mode_edit_white_24px);
                                                    }
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
                break;
            case PROFILE:
                if (isEditing) {
                    setDrawable(R.drawable.ic_save_white_24dp);
                } else {
                    setDrawable(R.drawable.ic_mode_edit_white_24px);
                }
                break;
        }
    }

    private void setDrawable(int drawableID) {
        fab.setImageDrawable(ContextCompat.getDrawable(context,
                                                       drawableID));
        show();
    }

    void hide() {
        fab.setClickable(false);
        Animation anim = AnimationUtils.loadAnimation(context,
                                                      R.anim.shrink_fade_out);
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

    void show() {
        fab.setClickable(true);
        Animation anim = AnimationUtils.loadAnimation(context,
                                                      R.anim.grow_fade_in);
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
