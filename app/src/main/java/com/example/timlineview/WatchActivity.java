package com.example.timlineview;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.URLUtil;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.video.timeline.tests.gl.PreviewListView;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.video.timeline.VideoRendererOnlyFactory;

import java.io.File;

public class WatchActivity extends AppCompatActivity {

    private SimpleExoPlayer player;
    private ThumbLoader renderer;
    private PreviewListView thumbnailsView;
    private ProgressiveMediaSource mediaSource;
    private SimpleExoPlayer thumbPlayer;
    private String fileUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fileUri = getIntent().getStringExtra("file_uri");
        setContentView(R.layout.preview);

        PlayerView playerView = findViewById(R.id.playerView);

        mediaSource = new ProgressiveMediaSource.Factory
                (new DefaultDataSourceFactory(this, "geo"), new DefaultExtractorsFactory())
                .createMediaSource(URLUtil.isNetworkUrl(fileUri) ? Uri.parse(fileUri) : Uri.fromFile(new File(fileUri)));

        player = new SimpleExoPlayer.Builder(this)
                .setLoadControl(
                        new DefaultLoadControl.Builder()
                                .setBufferDurationsMs(3000, 3000, 1000, 2000)
                        .createDefaultLoadControl()
                )
                .build();
        player.prepare(mediaSource);
        player.addListener(new Player.EventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, int reason) {
                Timeline.Window window = new Timeline.Window();
                timeline.getWindow(0, window);
                if (window.durationUs > 0) {
                    generateThumbnails(fileUri, 0, window.durationUs / C.MICROS_PER_SECOND);
                }
            }
        });

        playerView.setPlayer(player);

        thumbnailsView = findViewById(R.id.thumb_list);

        renderer = new ThumbLoader(thumbnailsView.getContext(),
                new Handler(Looper.getMainLooper(), msg -> {
                    switch (msg.what) {
                        case 1:
                            break;

                        case 2:
                            break;

                        case 3: // error
//                            hideThumbView();
                            break;
                    }
                    return false;
                }));

        thumbPlayer = new SimpleExoPlayer.Builder(this, new VideoRendererOnlyFactory(this))
                .setLoadControl(
                        new DefaultLoadControl.Builder()
                                .setBufferDurationsMs(
                                        1000, 1000, 500, 1000)
                                .createDefaultLoadControl()
                )
                .setTrackSelector(new DefaultTrackSelector(this) {

                })
                .build();
        thumbPlayer.setSeekParameters(SeekParameters.NEXT_SYNC);
        thumbnailsView.setSimpleExoPlayer(thumbPlayer);
    }

    public void generateThumbnails(String url, long s, long e) {
        TrimInfo trimInfo = new TrimInfo(s, e, url);
        if (renderer != null && !renderer.isStateIdentical(url)) {
            renderer.release();
        }


        thumbPlayer.prepare(new ProgressiveMediaSource.Factory
                (new DefaultDataSourceFactory(this, "geo"), new DefaultExtractorsFactory())
                .createMediaSource(Uri.fromFile(new File(fileUri))));

        thumbnailsView.getRenderer().clearState();
//        renderer.prepare(trimInfo);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
        renderer.destroy();
        thumbPlayer.release();
    }
}
