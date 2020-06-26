package com.video.timeline;


import android.content.Context;
import android.os.Handler;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class VideoRendererOnlyFactory extends DefaultRenderersFactory {
    private Context context;

    public VideoRendererOnlyFactory(Context context) {
        super(context);
        this.context = context;
    }

    @NotNull
    @Override
    public Renderer[] createRenderers(Handler eventHandler, @NotNull VideoRendererEventListener videoRendererEventListener,
                                      AudioRendererEventListener audioRendererEventListener,
                                      TextOutput textRendererOutput, @NotNull MetadataOutput metadataRendererOutput,
                                      @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager) {
        ArrayList<Renderer> rendererList = new ArrayList<>();

        buildVideoRenderers(context, EXTENSION_RENDERER_MODE_OFF,
                MediaCodecSelector.DEFAULT, drmSessionManager, false, false,
                eventHandler,videoRendererEventListener, 1000, rendererList);

        return rendererList.toArray(new Renderer[0]);
    }
}