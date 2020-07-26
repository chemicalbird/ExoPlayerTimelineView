package com.video.timeline;

import android.content.Context;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Surface;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
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

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

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

    private Handler mainHandler = new Handler();
    private long playerPosition;
    private long lastSeekPos;
    private MRetriever mediaMetRetreiver;

    private RetroInstance(ExoPlayerFactory playerFactory, int size, Context context, File cacheDir) {
        this.size = size;
        this.context = context;
        this.videoPlayerFactory = playerFactory;
        this.videoFrameCache = new VideoFrameCache(cacheDir, playerFactory.getMediaUri());
    }

    public void load(Long presentationMs, int hash, FetchCallback<File> callback) {
        Task previous = jobHashMap.get(hash);
        if (previous != null) {
            boolean removed1 = jobs.remove(previous);
            Log.d("hash_study", "Rem1: " + removed1);
        }
        Task job = new Task(presentationMs, hash, callback);
        jobHashMap.put(hash, job);
        jobs.add(job);

        execute();
    }

    private void execute() {
        if (currentJob == null && !jobs.isEmpty()) {
            currentJob = jobs.poll();
            if (currentJob != null) {
                File cache = videoFrameCache.fileAt(currentJob.time);
                if (cache != null && cache.exists()) {
                    Log.d("retro_study", "Cache hit: " + currentJob.time);
                    currentJob.callback.onSuccess(cache);
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
        jobHashMap.remove(currentJob.hash);
        currentJob = null;
        execute();
    }

    private void checkPlayerAndSurface() {
        if (player == null) {
            prepareVideoPlayer();
        }
        if (offscreenSurface == null) {
            offscreenSurface = new RetroRenderer(size, size, this);
        }
    }

    private void prepareVideoPlayer() {
        player = videoPlayerFactory.getPlayer(context);
        player.prepare(videoPlayerFactory.getMediaSource(context));
        player.addVideoListener(this);
        player.addListener(this);
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Loggy.d("Player err: " + (error != null ? error.getMessage() : ""));
        // Improve: use fallback only for render type errors

        if (mediaMetRetreiver == null) {
            mediaMetRetreiver = new MRetriever(context, videoPlayerFactory.getMediaUri(), frameSize(),
                    Executors.newFixedThreadPool(1));
        }

        loadUsingFallbackMethod();
    }

    private void loadUsingFallbackMethod() {
        if (currentJob != null) {
            mediaMetRetreiver.frameAt(currentJob.time, result -> {
                File cacheWrite = videoFrameCache.fileAt(currentJob.time);
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
        File cacheWrite = videoFrameCache.fileAt(currentJob.time);
        if (cacheWrite != null) {
            FileHelper.saveToFile(cacheWrite, pixelBuffer, size, size);
        }
        mainHandler.post(this::currentJobFinished);
    }

    private void currentJobFinished() {
        if (currentJob != null) {
            Log.d("retro_study", "Done: " + currentJob.time + " Player: " + player.getCurrentPosition());
            currentJob.callback.onSuccess(videoFrameCache.fileAt(currentJob.time));
        }
        next();
    }

    @Override
    public void onSeekProcessed() {
        if (player != null && lastSeekPos <= player.getDuration()) {
            if (lastSeekPos != 0 && player.getCurrentPosition() == playerPosition) {
                Log.d("retro_study", String.format("Player: %s, lasSeek: %s", playerPosition, lastSeekPos));
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
        if (player != null) {
            player.release();
            player = null;
        }

        if (offscreenSurface != null) {
            offscreenSurface.release();
        }
    }

    public int frameSize() {
        return size;
    }

    public static class Builder {
        private static final int DEFAULT_SIZE = 80;

        private Context context;
        private final String mediaURI;
        private int size;
        private SeekParameters seekParams;
        private File cacheDir;

        private int dpToPx(float dpValue, Context context) {
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            return (int) TypedValue.applyDimension(COMPLEX_UNIT_DIP, dpValue, dm);
        }

        public Builder(Context context, String mediaURI) {
            this.context = context;
            this.mediaURI = mediaURI;
            this.size = DEFAULT_SIZE;
            seekParams = SeekParameters.CLOSEST_SYNC;
            cacheDir = new File(context.getCacheDir(), ".frames");
        }

        public Builder setSeekParams(SeekParameters seekParams) {
            this.seekParams = seekParams;
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

        public RetroInstance create() {
            return new RetroInstance(new ExoPlayerFactory(mediaURI, seekParams), size, context, cacheDir);
        }
    }

    static class Task {
        final long time;
        private int hash;
        final FetchCallback<File> callback;

        public Task(long time, int hash, FetchCallback<File> callback) {
            this.time = time;
            this.hash = hash;
            this.callback = callback;
        }
    }
}
