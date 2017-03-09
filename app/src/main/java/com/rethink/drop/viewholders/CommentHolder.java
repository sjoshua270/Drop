package com.rethink.drop.viewholders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.rethink.drop.R;

public class CommentHolder extends RecyclerView.ViewHolder {
    public final ImageView profile;
    public final TextView timeStamp;
    public final TextView username;
    public final TextView text;
    public final TextView edited;

    public CommentHolder(View itemView) {
        super(itemView);
        profile = (ImageView) itemView.findViewById(R.id.comment_prof_image);
        timeStamp = (TextView) itemView.findViewById(R.id.comment_timestamp);
        username = (TextView) itemView.findViewById(R.id.comment_user_name);
        text = (TextView) itemView.findViewById(R.id.comment_text);
        edited = (TextView) itemView.findViewById(R.id.comment_edited);
    }
}
