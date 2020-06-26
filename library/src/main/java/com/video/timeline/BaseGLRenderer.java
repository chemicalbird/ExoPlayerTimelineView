package com.video.timeline;

import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.otaliastudios.opengl.core.EglCore;
import com.otaliastudios.opengl.core.Egloo;
import com.otaliastudios.opengl.surface.EglSurface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class BaseGLRenderer implements Handler.Callback {
    private final Handler drawHandler;

    private EglCore eglCore;
    private EglSurface eglSurface;

    BaseGLRenderer() {
        HandlerThread handlerThread = new HandlerThread("offscreen_surface_drawer");
        handlerThread.start();
        drawHandler = new Handler(handlerThread.getLooper(), this);
        drawHandler.obtainMessage(0).sendToTarget();
    }

    void requestRender() {
        drawHandler.obtainMessage(2).sendToTarget();
    }

    void makeCurrent() {
        if (!eglSurface.isCurrent()) {
            eglSurface.makeCurrent();
        }
    }

    public void release() {
        drawHandler.obtainMessage(3).sendToTarget();
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case 0: // initialize
                this.eglCore = new EglCore();
                this.eglSurface = createEglSurface(eglCore);

                makeCurrent();
                setupDrawingResources(eglCore);
                break;

            case 2: // draw
                drawFrame();
                break;

            case 3: // destroy
//                eglSurface.makeNothingCurrent();
                eglSurface.release();
                eglCore.release();
                break;
        }
        return false;
    }

    String saveFrame(int width, int height, String identifier) {
        GLES20.glFinish();

        ByteBuffer pixelBuf = ByteBuffer.allocateDirect(width * height * 4);
        pixelBuf.order(ByteOrder.LITTLE_ENDIAN);

        long startWhen = System.currentTimeMillis();

        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuf);
        Egloo.checkGlError("glReadPixels");
        pixelBuf.rewind();

        try {
            return GlUtils.savePixelBuffer(pixelBuf, width, height);
        } catch (IOException e) {
            Log.d("pbo_test", "Save Ex: " + e.getMessage());
        }
        Log.d("pbo_test", "Dur: " + (System.currentTimeMillis() - startWhen));

        return null;
    }

    abstract EglSurface createEglSurface(EglCore egl);
    abstract void setupDrawingResources(EglCore egl);
    abstract void drawFrame();
}
