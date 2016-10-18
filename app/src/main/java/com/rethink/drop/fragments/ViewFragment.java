package com.rethink.drop.fragments;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.transition.ChangeBounds;
import android.transition.ChangeTransform;
import android.transition.Fade;
import android.transition.TransitionSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.GoogleMap;
import com.rethink.drop.R;

import static com.rethink.drop.models.Listing.KEY;

public class ViewFragment
        extends ListingFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        inputTitle.setVisibility(View.GONE);
        inputDesc.setVisibility(View.GONE);
        if (imageIcon == null) {
            imageView.setVisibility(View.GONE);
        }
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageFragment imageFragment = ImageFragment.newInstance(getArguments().getString(KEY));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    imageFragment.setSharedElementEnterTransition(new ViewFragment.ViewTransition());
                    imageFragment.setEnterTransition(new Fade());
                    ViewFragment.this.setReturnTransition(new Fade());
                    ViewFragment.this.setSharedElementReturnTransition(new ViewFragment.ViewTransition());
                }
                getActivity().getSupportFragmentManager()
                             .beginTransaction()
                             .addSharedElement(imageView, "image")
                             .replace(R.id.main_fragment_container,
                                     imageFragment)
                             .addToBackStack(null)
                             .commit();
            }
        });

        return fragmentView;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        super.onMapReady(googleMap);
        googleMap.getUiSettings()
                 .setAllGesturesEnabled(false);
        googleMap.getUiSettings()
                 .setMyLocationButtonEnabled(false);
    }

    // Life cycle events

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
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
