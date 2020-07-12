package com.video.timeline;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import com.otaliastudios.opengl.draw.GlDrawable;
import com.otaliastudios.opengl.draw.GlRect;
import com.otaliastudios.opengl.texture.GlTexture;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;

public class GlRenderer implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private static final long TIMEOUT = 1000;

    private TextureProgram sceneProgram;
    private GlDrawable drawable;
    private SurfaceTexture surfaceTexture;

    private final SurfaceEventListener surfaceEventListener;
    private int width;
    private int height;
    private float[] MVPMatrix = new float[16];
    private float[] PMatrix = new float[16];
    private float[] VMatrix = new float[16];

    private int pendingIndex;
    private int frameCount;

    private boolean cleanScene = true;
    private boolean startRendering;
    private FBOHandler fboHandler;

    private final Object countAvailableLock = new Object();

    GlRenderer(SurfaceEventListener surfaceEventListener) {
        this.surfaceEventListener = surfaceEventListener;
        Matrix.setLookAtM(VMatrix, 0,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f
        );
        Matrix.setIdentityM(PMatrix, 0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        createDrawingTools();
    }

    private void createDrawingTools() {
        if (!startRendering || fboHandler != null) return;
        drawable = new GlRect();
        sceneProgram = new TextureProgram(TextureProgram.SIMPLE_VERTEX_SHADER, TextureProgram.SIMPLE_FRAGMENT_SHADER);

        GlTexture glTexture = new GlTexture();
        fboHandler = new FBOHandler(TextureProgram.createOESProgram(glTexture));

        surfaceTexture = new SurfaceTexture(glTexture.getId());
        surfaceTexture.setOnFrameAvailableListener(this);
        if (surfaceEventListener != null) {
            surfaceEventListener.onSurfaceAvailable(new Surface(surfaceTexture));
        } else {
            Log.e("error", "Surface is not attached to the player");
        }

        Matrix.multiplyMM(MVPMatrix, 0, PMatrix, 0, VMatrix, 0);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (this.width != width && this.height != height) {
            this.width = width;
            this.height = height;

            configureFBO();
        }
    }

    void setStartRendering() {
        startRendering = true;
    }

    private void configureFBO() {
        if (!startRendering) return;
        GlTexture fbTexture = fboHandler.configure(width, height);
        sceneProgram.setTexture(fbTexture);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (cleanScene) {
            GLES20.glClear(GL_COLOR_BUFFER_BIT);
            cleanScene = false;
        } else if (pendingIndex == frameCount) {
            GLES20.glBindFramebuffer(GL_FRAMEBUFFER, 0);
            GLES20.glViewport(0, 0, width, height);
            GLES20.glClear(GL_COLOR_BUFFER_BIT);
            sceneProgram.draw(drawable);
        } else {
            surfaceTexture.updateTexImage();
            surfaceTexture.getTransformMatrix(fboHandler.getTextureTransform());

            fboHandler.bind();
            int size = width / frameCount;
            GLES20.glViewport(pendingIndex++ * size, 0, size, height);
            fboHandler.draw(drawable, MVPMatrix);
            fboHandler.unbind();

            GLES20.glBindFramebuffer(GL_FRAMEBUFFER, 0);
            GLES20.glViewport(0, 0, width, height);
            GLES20.glClear(GL_COLOR_BUFFER_BIT);

            sceneProgram.draw(drawable);
        }
    }

    public void release() {
        if (fboHandler != null) {
            fboHandler.release();
        }
        if (sceneProgram != null) {
            sceneProgram.release();
        }
    }

    void onAspectPrepared(float videoAspect) {
        synchronized (countAvailableLock) {
            frameCount = (int) Math.ceil(width * 1f / height);

            if (videoAspect > 1) {
                Matrix.orthoM(PMatrix, 0, -1 / videoAspect, 1 / videoAspect, -1, 1, -1, 1);
            } else {
                Matrix.orthoM(PMatrix, 0, -1, 1, -videoAspect, videoAspect, -1, 1);
            }
            Matrix.multiplyMM(MVPMatrix, 0, PMatrix, 0, VMatrix, 0);

            countAvailableLock.notifyAll();
        }
    }

    private void waitForCount() {
        synchronized (countAvailableLock) {
            if (frameCount == 0) {
                try {
                    Loggy.d("Waiting for clue");
                    countAvailableLock.wait(TIMEOUT);
                    if (frameCount == 0) {
                        Loggy.d("First frame could be damaged");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        waitForCount();
        if (surfaceEventListener != null) {
            if (pendingIndex < frameCount) {
                surfaceEventListener.drawAndMoveToNext(pendingIndex, frameCount);
            } else {
                surfaceTexture.setOnFrameAvailableListener(null);
            }
        }
    }
}
