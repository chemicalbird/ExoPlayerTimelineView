package com.video.timeline;

import android.graphics.SurfaceTexture;
import android.opengl.Matrix;
import android.text.TextUtils;
import android.view.Surface;

import com.otaliastudios.opengl.core.EglCore;
import com.otaliastudios.opengl.draw.GlRect;
import com.otaliastudios.opengl.surface.EglOffscreenSurface;
import com.otaliastudios.opengl.surface.EglSurface;
import com.otaliastudios.opengl.texture.GlTexture;

public class OSRenderer extends BaseGLRenderer implements SurfaceTexture.OnFrameAvailableListener {

    private static final long TIMEOUT = 3000;

    private float[] mvp = new float[16];
    private float[] projection = new float[16];
    private float[] view = new float[16];

    private SurfaceTexture surfaceTexture;
    private final GlRect drawable = new GlRect();

    private final SurfaceEventListener eventListener;
    private int mWidth;
    private int mHeight;

    private final Object countAvailableLock = new Object();
    private int itemCount;
    private int pendingIndex;

    private FBOHandler fboHandler;

    OSRenderer(int width, int height, SurfaceEventListener eventListener) {
        super();
        this.mWidth = width;
        this.mHeight = height;
        this.eventListener = eventListener;

        Matrix.setLookAtM(view, 0,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f
        );
        Matrix.setIdentityM(projection, 0);
        Matrix.multiplyMM(mvp, 0, projection, 0, view, 0);
    }

    @Override
    EglSurface createEglSurface(EglCore egl) {
        return new EglOffscreenSurface(egl, mWidth, mHeight);
    }

    @Override
    void setupDrawingResources(EglCore egl) {
        GlTexture glTexture = new GlTexture();
        fboHandler = new FBOHandler(TextureProgram.createOESProgram(glTexture));
        fboHandler.configure(mWidth, mHeight);

        surfaceTexture = new SurfaceTexture(glTexture.getId());
        surfaceTexture.setOnFrameAvailableListener(this);
        if (eventListener != null) {
            eventListener.onSurfaceAvailable(new Surface(surfaceTexture));
        }
    }

    @Override
    void drawFrame(int itemID) {
        if (itemID < 0) return;
        surfaceTexture.updateTexImage();
        surfaceTexture.getTransformMatrix(fboHandler.getTextureTransform());

        makeCurrent();
        fboHandler.bind();
        fboHandler.draw(drawable, mvp);

        String file = saveFrame(mWidth, mHeight, "");
        if (!TextUtils.isEmpty(file)) {
            eventListener.onFrameAvailable(file, itemID);
        }
        fboHandler.unbind();
    }

    void onVideoAspectChanged(float videoAspect) {
        synchronized (countAvailableLock) {

            if (videoAspect > 1) {
                Matrix.orthoM(projection, 0, -1 / videoAspect, 1 / videoAspect, -1, 1, -1, 1);
            } else {
                Matrix.orthoM(projection, 0, -1, 1, -videoAspect, videoAspect, -1, 1);
            }
            Matrix.multiplyMM(mvp, 0, projection, 0, view, 0);

            countAvailableLock.notifyAll();
        }
    }

    void setItemCount(int itemCount) {
        synchronized (countAvailableLock) {
            this.itemCount = itemCount;
            countAvailableLock.notifyAll();
        }
    }

    private void waitForCount() {
        synchronized (countAvailableLock) {
            if (itemCount == 0 || mvp == null) {
                try {
                    Loggy.d("Waiting for clue");
                    countAvailableLock.wait(TIMEOUT);
                    if (itemCount == 0 || mvp == null) {
                        Loggy.d("Took too long");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // called on drawing thread
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        Loggy.d("Frame available");
        waitForCount();
        if (pendingIndex < itemCount) {
            requestRender(pendingIndex);
            eventListener.drawAndMoveToNext(pendingIndex++, itemCount);
        } else {
            surfaceTexture.setOnFrameAvailableListener(null);
        }
    }

    public void drawSameFrame() {
        requestRender(pendingIndex++);
    }

    @Override
    public void release() {
        super.release();
        fboHandler.release();
    }
}
