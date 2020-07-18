package com.video.timeline;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.google.android.exoplayer2.SeekParameters;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

public class VideoTimeLine {

    private TimelineViewFace timelineView;
    private boolean started;

    public void start() {
        if (started) return;

        started = true;
        timelineView.startSurfaceRenderer();
    }

    public void destroy() {
        release();
    }

    private void release() {
        timelineView.releaseSurface();
    }

    public static Builder with(String mediaUri) {
        return new Builder(mediaUri);
    }

    public static class Builder {
        private static final int DEFAULT_SIZE = 80;

        private final String mediaURI;
        private int size;

        private int dpToPx(float dpValue, Context context) {
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            return (int) TypedValue.applyDimension(COMPLEX_UNIT_DIP, dpValue, dm);
        }

        Builder(String mediaURI) {
            this.mediaURI = mediaURI;
            this.size = DEFAULT_SIZE;
        }

        public Builder setFrameSizeDp(int size) {
            this.size = size;
            return this;
        }

        public VideoTimeLine into(TimelineViewFace fixedView) {
            VideoTimeLine timeline = new VideoTimeLine();
            timeline.timelineView = fixedView;
            timeline.timelineView.attachVideoFactory(new ExoPlayerFactory(mediaURI, SeekParameters.NEXT_SYNC));
            return timeline;
        }

        public VideoTimeLine show(TimelineViewFace view) {
            VideoTimeLine timeLine = into(view);
            timeLine.start();
            return timeLine;
        }
    }
}
