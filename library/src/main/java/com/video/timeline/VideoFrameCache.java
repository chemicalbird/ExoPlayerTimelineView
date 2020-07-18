package com.video.timeline;

import androidx.annotation.Nullable;

import com.video.timeline.tools.FileHelper;

import java.io.File;

public class VideoFrameCache {

    private final String mediaId;
    private File cacheDir;

    VideoFrameCache(File cacheDir, String videoUri) {
        this.mediaId = videoUri.substring(videoUri.lastIndexOf('/') + 1);
        this.cacheDir = cacheDir;
        if (cacheDir != null) {
            this.cacheDir.mkdir();
        }
    }

    @Nullable
    File fileAt(long timeMs) {
        return cacheDir != null ? FileHelper.getCachedFile(cacheDir, mediaId, timeMs) : null;
    }
}
