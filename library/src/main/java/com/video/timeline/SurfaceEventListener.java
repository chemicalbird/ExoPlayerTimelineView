package com.video.timeline;

import android.view.Surface;

public interface SurfaceEventListener{
    void onSurfaceAvailable(Surface surface);
    void drawAndMoveToNext(int offset, int limit);
    void onFrameAvailable(String filePath, int frameIndex);
}
