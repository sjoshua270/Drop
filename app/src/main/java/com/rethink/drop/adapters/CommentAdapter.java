package com.rethink.drop.adapters;


import android.view.View;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.Query;
import com.rethink.drop.MainActivity;
import com.rethink.drop.models.Comment;
import com.rethink.drop.tools.Utilities;
import com.rethink.drop.viewholders.CommentHolder;

public class CommentAdapter extends FirebaseRecyclerAdapter<Comment, CommentHolder> {

    /**
     * @param modelClass      Firebase will marshall the data at a location into an instance of a class that you provide
     * @param modelLayout     This is the layout used to represent a single item in the list. You will be responsible for populating an
     *                        instance of the corresponding view with the data from an instance of modelClass.
     * @param viewHolderClass The class that hold references to all sub-views in an instance modelLayout.
     * @param ref             The Firebase location to watch for data changes. Can also be a slice of a location, using some
     *                        combination of {@code limit()}, {@code startAt()}, and {@code endAt()}.
     */
    public CommentAdapter(Class<Comment> modelClass, int modelLayout, Class<CommentHolder> viewHolderClass, Query ref) {
        super(modelClass,
              modelLayout,
              viewHolderClass,
              ref);
    }

    @Override
    protected void populateViewHolder(CommentHolder viewHolder, final Comment comment, final int position) {
        viewHolder.text.setText(comment.getText());
        String edited = comment.isEdited() ? "edited" : "";
        viewHolder.timeStamp.setText(Utilities.getTimeStampString(comment.getTimeStamp()));
        viewHolder.edited.setText(edited);
        Utilities.setProfileData(MainActivity.getInstance(),
                                 comment.getCommenterID(),
                                 viewHolder.profile,
                                 viewHolder.username);
        viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                MainActivity.getInstance()
                            .editComment(getKey(position),
                                         comment);
                return true;
            }
        });
    }

    private String getKey(int position) {
        return getRef(position).getKey();
    }
}

