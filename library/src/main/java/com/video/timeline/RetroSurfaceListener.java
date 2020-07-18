package com.video.timeline;

import android.view.Surface;

import java.nio.ByteBuffer;

public interface RetroSurfaceListener {
    void onSurfaceAvailable(Surface surface);
    void onTextureRetrieved(ByteBuffer pixelBuffer);
}
