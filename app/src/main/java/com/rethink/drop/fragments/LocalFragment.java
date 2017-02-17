package com.rethink.drop.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.rethink.drop.BuildConfig;
import com.rethink.drop.MyLayoutManager;
import com.rethink.drop.R;
import com.rethink.drop.adapters.DropAdapter;

import java.util.Arrays;

import static com.rethink.drop.DataManager.keys;
import static com.rethink.drop.MainActivity.RC_SIGN_IN;


public class LocalFragment
        extends Fragment {
    private DropAdapter dropAdapter;
    private RecyclerView dropsRecycler;

    public static LocalFragment newInstance() {

        Bundle args = new Bundle();

        LocalFragment fragment = new LocalFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dropAdapter = new DropAdapter();
        setHasOptionsMenu(true);
    }

    public void notifyDropInserted(String key) {
        int position = keys.indexOf(key);
        dropAdapter.notifyItemInserted(position);
    }

    public void notifyDropRemoved(String key) {
        int position = keys.indexOf(key);
        dropAdapter.notifyItemRemoved(position);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup
            container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.recycler_view, container, false);
        dropsRecycler = (RecyclerView) rootView.findViewById(R.id.recycler_local_listings);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        dropsRecycler.setLayoutManager(new MyLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        dropsRecycler.setAdapter(dropAdapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            inflater.inflate(R.menu.menu_local, menu);
            super.onCreateOptionsMenu(menu, inflater);
        }
    }

    public void scrollToDrop(String key) {
        dropsRecycler.smoothScrollToPosition(keys.indexOf(key));
    }

    public void handleFabPress() {
        startActivityForResult(
                // Get an instance of AuthUI based on the default app
                AuthUI.getInstance()
                      .createSignInIntentBuilder()
                      .setIsSmartLockEnabled(!BuildConfig.DEBUG)
                      .setProviders(Arrays.asList(
                              new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                              new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                      .setTheme(R.style.AppTheme)
                      .build(),
                RC_SIGN_IN);
    }
}
