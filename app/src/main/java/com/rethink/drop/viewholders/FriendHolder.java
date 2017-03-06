package com.rethink.drop.viewholders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.rethink.drop.R;

public class FriendHolder extends RecyclerView.ViewHolder {
    public ImageView profileImage;
    public TextView profileName;

    public FriendHolder(View itemView) {
        super(itemView);
        profileImage = (ImageView) itemView.findViewById(R.id.friend_prof_image);
        profileName = (TextView) itemView.findViewById(R.id.friend_text);
    }
}
