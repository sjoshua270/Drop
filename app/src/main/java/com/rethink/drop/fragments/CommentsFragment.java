package com.rethink.drop.fragments;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rethink.drop.R;
import com.rethink.drop.adapters.CommentAdapter;
import com.rethink.drop.models.Comment;
import com.rethink.drop.viewholders.CommentHolder;

import java.util.Calendar;

import static com.rethink.drop.models.Drop.KEY;

public class CommentsFragment extends Fragment {
    private CommentAdapter commentAdapter;
    private RecyclerView commentRecycler;
    private String dropKey;
    private TextInputEditText textField;

    public static CommentsFragment newInstance(Bundle args) {
        CommentsFragment fragment = new CommentsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        dropKey = args.getString(KEY);
        DatabaseReference ref = FirebaseDatabase.getInstance()
                                                .getReference()
                                                .child("comments")
                                                .child(dropKey);
        commentAdapter = new CommentAdapter(Comment.class,
                                            R.layout.item_comment,
                                            CommentHolder.class,
                                            ref);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater,
                           container,
                           savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_comments,
                                         container,
                                         false);
        commentRecycler = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        textField = (TextInputEditText) rootView.findViewById(R.id.comment_edit_text);
        final String commenterID;
        FirebaseUser user = FirebaseAuth.getInstance()
                                        .getCurrentUser();
        if (user != null) {
            commenterID = user.getUid();
        } else {
            //TODO: Have the user log in
            commenterID = "";
        }
        rootView.findViewById(R.id.comment_submit)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        FirebaseDatabase.getInstance()
                                        .getReference()
                                        .child("comments")
                                        .child(dropKey)
                                        .push()
                                        .setValue(new Comment(commenterID,
                                                              textField.getText()
                                                                       .toString(),
                                                              Calendar.getInstance()
                                                                      .getTimeInMillis()));
                    }
                });
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        commentRecycler.setLayoutManager(new LinearLayoutManager(getContext(),
                                                                 LinearLayoutManager.VERTICAL,
                                                                 false));
        commentRecycler.setAdapter(commentAdapter);
    }
}
