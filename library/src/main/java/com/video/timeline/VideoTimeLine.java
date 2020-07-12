package com.video.timeline;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

public class VideoTimeLine {

    private SimpleExoPlayer exoPlayer;
    private MediaSource mediaSource;
    private TimelineViewFace timelineView;

    private boolean started;

    public void start() {
        if (started) return;

        if (mediaSource != null) {
            started = true;
            exoPlayer.prepare(mediaSource);

            timelineView.setExoPlayer(exoPlayer);
            timelineView.prepareSurfaceRenderer();
        }
    }

    public void destroy() {
        release();
    }

    private void release() {
        if (exoPlayer != null) {
            exoPlayer.release();
            timelineView.releaseSurface();
        }
    }

    private void prepareMedia(Context context, String mediaURI, SeekParameters seekParams) {
        if (!TextUtils.isEmpty(mediaURI)) {

            mediaSource = new ProgressiveMediaSource.Factory(new DefaultDataSourceFactory(context, "exo"))
                    .createMediaSource(Uri.parse(mediaURI));

            exoPlayer = new SimpleExoPlayer.Builder(context, new VideoRendererOnlyFactory(context))
                    .setLoadControl(
                            new DefaultLoadControl.Builder()
                                    .setBufferDurationsMs(
                                            100, 100, 100, 100)
                                    .createDefaultLoadControl()
                    )
                    .build();
            exoPlayer.setSeekParameters(seekParams);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
//        release();
    }

    public static Builder with(String mediaUri) {
        return new Builder(mediaUri);
    }

    public static class Builder {
        private static final int DEFAULT_SIZE = 80;

        private final String mediaURI;
        private boolean scrollable;
        private int size;
        private int frameDuration;
        private ImageLoader imageLoader;
        private SeekParameters seekParams;

        private int dpToPx(float dpValue, Context context) {
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            return (int) TypedValue.applyDimension(COMPLEX_UNIT_DIP, dpValue, dm);
        }

        Builder(String mediaURI) {
            this.mediaURI = mediaURI;
            this.size = DEFAULT_SIZE;
            this.frameDuration = 1;
            seekParams = SeekParameters.NEXT_SYNC;
        }

        public Builder setSeekParams(SeekParameters seekParams) {
            this.seekParams = seekParams;
            return this;
        }

        public Builder setFrameSizeDp(int size) {
            this.size = size;
            return this;
        }

        public Builder setFrameDuration(int seconds) {
            this.frameDuration = seconds;
            return this;
        }

        public Builder setImageLoader(ImageLoader loader) {
            this.imageLoader = loader;
            return this;
        }

        public VideoTimeLine into(TimelineViewFace fixedView) {
            Context context = fixedView.context();

            VideoTimeLine timeline = new VideoTimeLine();
            timeline.timelineView = fixedView;
            timeline.prepareMedia(context, mediaURI, seekParams);

            if (fixedView instanceof ScrollableTimelineGlView) {
                ((ScrollableTimelineGlView)fixedView).
                        setup(createRecyclerView(context), imageLoader, dpToPx(size, context), frameDuration);
            }

            return timeline;
        }

        public VideoTimeLine show(TimelineViewFace view) {
            VideoTimeLine timeLine = into(view);
            timeLine.start();
            return timeLine;
        }

        private RecyclerView createRecyclerView(Context context) {
            RecyclerView recyclerView = new RecyclerView(context);
            recyclerView.setLayoutParams(new ViewGroup.LayoutParams
                    (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            recyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false));
            return recyclerView;
        }
    }
}
