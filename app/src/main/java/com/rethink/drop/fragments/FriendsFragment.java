package com.rethink.drop.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DatabaseReference;
import com.rethink.drop.MainActivity;
import com.rethink.drop.R;
import com.rethink.drop.adapters.FriendAdapter;
import com.rethink.drop.exceptions.FragmentArgsMismatch;
import com.rethink.drop.models.Profile;
import com.rethink.drop.viewholders.FriendHolder;

import static com.rethink.drop.models.Profile.PROFILE_KEY;

public class FriendsFragment extends Fragment {
    private FriendAdapter friendAdapter;

    public static FriendsFragment newInstance(Bundle args) throws FragmentArgsMismatch {
        String profileKey = args.getString(PROFILE_KEY);
        if (profileKey == null) {
            throw new FragmentArgsMismatch("profileKey not provided for FriendsFragment");
        }
        FriendsFragment fragment = new FriendsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        String profileKey = args.getString(PROFILE_KEY);
        if (profileKey != null) {
            DatabaseReference friendsRef = Profile.getFriendsRef(profileKey);
            friendAdapter = new FriendAdapter(Profile.class,
                                              R.layout.item_friend,
                                              FriendHolder.class,
                                              friendsRef);
        } else {
            MainActivity.getInstance()
                        .showMessage(getString(R.string.unexpected_error));
            Log.e("FriendsFragment",
                  "Missing profileKey in onCreate");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_friends,
                                  container,
                                  false);
        RecyclerView friendRecycler = (RecyclerView) v.findViewById(R.id.recycler_view);
        friendRecycler.setLayoutManager(new LinearLayoutManager(MainActivity.getInstance()));
        friendRecycler.setAdapter(friendAdapter);
        return v;
    }

    public void addFriend() {

    }
}
