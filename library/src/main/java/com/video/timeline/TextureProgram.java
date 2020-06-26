package com.video.timeline;

import android.opengl.GLES20;

import com.otaliastudios.opengl.core.Egloo;
import com.otaliastudios.opengl.draw.GlDrawable;
import com.otaliastudios.opengl.extensions.BuffersKt;
import com.otaliastudios.opengl.program.GlProgram;
import com.otaliastudios.opengl.program.GlProgramLocation;
import com.otaliastudios.opengl.texture.GlTexture;

import org.jetbrains.annotations.NotNull;

import java.nio.FloatBuffer;

public class TextureProgram extends GlProgram {

    public static final String SIMPLE_VERTEX_SHADER =
            "" +
                    "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uTexMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMVPMatrix * aPosition;\n" +
                    "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
                    "}\n";

    public static final String OES_FRAGMENT_SHADER =
            "" +
                    "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";

    public static final String SIMPLE_FRAGMENT_SHADER =
            "precision mediump float;\n" +
                    "varying highp vec2 vTextureCoord;\n" +
                    "uniform lowp sampler2D sTexture;\n" +
                    "void main() {\n" +
                    "gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";

    private final GlProgramLocation textureTransformHandle;
    private final GlProgramLocation textureCoordsHandle;
    private final GlProgramLocation vertexPositionHandle;
    private final GlProgramLocation vertexMvpMatrixHandle;
    private final FloatBuffer textureCoordsBuffer;

    private float[] textureTransform;
    private GlTexture texture;

    private float[] textureCoords = {
            0, 0,
            1, 0,
            0, 1,
            1, 1
    };

    static TextureProgram createOESProgram(GlTexture texture) {
        return new TextureProgram(TextureProgram.SIMPLE_VERTEX_SHADER,
                TextureProgram.OES_FRAGMENT_SHADER, texture);
    }

    public TextureProgram(@NotNull String vertexShader, @NotNull String fragmentShader) {
        this(create(vertexShader, fragmentShader), true);
    }

    private TextureProgram(@NotNull String vertexShader, @NotNull String fragmentShader, GlTexture texture) {
        this(create(vertexShader, fragmentShader), true);
        this.texture = texture;
    }

    private TextureProgram(int handle, boolean ownsHandle) {
        super(handle, ownsHandle);
        textureTransform = Egloo.IDENTITY_MATRIX.clone();
        textureTransformHandle = getUniformHandle("uTexMatrix");
        textureCoordsHandle = getAttribHandle("aTextureCoord");
        vertexPositionHandle = getAttribHandle("aPosition");
        vertexMvpMatrixHandle = getUniformHandle("uMVPMatrix");

        textureCoordsBuffer = BuffersKt.floatBufferOf(textureCoords);
    }

    public float[] getTextureTransform() {
        return textureTransform;
    }

    public void setTextureTransform(float[] textureTransform) {
        this.textureTransform = textureTransform;
    }

    @Override
    public void onPreDraw(@NotNull GlDrawable drawable, @NotNull float[] modelViewProjectionMatrix) {
        super.onPreDraw(drawable, modelViewProjectionMatrix);

        if (texture != null) {
            texture.bind();
        }

        GLES20.glUniformMatrix4fv(vertexMvpMatrixHandle.getValue(), 1, false, modelViewProjectionMatrix, 0);
        Egloo.checkGlError("glUniformMatrix4fv");

        GLES20.glUniformMatrix4fv(textureTransformHandle.getValue(), 1, false, textureTransform, 0);
        Egloo.checkGlError("glUniformMatrix4fv");

        GLES20.glEnableVertexAttribArray(vertexPositionHandle.getValue());
        Egloo.checkGlError("glEnableVertexAttribArray");
        GLES20.glVertexAttribPointer(vertexPositionHandle.getValue(), 2,
                GLES20.GL_FLOAT,
                false,
                drawable.getVertexStride(),
                drawable.getVertexArray());
        Egloo.checkGlError("glVertexAttribPointer");

        GLES20.glEnableVertexAttribArray(textureCoordsHandle.getValue());
        Egloo.checkGlError("glEnableVertexAttribArray");
        GLES20.glVertexAttribPointer(textureCoordsHandle.getValue(), 2,
                GLES20.GL_FLOAT,
                false,
                drawable.getVertexStride(),
                textureCoordsBuffer);
        Egloo.checkGlError("glVertexAttribPointer");
    }

    @Override
    public void onPostDraw(@NotNull GlDrawable drawable) {
        super.onPostDraw(drawable);
        GLES20.glDisableVertexAttribArray(vertexPositionHandle.getValue());
        GLES20.glDisableVertexAttribArray(textureCoordsHandle.getValue());
        if (texture != null) {
            texture.unbind();
        }
    }

    @Override
    public void release() {
        super.release();
        if (texture != null) {
            texture.release();
            texture = null;
        }
    }

    public void setTexture(GlTexture texture) {
        this.texture = texture;
    }
}
