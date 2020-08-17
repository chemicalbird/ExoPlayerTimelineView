package com.video.timeline;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

public class ExoPlayerFactory {

    private SeekParameters seekParameters;
    private boolean preferSoftwareDecoder;
    private DataSource.Factory dataSourceFactory;

    ExoPlayerFactory(SeekParameters seekParameters) {
        this(seekParameters, false, null);
    }

    ExoPlayerFactory(SeekParameters seekParameters,
                     boolean preferSoftwareDecoder,
                     DataSource.Factory dataSourceFactory) {
        this.seekParameters = seekParameters;
        this.preferSoftwareDecoder = preferSoftwareDecoder;
        this.dataSourceFactory = dataSourceFactory;
    }

    public SimpleExoPlayer getPlayer(Context context) {
        int bufferMs = preferSoftwareDecoder ? 100 : 2000;
        SimpleExoPlayer exoPlayer = new SimpleExoPlayer.Builder(context, new VideoRendererOnlyFactory(context, preferSoftwareDecoder))
                .setLoadControl(
                        new DefaultLoadControl.Builder()
                                .setBufferDurationsMs(
                                        bufferMs, bufferMs, 0, 0)
                                .createDefaultLoadControl()
                )
                .build();
        exoPlayer.setSeekParameters(seekParameters);
        return exoPlayer;
    }

    public MediaSource getMediaSource(String mediaUri, Context context) {
        if (dataSourceFactory == null) {
            dataSourceFactory = new DefaultDataSourceFactory(context, "exo");
        }
        return new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(mediaUri));
    }
}
