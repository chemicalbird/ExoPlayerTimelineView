package com.video.timeline;

import android.content.Context;

public interface TimelineViewFace {
    void startSurfaceRenderer();
    void releaseSurface();
    Context context();
    void attachVideoFactory(ExoPlayerFactory playerFactory);
}
