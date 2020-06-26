package com.video.timeline;

import android.content.Context;

import com.google.android.exoplayer2.SimpleExoPlayer;

public interface TimelineViewFace {
    void setExoPlayer(SimpleExoPlayer exoPlayer);
    void prepareSurfaceRenderer();
    void releaseSurface();
    Context context();
}
