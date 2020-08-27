package com.video.timeline;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.video.VideoListener;
import com.video.timeline.android.MRetriever;
import com.video.timeline.render.RetroRenderer;
import com.video.timeline.tools.FileHelper;
import com.video.timeline.tools.Loggy;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.Executors;

public class RetroInstance implements RetroSurfaceListener, VideoListener, Player.EventListener {

    private final int size;
    private Context context;

    private RetroRenderer offscreenSurface;
    private LinkedList<Task> jobs = new LinkedList<>();
    private HashMap<Integer, Task> jobHashMap = new HashMap<>();
    private Task currentJob;

    private ExoPlayerFactory videoPlayerFactory;
    private VideoFrameCache videoFrameCache;
    private SimpleExoPlayer player;
    private MediaSource explicitMediaSource;

    private Handler mainHandler = new Handler();
    private long playerPosition;
    private long lastSeekPos;
    private MRetriever mediaMetRetreiver;

    private String currentPreparedSource;
    private long time;
    private long totalTime;

    private RetroInstance(ExoPlayerFactory playerFactory, int size, Context context, File cacheDir) {
        this.size = size;
        this.context = context;
        this.videoPlayerFactory = playerFactory;
        this.videoFrameCache = new VideoFrameCache(cacheDir);
    }

    public File getCacheDir() {
        return videoFrameCache.getCacheDir();
    }

    public void setPlayerInstance(SimpleExoPlayer player) {
        this.player = player;
    }

    public void setExplicitMediaSource(MediaSource explicitMediaSource) {
        this.explicitMediaSource = explicitMediaSource;
        if (player != null) {
            player.prepare(explicitMediaSource);
        }
    }

    public void load(String mediaUri, Long presentationMs, int hash, FetchCallback<File> callback) {
        Task previous = jobHashMap.get(hash);
        if (previous != null) {
            jobs.remove(previous);
            jobHashMap.remove(hash);
        }

        Task job = new Task(mediaUri, presentationMs, hash, callback);

        File cache = findCache(job);
        if (cache != null) {
            job.callback.onSuccess(cache);
        } else {
            jobHashMap.put(hash, job);
            jobs.add(job);
            execute();
        }
    }

    private File findCache(Task item) {
        File cache = videoFrameCache.fileAt(item.mediaUri, item.time);
        return cache != null && cache.exists() ? cache : null;
    }

    private void execute() {
        if (currentJob == null && !jobs.isEmpty()) {
            currentJob = jobs.poll();
            time = SystemClock.elapsedRealtime();
            if (currentJob != null) {
                File cache = findCache(currentJob);
                if (cache != null) {
                    Log.d("retro_study", "Cache hit: " + currentJob.time);
                    currentJob.callback.onSuccess(cache);
                    jobHashMap.remove(currentJob.hash);
                    next();
                } else if (mediaMetRetreiver != null) { // fallback is activated
                    loadUsingFallbackMethod();
                } else {
                    checkPlayerAndSurface();
                    Log.d("retro_study", "Decode: " + currentJob.time);
                    if (currentJob.time >= 0) {
                        playerPosition = player.getCurrentPosition();
                        lastSeekPos = playerPosition == currentJob.time ? currentJob.time + 100L : currentJob.time;
                        player.seekTo(lastSeekPos);
                    }
                }
            } else {
                execute();
            }
        }
    }

    private void next() {
        currentJob = null;
        execute();
    }

    private void checkPlayerAndSurface() {
        if (player == null) {
            prepareVideoPlayer();
        }
        if (explicitMediaSource != null) {
            if (isIdle()) {
                player.prepare(explicitMediaSource);
            }
        } else if (!currentJob.mediaUri.equals(currentPreparedSource) || isIdle()) {
            player.prepare(videoPlayerFactory.getMediaSource(currentJob.mediaUri, context));
            currentPreparedSource = currentJob.mediaUri;
            if (offscreenSurface != null) {
                offscreenSurface.clearTxtMtx();
            }
        }
        if (offscreenSurface == null) {
            offscreenSurface = new RetroRenderer(size, size, this);
        }
    }

    private void prepareVideoPlayer() {
        player = videoPlayerFactory.getPlayer(context);
        player.addVideoListener(this);
        player.addListener(this);
    }

    private boolean isIdle() {
        return player.getPlaybackState() == Player.STATE_IDLE;
    }

