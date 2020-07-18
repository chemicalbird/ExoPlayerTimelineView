package com.video.timeline;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

public class ExoPlayerFactory {

    private String mediaUri;
    private SeekParameters seekParameters;

    private SimpleExoPlayer exoPlayer;

    public ExoPlayerFactory(String mediaUri, SeekParameters seekParameters) {
        this.mediaUri = mediaUri;
        this.seekParameters = seekParameters;
    }

    public SimpleExoPlayer getPlayer(Context context) {
        if (exoPlayer == null) {
            exoPlayer = new SimpleExoPlayer.Builder(context, new VideoRendererOnlyFactory(context))
                    .setLoadControl(
                            new DefaultLoadControl.Builder()
                                    .setBufferDurationsMs(
                                            100, 100, 100, 100)
                                    .createDefaultLoadControl()
                    )
                    .build();
            exoPlayer.setSeekParameters(seekParameters);
        }

        return exoPlayer;
    }

    public MediaSource getMediaSource(Context context) {
        return new ProgressiveMediaSource.Factory(new DefaultDataSourceFactory(context, "exo"))
                .createMediaSource(Uri.parse(mediaUri));
    }

    public void destroyInstances() {
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    public String getMediaUri() {
        return mediaUri;
    }
}
