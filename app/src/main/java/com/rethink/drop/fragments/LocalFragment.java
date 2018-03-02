package com.rethink.drop.fragments;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;

import com.google.firebase.auth.FirebaseAuth;
import com.rethink.drop.MyLayoutManager;
import com.rethink.drop.R;
import com.rethink.drop.adapters.DropAdapter;

import static com.rethink.drop.managers.DataManager.feedKeys;
import static com.rethink.drop.managers.DataManager.getDropIndex;


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
        dropAdapter.notifyItemInserted(getDropIndex(key));
    }

    public void notifyDropChanged(String key) {
        dropAdapter.notifyItemChanged(getDropIndex(key));
    }

    public void notifyDropRemoved(String key) {
        dropAdapter.notifyItemRemoved(getDropIndex(key));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup
            container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.recycler_view, container, false);
        dropsRecycler = rootView.findViewById(R.id.recycler_view);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        dropsRecycler.setLayoutManager(new MyLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        int resId = R.anim.layout_anim_fade_in;
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(
                getContext(),
                resId
        );
        dropsRecycler.setLayoutAnimation(animation);
        dropsRecycler.setAdapter(dropAdapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            inflater.inflate(R.menu.menu_local, menu);

            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity != null) {
                SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
                SearchView searchView = (SearchView) menu.findItem(R.id.search)
                                                         .getActionView();
                if (searchManager != null) {
                    searchView.setSearchableInfo(searchManager.getSearchableInfo(activity.getComponentName()));
                    searchView.setIconifiedByDefault(true);
                }
            }
            super.onCreateOptionsMenu(menu,
                                      inflater);
        }
    }

    public void scrollToDrop(String key) {
        dropsRecycler.smoothScrollToPosition(feedKeys.indexOf(key));
    }
}
