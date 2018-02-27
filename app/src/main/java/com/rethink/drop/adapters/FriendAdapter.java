package com.rethink.drop.adapters;


import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.Query;
import com.rethink.drop.models.Profile;
import com.rethink.drop.viewholders.FriendHolder;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

public class FriendAdapter extends FirebaseRecyclerAdapter<Profile, FriendHolder> {
    private Context context;

    /**
     * @param modelClass      Firebase will marshall the data at a location into an instance of a class that you provide
     * @param modelLayout     This is the layout used to represent a single item in the list. You will be responsible for populating an
     *                        instance of the corresponding view with the data from an instance of modelClass.
     * @param viewHolderClass The class that hold references to all sub-views in an instance modelLayout.
     * @param ref             The Firebase location to watch for data changes. Can also be a slice of a location, using some
     *                        combination of {@code limit()}, {@code startAt()}, and {@code endAt()}.
     */
    public FriendAdapter(Context context, Class<Profile> modelClass, int modelLayout, Class<FriendHolder> viewHolderClass, Query ref) {
        super(modelClass,
              modelLayout,
              viewHolderClass,
              ref);
        this.context = context;
    }

    @Override
    protected void populateViewHolder(FriendHolder friendHolder, Profile profile, int position) {
        RequestOptions glideOptions = new RequestOptions()
                .centerCrop();
        Glide.with(context)
             .load(profile.getImageURL())
             .apply(glideOptions)
             .transition(withCrossFade())
             .into(friendHolder.profileImage);
        friendHolder.profileName.setText(profile.getName());
    }
}
