package com.rethink.drop.viewholders;

import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.rethink.drop.R;

public class DropHolder extends RecyclerView.ViewHolder {
    public final ImageView profile;
    public final ImageView imageView;
    public final TextView desc;
    public final RelativeLayout details;
    public final TextView dist;
    public final TextView timeStampTime;
    public final TextView timeStampDay;

    public DropHolder(View itemView) {
        super(itemView);
        profile = itemView.findViewById(R.id.item_prof_img);
        imageView = itemView.findViewById(R.id.item_image);
        desc = itemView.findViewById(R.id.item_desc);
        details = itemView.findViewById(R.id.item_details);
        dist = itemView.findViewById(R.id.item_distance);
        timeStampTime = itemView.findViewById(R.id.item_timestamp_time);
        timeStampDay = itemView.findViewById(R.id.item_timestamp_day);
    }

    public void setKey(String dropKey) {
        ViewCompat.setTransitionName(
                imageView,
                "image_" + dropKey
        );
        ViewCompat.setTransitionName(
                details,
                "detail_" + dropKey
        );
    }
}
