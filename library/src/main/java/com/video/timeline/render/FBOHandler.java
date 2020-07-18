package com.video.timeline.render;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.otaliastudios.opengl.draw.GlDrawable;
import com.otaliastudios.opengl.draw.GlRect;
import com.otaliastudios.opengl.texture.GlFramebuffer;
import com.otaliastudios.opengl.texture.GlTexture;

import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_FRAMEBUFFER_BINDING;
import static android.opengl.GLES20.GL_RENDERBUFFER;
import static android.opengl.GLES20.GL_RENDERBUFFER_BINDING;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_BINDING_2D;

public class FBOHandler {

    private GlFramebuffer glFramebuffer;
    private TextureProgram glProgram;

    public FBOHandler(@NonNull TextureProgram glProgram) {
        this.glProgram = glProgram;
        initialize();
    }

    private void initialize() {
        if (glFramebuffer == null) {
            glFramebuffer = new GlFramebuffer();
        }
    }

    public GlTexture configure(int width, int height) {
        final int[] args = new int[1];

        GLES20.glGetIntegerv(GL_FRAMEBUFFER_BINDING, args, 0);
        final int saveFramebuffer = args[0];
        GLES20.glGetIntegerv(GL_RENDERBUFFER_BINDING, args, 0);
        final int saveRenderbuffer = args[0];
        GLES20.glGetIntegerv(GL_TEXTURE_BINDING_2D, args, 0);
        final int saveTexName = args[0];

        GlTexture frameBufferTexture = new GlTexture(GLES20.GL_TEXTURE0, GL_TEXTURE_2D, width, height, GLES20.GL_RGBA);
        frameBufferTexture.bind();
        glFramebuffer.attach(frameBufferTexture);
        frameBufferTexture.unbind();

        GLES20.glBindFramebuffer(GL_FRAMEBUFFER, saveFramebuffer);
        GLES20.glBindRenderbuffer(GL_RENDERBUFFER, saveRenderbuffer);
        GLES20.glBindTexture(GL_TEXTURE_2D, saveTexName);

        return frameBufferTexture;
    }

    float[] getTextureTransform() {
        return glProgram.getTextureTransform();
    }

    public void draw(GlRect drawable, float[] mvp, float[] tex) {
        glFramebuffer.bind();
        glProgram.setTextureTransform(tex);
        glProgram.draw(drawable, mvp);
    }

    void bind() {
        glFramebuffer.bind();
    }

    public void draw(GlDrawable drawable, float[] mvp) {
        glProgram.draw(drawable, mvp);
    }

    public void unbind() {
        glFramebuffer.unbind();
    }

    public void release() {
        if (glFramebuffer != null) {
            glFramebuffer.release();
        }

        if (glProgram != null) {
            glProgram.release();
        }
    }

}
