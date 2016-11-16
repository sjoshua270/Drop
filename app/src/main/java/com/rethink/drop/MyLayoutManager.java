package com.rethink.drop;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;

public class MyLayoutManager
        extends LinearLayoutManager {


    public MyLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    @Override
    public boolean supportsPredictiveItemAnimations() {
        return false;
    }
}
