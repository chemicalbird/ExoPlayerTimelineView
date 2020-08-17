package com.video.timeline;


import android.content.Context;
import android.os.Handler;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecInfo;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class VideoRendererOnlyFactory extends DefaultRenderersFactory {
    private Context context;
    private boolean preferSoftware;
    private Renderer mediaCodecVideoRenderer;

    private MediaCodecSelector selector = new MediaCodecSelector() {
        @NotNull
        @Override
        public List<MediaCodecInfo> getDecoderInfos(@NotNull String mimeType, boolean requiresSecureDecoder, boolean requiresTunnelingDecoder) throws MediaCodecUtil.DecoderQueryException {
            List<MediaCodecInfo> codecs =  MediaCodecUtil.getDecoderInfos(
                    mimeType, requiresSecureDecoder, requiresTunnelingDecoder);
            return preferSoftware ? getDecodersSortedSoftwareFirst(codecs) : codecs;
        }

        public List<MediaCodecInfo> getDecodersSortedSoftwareFirst(
                List<MediaCodecInfo> decoderInfos) {
            decoderInfos = new ArrayList<>(decoderInfos);
            Collections.sort(decoderInfos, new Comparator<MediaCodecInfo>() {
                @Override
                public int compare(MediaCodecInfo o1, MediaCodecInfo o2) {
                    if (o1.softwareOnly && !o2.softwareOnly) {
                        return -1;
                    } else if (o2.softwareOnly && !o1.softwareOnly) {
                        return 1;
                    }
                    return 0;
                }
            });
            return decoderInfos;

        }

        @Nullable
        @Override
        public MediaCodecInfo getPassthroughDecoderInfo() throws MediaCodecUtil.DecoderQueryException {
            return MediaCodecUtil.getPassthroughDecoderInfo();
        }
    };

    public VideoRendererOnlyFactory(Context context, boolean preferSoftware) {
        super(context);
        this.context = context;
        this.preferSoftware = preferSoftware;
    }

    @NotNull
    @Override
    public Renderer[] createRenderers(Handler eventHandler, @NotNull VideoRendererEventListener videoRendererEventListener,
                                      AudioRendererEventListener audioRendererEventListener,
                                      TextOutput textRendererOutput, @NotNull MetadataOutput metadataRendererOutput,
                                      @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager) {
        ArrayList<Renderer> rendererList = new ArrayList<>();

        buildVideoRenderers(context, EXTENSION_RENDERER_MODE_OFF,
                selector, drmSessionManager, false, true,
                eventHandler,videoRendererEventListener, 1000, rendererList);
        if (rendererList.get(0) instanceof MediaCodecVideoRenderer) {
            mediaCodecVideoRenderer = rendererList.get(0);
        }
        return rendererList.toArray(new Renderer[0]);
    }

    public Renderer getMediaCodecVideoRenderer() {
        return mediaCodecVideoRenderer;
    }
}