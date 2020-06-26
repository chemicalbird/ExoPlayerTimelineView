package com.video.timeline;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Surface;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.video.VideoListener;
import com.otaliastudios.opengl.core.EglConfigChooser;
import com.otaliastudios.opengl.core.EglContextFactory;

public class TimelineGlSurfaceView extends GLSurfaceView implements TimelineViewFace, VideoListener, SurfaceEventListener {

    private final Handler mainHandler;
    private GlRenderer renderer;
    private SimpleExoPlayer player;

    public TimelineGlSurfaceView(Context context) {
        this(context, null);
    }

    public TimelineGlSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setVisibility(GONE);
        mainHandler = new Handler(Looper.getMainLooper());

        setEGLContextFactory(EglContextFactory.GLES2);
        setEGLConfigChooser(EglConfigChooser.GLES2);

        setRenderer();
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    public void setRenderer() {
        renderer = new GlRenderer(this);
        setRenderer(renderer);
    }

    @Override
    public void onPause() {
        super.onPause();
        renderer.release();
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        float videoAspect = ((float) width / height) * pixelWidthHeightRatio;
        renderer.onAspectPrepared(videoAspect);
        requestLayout();
    }

    @Override
    public Context context() {
        return getContext();
    }

    @Override
    public void setExoPlayer(SimpleExoPlayer videoComponent) {
        this.player = videoComponent;
        videoComponent.addVideoListener(this);
    }

    @Override
    public void prepareSurfaceRenderer() {
        setVisibility(VISIBLE);
        renderer.setStartRendering();
    }

    @Override
    public void releaseSurface() {
        renderer.release();
    }

    @Override
    public void onSurfaceAvailable(Surface surface) {
        mainHandler.post(() -> {
            this.player.setVideoSurface(surface);
        });
    }

    @Override
    public void drawAndMoveToNext(int offset, int limit) {
        requestRender();
        if (offset < limit) {
            long videoDuration = player.getDuration();
            long seekPos =
                    Math.max(videoDuration / limit + offset * (videoDuration / limit), player.getContentPosition() + 1);

            player.seekTo(seekPos);
        }
    }

    @Override
    public void onFrameAvailable(String filePath) {

    }
}