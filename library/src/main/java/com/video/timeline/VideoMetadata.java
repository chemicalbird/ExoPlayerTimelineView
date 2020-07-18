package com.video.timeline;

public class VideoMetadata {
    private long durationMs;
    private int width;
    private int height;

    public float aspectRatio() {
        return height == 0 ? 0 : width * 1F / height;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public boolean isCorrupt() {
        return durationMs <= 0 || width <=0 || height <= 0;
    }

    public boolean isCorruptDimensions() {
        return width <=0 || height <= 0;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
}
