package com.video.timeline;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Surface;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.video.VideoListener;

public class ScrollableTimelineGlView extends FrameLayout implements TimelineViewFace,
        VideoListener, SurfaceEventListener, Player.EventListener {
    private RecyclerView recyclerView;
    private VideoAdapter imagesAdapter;

    private SimpleExoPlayer player;
    private long lastSeekPos;
    private Handler mainHandler;

    private long videoDuration;
    private long frameDuration;
    private int frameSize;
    private int frameCount;

    OSRenderer offscreenSurface;

    public ScrollableTimelineGlView(@NonNull Context context) {
        this(context, null);
    }

    public ScrollableTimelineGlView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public ScrollableTimelineGlView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mainHandler = new Handler();
    }

    public void setup(RecyclerView recyclerView, ImageLoader imageLoader, int frameSize, int frameDurationSc) {
        this.frameDuration = frameDurationSc * C.MILLIS_PER_SECOND;
        this.frameSize = frameSize;
        this.recyclerView = recyclerView;

        imagesAdapter = new VideoAdapter(frameSize, imageLoader);
        recyclerView.setAdapter(imagesAdapter);
        addView(recyclerView);
    }

    private void prepareOffscreenRenderer() {
        offscreenSurface = new OSRenderer(frameSize, frameSize, this);
//        offscreenSurface.configure();
    }

    public void releaseDrawingResources() {
        if (offscreenSurface != null) {
            offscreenSurface.release();
        }
    }

    @Override
    public Context context() {
        return getContext();
    }

    @Override
    public void setExoPlayer(SimpleExoPlayer videoComponent) {
        this.player = videoComponent;
        videoComponent.addVideoListener(this);
        videoComponent.addListener(this);

        videoDuration = videoComponent.getDuration();
        if (videoDuration != C.TIME_UNSET) {
            calculateFrameCount();
        }
    }

    @Override
    public void prepareSurfaceRenderer() {
        prepareOffscreenRenderer();
    }

    @Override
    public void releaseSurface() {
        releaseDrawingResources();
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        float videoAspect = ((float) width / height) * pixelWidthHeightRatio;
        offscreenSurface.onVideoAspectChanged(videoAspect);
    }

    @Override
    public void onSurfaceAvailable(Surface surface) {
        Loggy.d("Set Player surface");
        mainHandler.post(() -> this.player.setVideoSurface(surface));
    }

    @Override
    public void drawAndMoveToNext(int offset, int limit) {
        mainHandler.post(() -> {
            if (offset < limit) {
                lastSeekPos = Math.max(frameDuration * (1+offset), player.getContentPosition() + frameDuration / 3);
                Loggy.d("Player seek: " + lastSeekPos);
                player.seekTo(lastSeekPos);
            }
        });
    }

    @Override
    public void onSeekProcessed() {
//        if (lastSeekPos != 0 && player.getContentPosition() == lastSeekPos) {
//            player.seekTo(lastSeekPos + 1);
//        }
//        lastSeekPos = 0;
    }

    @Override
    public void onFrameAvailable(String filePath) {
        mainHandler.post(() -> imagesAdapter.addPath(filePath));
    }

    public void calculateFrameCount() {
        frameCount = (int) (videoDuration / frameDuration);
        Loggy.d("FrameC: " + frameCount);
        if (offscreenSurface != null) {
            offscreenSurface.setItemCount(frameCount);
        }
    }

    @Override
    public void onTimelineChanged(Timeline timeline, int reason) {
        long duration = player.getDuration();
        if (duration != C.TIME_UNSET && duration != videoDuration) {
            videoDuration = duration;
            calculateFrameCount();
        }
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }
}
