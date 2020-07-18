package com.example.timlineview;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.squareup.picasso.Picasso;
import com.video.timeline.ImageLoader;
import com.video.timeline.RetroInstance;
import com.video.timeline.VideoMetadata;
import com.video.timeline.render.TimelineGlSurfaceView;
import com.video.timeline.VideoTimeLine;
import com.video.timeline.android.MediaRetrieverAdapter;
import com.video.timeline.tools.MediaHelper;

import java.io.File;

public class NewActivity extends AppCompatActivity implements View.OnClickListener {
    private SimpleExoPlayer player;

    private VideoTimeLine fixedVideoTimeline;

    private ImageLoader picassoLoader = (file, view) -> {
        if (file == null || !file.exists()) {
            view.setImageDrawable(new ColorDrawable(Color.LTGRAY));
        } else {
            Picasso.get().load(Uri.fromFile(file))
                    .placeholder(new ColorDrawable(Color.LTGRAY)).into(view);
        }
    };
    private RecyclerView defaultListView;
    private RecyclerView retroListView;
    private RetroInstance retroInstance;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String fileUri = getIntent().getStringExtra("file_uri");
        setContentView(R.layout.preview3);
        findViewById(R.id.show_fixed).setOnClickListener(this);

        PlayerView playerView = findViewById(R.id.playerView);

        MediaSource mediaSource = new ProgressiveMediaSource.Factory
                (new DefaultDataSourceFactory(this, "geo"), new DefaultExtractorsFactory())
                .createMediaSource(URLUtil.isNetworkUrl(fileUri) ? Uri.parse(fileUri) : Uri.fromFile(new File(fileUri)));

        player = new SimpleExoPlayer.Builder(this).build();
        player.prepare(mediaSource);
        playerView.setPlayer(player);


        TimelineGlSurfaceView glSurfaceView = findViewById(R.id.fixed_thumb_list);
        fixedVideoTimeline = VideoTimeLine.with(fileUri)
                .into(glSurfaceView);

        playerView.getVideoSurfaceView().setOnClickListener(v -> player.setPlayWhenReady(!player.getPlayWhenReady()));

        findViewById(R.id.cache_clear).setOnClickListener(v -> {
            deleteCache(new File(getCacheDir(), "exo_frames"));
            deleteCache(new File(getCacheDir(), ".thumbs"));
            deleteCache(new File(getCacheDir(), ".frames"));
            deleteCache(new File(Environment.getExternalStorageDirectory(), "ACache"));
        });

        defaultListView = findViewById(R.id.default_list_view);
        defaultListView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        findViewById(R.id.show_default).setOnClickListener(this);

        retroListView = findViewById(R.id.retro_list_view);
        retroListView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        findViewById(R.id.show_retro).setOnClickListener(this);

        retroListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    long seekPos = (long) (recyclerView.computeHorizontalScrollOffset() * 1F /
                            recyclerView.computeHorizontalScrollRange()
                            * player.getDuration());
                    player.seekTo(seekPos);
                    findViewById(R.id.show_retro).setEnabled(true);
                } else {
                    findViewById(R.id.show_retro).setEnabled(false);
                }
            }
        });

        defaultListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                findViewById(R.id.show_default).setEnabled(newState == RecyclerView.SCROLL_STATE_IDLE);
            }
        });
    }

    private void deleteCache(File dir) {
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files == null) return;

            for (File el: files) {
                el.delete();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();

        if (fixedVideoTimeline != null) {
            fixedVideoTimeline.destroy();
        }

        if (retroInstance != null) {
            retroInstance.onDestroy();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.show_fixed) {
            fixedVideoTimeline.start();
        } else if (v.getId() == R.id.show_default) {
            MediaRetrieverAdapter adapter = new MediaRetrieverAdapter(this,
                    getIntent().getStringExtra("file_uri"), 2000, 180, picassoLoader);
            defaultListView.setAdapter(adapter);
        } else if (v.getId() == R.id.show_retro) {
            String mediaUri = getIntent().getStringExtra("file_uri");
            VideoMetadata videoMetadata = new VideoMetadata();
            MediaHelper.getVideoMets(this, mediaUri, videoMetadata);

            if (retroInstance == null) {
                retroInstance = new RetroInstance.Builder(this, getIntent().getStringExtra("file_uri"))
                        .setFrameSizeDp(180)
                        .create();
            }

            retroListView.setAdapter(
                    new VideoFrameAdapter(retroInstance, 2000, videoMetadata.getDurationMs(), picassoLoader));
        }
    }
}