    public void stop(boolean reset) {
        if (player != null) {
            player.stop(reset);
        }
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Loggy.d("Player err: " + (error != null ? error.getMessage() : ""));
        stop(true);
        if (error != null && error.type == ExoPlaybackException.TYPE_SOURCE) {
            currentJobFinished();
            return;
        }

        if (explicitMediaSource == null) { // fallback not handled for explicit mediaSource
            if (mediaMetRetreiver == null) {
                mediaMetRetreiver = new MRetriever(context, frameSize(),
                        Executors.newFixedThreadPool(1));
            }
            loadUsingFallbackMethod();
        }
    }

    private void loadUsingFallbackMethod() {
        if (currentJob != null) {
            mediaMetRetreiver.frameAt(currentJob.mediaUri, currentJob.time, result -> {
                File cacheWrite = videoFrameCache.fileAt(currentJob.mediaUri, currentJob.time);
                if (cacheWrite != null && result != null) {
                    FileHelper.saveBitmapToFile(cacheWrite, result);
                    result.recycle();
                }
                mainHandler.post(this::currentJobFinished);
            }, currentJob.hash);
        }
    }

    @Override
    public void onSurfaceAvailable(Surface surface) {
        mainHandler.post(() -> this.player.setVideoSurface(surface));
    }

    @Override
    public void onTextureRetrieved(ByteBuffer pixelBuffer) {
        if (currentJob == null) return; // ?investigate

        totalTime += (SystemClock.elapsedRealtime() - time);
        File cacheWrite = videoFrameCache.fileAt(currentJob.mediaUri, currentJob.time);
        if (cacheWrite != null) {
            FileHelper.saveToFile(cacheWrite, pixelBuffer, size, size);
        }
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                currentJobFinished();
            }
        });
    }

    private void currentJobFinished() {
        if (currentJob != null) {
            currentJob.callback.onSuccess(videoFrameCache.fileAt(currentJob.mediaUri, currentJob.time));
            jobHashMap.remove(currentJob.hash);
        }
        next();
    }

    @Override
    public void onSeekProcessed() {
        if (currentJob == null) return;
        if (player != null && player.getDuration() > 0) {
            if ((lastSeekPos != 0 && player.getCurrentPosition() == playerPosition) || lastSeekPos > player.getDuration()) {
                offscreenSurface.drawSameFrame();
            }
        }
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        float videoAspect = ((float) width / height) * pixelWidthHeightRatio;
        offscreenSurface.onVideoAspectChanged(videoAspect);
    }

    public void onDestroy() {
        jobs.clear();
        jobHashMap.clear();
        currentJob = null;
        if (player != null) {
            player.removeListener(this);
            player.removeVideoListener(this);
            player.release();
            player = null;
        }

        if (offscreenSurface != null) {
            offscreenSurface.release();
            offscreenSurface = null;
        }

        if (mediaMetRetreiver != null) {
            mediaMetRetreiver.release();
            mediaMetRetreiver = null;
        }
    }

    public int frameSize() {
        return size;
    }

    public static class Builder {
        private static final int DEFAULT_SIZE = 80;

        private Context context;
        private int size;
        private SeekParameters seekParams;
        private File cacheDir;
        private boolean preferSoftwareDecoder;
        private DataSource.Factory dataSourceFactory;

        public Builder(Context context) {
            this.context = context;
            this.size = DEFAULT_SIZE;
            seekParams = SeekParameters.CLOSEST_SYNC;
            cacheDir = new File(context.getCacheDir(), ".frames");
        }

        public Builder setSeekParams(SeekParameters seekParams) {
            this.seekParams = seekParams;
            return this;
        }

        public Builder softwareDecoder(boolean flag) {
            this.preferSoftwareDecoder = flag;
            return this;
        }

        public Builder setFrameSizeDp(int size) {
            this.size = size;
            return this;
        }

        public Builder cache(File dir) {
            this.cacheDir = dir;
            return this;
        }

        public Builder sourceFactory(DataSource.Factory factory) {
            this.dataSourceFactory = factory;
            return this;
        }

        public RetroInstance create() {
            return new RetroInstance(new ExoPlayerFactory(seekParams, preferSoftwareDecoder, dataSourceFactory),
                    size, context, cacheDir);
        }
    }

    static class Task {
        private String mediaUri;
        final long time;
        private int hash;
        final FetchCallback<File> callback;

        public Task(String mediaUri, long time, int hash, FetchCallback<File> callback) {
            this.mediaUri = mediaUri;
            this.time = time;
            this.hash = hash;
            this.callback = callback;
        }
    }
}
