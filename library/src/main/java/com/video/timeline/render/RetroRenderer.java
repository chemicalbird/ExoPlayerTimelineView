package com.video.timeline.render;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.view.Surface;

import com.otaliastudios.opengl.core.EglCore;
import com.otaliastudios.opengl.core.Egloo;
import com.otaliastudios.opengl.draw.GlRect;
import com.otaliastudios.opengl.surface.EglOffscreenSurface;
import com.otaliastudios.opengl.surface.EglSurface;
import com.otaliastudios.opengl.texture.GlTexture;
import com.video.timeline.tools.GlUtils;
import com.video.timeline.tools.Loggy;
import com.video.timeline.RetroSurfaceListener;
import com.video.timeline.VideoFrameCache;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class RetroRenderer extends BaseGLRenderer implements SurfaceTexture.OnFrameAvailableListener {

    private static final long TIMEOUT = 5000;

    private float[] mvp;
    private float[] projection = new float[16];
    private float[] view = new float[16];

    private SurfaceTexture surfaceTexture;
    private final GlRect drawable = new GlRect();

    private final RetroSurfaceListener eventListener;
    private VideoFrameCache videoCache;
    private int mWidth;
    private int mHeight;

    private final Object sizeAvailableLock = new Object();

    private FBOHandler fboHandler;

    public RetroRenderer(int width, int height, RetroSurfaceListener eventListener) {
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

        surfaceTexture.updateTexImage();
        surfaceTexture.getTransformMatrix(fboHandler.getTextureTransform());

        makeCurrent();
        fboHandler.bind();
        fboHandler.draw(drawable, mvp);

        GLES20.glFinish();

        ByteBuffer pixelBuf = ByteBuffer.allocateDirect(mWidth * mHeight * 4);
        pixelBuf.order(ByteOrder.LITTLE_ENDIAN);
        GLES20.glReadPixels(0, 0, mWidth, mHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuf);
        Egloo.checkGlError("glReadPixels");
        pixelBuf.rewind();

        GlUtils.reverseBuf(pixelBuf, mWidth, mHeight);

        eventListener.onTextureRetrieved(pixelBuf);

        fboHandler.unbind();
    }

    public void onVideoAspectChanged(float videoAspect) {
        synchronized (sizeAvailableLock) {
            mvp = new float[16];
            if (videoAspect > 1) {
                Matrix.orthoM(projection, 0, -1 / videoAspect, 1 / videoAspect, -1, 1, -1, 1);
            } else {
                Matrix.orthoM(projection, 0, -1, 1, -videoAspect, videoAspect, -1, 1);
            }
            Matrix.multiplyMM(mvp, 0, projection, 0, view, 0);

            sizeAvailableLock.notifyAll();
        }
    }

    public void clearTxtMtx() {
        mvp = null;
    }

    private void waitForDimensions() {
        synchronized (sizeAvailableLock) {
            if (mvp == null) {
                try {
                    Loggy.d("Waiting for clue");
                    sizeAvailableLock.wait(TIMEOUT);
                    if (mvp == null) {
                        Loggy.d("Took too long");
                        assureMvpMatrix();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void assureMvpMatrix() {
        if (mvp == null) {
            mvp = new float[16];
            Matrix.multiplyMM(mvp, 0, projection, 0, view, 0);
        }
    }

    // called on drawing thread
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        Loggy.d("Frame available");
        waitForDimensions();
        requestRender(0);
    }

    public void drawSameFrame() {
        waitForDimensions();
        requestRender(55);
    }

    @Override
    public void release() {
        super.release();
        fboHandler.release();
    }
}