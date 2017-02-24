package com.rethink.drop.viewholders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.rethink.drop.R;

public class ListingHolder extends RecyclerView.ViewHolder {
    public final ImageView profile;
    public final ImageView imageView;
    public final TextView desc;
    public final TextView dist;
    public final TextView timeStampTime;
    public final TextView timeStampDay;

    public ListingHolder(View itemView) {
        super(itemView);
        profile = (ImageView) itemView.findViewById(R.id.item_prof_img);
        imageView = (ImageView) itemView.findViewById(R.id.item_image);
        desc = (TextView) itemView.findViewById(R.id.item_desc);
        dist = (TextView) itemView.findViewById(R.id.item_distance);
        timeStampTime = (TextView) itemView.findViewById(R.id.item_timestamp_time);
        timeStampDay = (TextView) itemView.findViewById(R.id.item_timestamp_day);
    }
}
