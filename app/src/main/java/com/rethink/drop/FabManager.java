package com.rethink.drop;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import static com.rethink.drop.tools.FragmentJuggler.CURRENT;
import static com.rethink.drop.tools.FragmentJuggler.LOCAL;

final class FabManager {

    private final Context context;
    private final FloatingActionButton fab;

    FabManager(Context context, FloatingActionButton fab) {
        this.context = context;
        this.fab = fab;
    }

    void update() {
        if (fab.getVisibility() != View.GONE) {
            hide();
        }
        switch (CURRENT) {
            case LOCAL:
                setDrawable(R.drawable.ic_add_white_24px);
                break;
        }
    }

    private void setDrawable(int drawableID) {
        fab.setImageDrawable(ContextCompat.getDrawable(context,
                                                       drawableID));
        show();
    }

    private void hide() {
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

    private void show() {
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
