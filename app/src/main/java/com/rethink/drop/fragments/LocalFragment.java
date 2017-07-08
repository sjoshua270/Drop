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

import com.google.firebase.auth.FirebaseAuth;
import com.rethink.drop.MyLayoutManager;
import com.rethink.drop.R;
import com.rethink.drop.adapters.DropAdapter;

import static com.rethink.drop.managers.DataManager.getDropIndex;
import static com.rethink.drop.managers.DataManager.keys;


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
        dropsRecycler = (RecyclerView) rootView.findViewById(R.id.recycler_view);
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
}
