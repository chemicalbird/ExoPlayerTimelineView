package com.example.timlineview;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlayerMessage;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.RendererConfiguration;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.source.DefaultMediaSourceEventListener;
import com.google.android.exoplayer2.source.EmptySampleStream;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.SampleStream;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectorResult;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.video.VideoListener;
import com.google.android.exoplayer2.video.VideoRendererEventListener;
import com.video.timeline.VideoRendererOnlyFactory;

import java.io.IOException;


public class ThumbLoader implements PlayerMessage.Sender, Handler.Callback, MediaPeriod.Callback, MediaSource.MediaSourceCaller, TransferListener, TrackSelector.InvalidationListener {

    private HandlerThread internalPlaybackThread;
    private Handler defaultHandler;
    private Context context;
    private DefaultLoadControl control;
    private ExtractorMediaSource mediaSource;
    private boolean periodPrepared;
    private Renderer videoRenderer;
    private long rendererPositionUs;
    private MediaPeriod mediaPeriod;
    private DefaultTrackSelector trackSelector;
    private RendererCapabilities rendererCapabilities;
    private TrackSelectorResult selectorResult;
    private VideoListener listener;
    private long pendingSeekPositinoUs;
    private long pendingEndPositionUs;
    private DefaultExtractorsFactory extractorsFactory;

    private Timeline timeline;
    private TrimInfo trimInfo;

    private Handler eventHandler;
    private Timeline.Window window;

    public ThumbLoader(final Context context, Handler handler) {
        this.context = context;
        createWorkerThread();

        control = new DefaultLoadControl();

        extractorsFactory = new DefaultExtractorsFactory();

        eventHandler = handler;

        trackSelector = new DefaultTrackSelector(context);

        trackSelector.init(ThumbLoader.this, new DefaultBandwidthMeter.Builder(context).build());

        ComponentListener listener = new ComponentListener();

        videoRenderer = new VideoRendererOnlyFactory(context).
                createRenderers(eventHandler, listener, null, null, listener,null)[0];

        rendererCapabilities = videoRenderer.getCapabilities();

        window = new Timeline.Window();
    }

    private void createWorkerThread() {
        internalPlaybackThread = new HandlerThread("Thumbnail:Thread",
                Process.THREAD_PRIORITY_AUDIO);
        internalPlaybackThread.start();
        defaultHandler = new Handler(internalPlaybackThread.getLooper(), this);
    }

    private PlayerMessage createMessage(Renderer renderer) {
        return new PlayerMessage(this, renderer, Timeline.EMPTY, 0, defaultHandler);
    }

    public void setVideoSurface(Surface surface) {
        insureWorker();
        createMessage(videoRenderer).setType(C.MSG_SET_SURFACE).setPayload(surface).send();
    }

    public void prepare(TrimInfo trimInfo) {
        insureWorker();
        if (videoRenderer == null) {
            ComponentListener listener = new ComponentListener();
            videoRenderer = new VideoRendererOnlyFactory(context).
                    createRenderers(eventHandler, listener, null, null, listener,null)[0];

            rendererCapabilities = videoRenderer.getCapabilities();
        }

        defaultHandler.sendMessageDelayed(defaultHandler.obtainMessage(1, trimInfo), 100); //-= prepare
    }

    public void release() {
        if (mediaSource == null) return;
        defaultHandler.sendEmptyMessage(11);
    }

    public boolean isStateIdentical(String url) {
        return trimInfo != null && url.equals(trimInfo.url);
    }

    @Override
    public void onPrepared(MediaPeriod mediaPeriod) {
        defaultHandler.obtainMessage(3, mediaPeriod).sendToTarget();
    }

    @Override
    public void onContinueLoadingRequested(MediaPeriod source) {
        defaultHandler.obtainMessage(4).sendToTarget();
        Log.d("killer_john", "onContinueLoadingRequested");
    }

    public long getDurationInSeconds() {
        long endPos = pendingEndPositionUs;
        if (pendingEndPositionUs == C.TIME_END_OF_SOURCE) {
            endPos = timeline.getWindow(0, window).getDurationUs();
        }

        return (endPos - pendingSeekPositinoUs);

    }

    public void seekTo(long s) {
        if (s + rendererPositionUs < timeline.getWindow(0, window).getDurationUs()) {
            defaultHandler.obtainMessage(10, s + rendererPositionUs).sendToTarget();
        } else {
            onFinishedLoading();
        }
    }

    private void prepareInternal() {
//        DataSource.Factory cacheDataSource = new CacheDataSourceFactory(Singles.playerCache, new DefaultDataSourceFactory(context, AndroidUtil.getRealUserAgent()));
       DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(context, "exo");
        mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory)
//                .setContinueLoadingCheckIntervalBytes(100)
                .setExtractorsFactory(extractorsFactory)
                .createMediaSource(Uri.parse(trimInfo.url));

        mediaSource.prepareSource(this, this);

        mediaPeriod = mediaSource.createPeriod(new
                MediaSource.MediaPeriodId(/* periodUid= */ new Object()), control.getAllocator(), 0);

        mediaPeriod.prepare(this, 0);

        mediaSource.addEventListener(eventHandler, new DefaultMediaSourceEventListener() {
            @Override
            public void onLoadError(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
                if (error instanceof UnrecognizedInputFormatException) {
                    release();
                    eventHandler.obtainMessage(3).sendToTarget();
                }
            }
        });
    }

    private long getAdjustedSeekPosition(long pos) {
        SeekParameters seekParameters = SeekParameters.CLOSEST_SYNC;
        return mediaPeriod.getAdjustedSeekPositionUs(pos, seekParameters);
    }

    public long getKeyFramePosition(long pos) {
        try {
            SeekParameters seekParameters = SeekParameters.CLOSEST_SYNC;
            return mediaPeriod.getAdjustedSeekPositionUs(pos, seekParameters);
        } catch (NullPointerException ex) {
            return pos;
        }
    }

    public long getNextKeyFrame(long pos) {
        try {
            SeekParameters seekParameters = SeekParameters.NEXT_SYNC;
            return mediaPeriod.getAdjustedSeekPositionUs(pos + 100, seekParameters);
        } catch (NullPointerException ex) {
            return pos;
        }
    }

    @Override
    public void sendMessage(PlayerMessage message) {
        defaultHandler.obtainMessage(0, message).sendToTarget();
    }


    public void destroy() {
        defaultHandler.sendEmptyMessage(12);
    }

    public boolean isAlive() {
        return internalPlaybackThread != null && internalPlaybackThread.isAlive();
    }

    private void insureWorker() {
        if (!isAlive()) {
            if (internalPlaybackThread != null) {
                internalPlaybackThread.quit();
            }
            createWorkerThread();
        }
    }

    private void releaseResourcesInternal() {
        if (mediaSource == null) return;
        periodPrepared = false;
        defaultHandler.removeMessages(2); // -=doSomeWork
        defaultHandler.removeMessages(4); // -=continueWork
        rendererPositionUs = 0;
        try {
            if (videoRenderer.getState() == Renderer.STATE_STARTED) {
                videoRenderer.stop();
            }

            if (videoRenderer.getState() == Renderer.STATE_ENABLED) {
                videoRenderer.disable();
            }
        } catch (ExoPlaybackException e) {

        }
        mediaSource.releasePeriod(mediaPeriod);
        mediaSource.releaseSource(this);
        mediaSource = null;
        mediaPeriod = null;
        control.onReleased();
    }

    @Override
    public boolean handleMessage(Message msg) {
        Log.d("handl_test", msg.what + " " + (trimInfo != null ? trimInfo.url.substring(trimInfo.url.lastIndexOf("/")) : ""));
        try {
            switch (msg.what) {
                case 0:
                    PlayerMessage message = (PlayerMessage) msg.obj;
                    try {
                        message.getTarget().handleMessage(message.getType(), message.getPayload());
                    } catch (ExoPlaybackException e) {
                        e.printStackTrace();
                    }
                    break;

                case 1:
                    this.trimInfo = (TrimInfo) msg.obj;

                    if (periodPrepared && mediaPeriod != null) {
                        handlePeriodPrepared(mediaPeriod);
                    } else {
                        prepareInternal();
                    }
                    break;

                case 2:
                    doSomeWork();
                    break;

                case 3:
                    try {
                        handlePeriodPrepared((MediaPeriod) msg.obj);
                    } catch (ExoPlaybackException e) {
                        e.printStackTrace();
                    }

                    break;
                case 4:
                    handleContinueLoading();
                    break;

                case 10:
                    try {
                        seekToPeriodPosition((Long) msg.obj);
                    } catch (ExoPlaybackException e) {
                        e.printStackTrace();
                    }
                    break;

                case 11:
                    releaseResourcesInternal();
                    break;

                case 12:
                    releaseResourcesInternal();
                    defaultHandler.removeCallbacksAndMessages(null);
                    internalPlaybackThread.quit();
                    break;

                case 14:
                    finishInternal();
                    break;
            }
        } catch (Exception e) {
            Log.d("ex", e.getMessage());
        }
        return true;
    }

    public void finishInternal() {
        defaultHandler.removeMessages(2); // -=doSomeWork
        defaultHandler.removeMessages(4); // -=continueWork
    }

    public void onFinishedLoading() {
        defaultHandler.sendEmptyMessage(14);
    }

    private void doSomeWork() {
        if (periodPrepared) {
            try {
                videoRenderer.render(rendererPositionUs, SystemClock.elapsedRealtime() * 1000);
            } catch (ExoPlaybackException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                releaseResourcesInternal();
                Log.d("Exception", e.getMessage() != null ? e.getMessage() : "exception"); //   at android.media.MediaCodec.native_dequeueOutputBuffer(Native Method)
            }

            defaultHandler.sendEmptyMessage(2);
        }
    }

    private void handleContinueLoading() {
        long nextLoadPositionUs = mediaPeriod.getNextLoadPositionUs();
        if (nextLoadPositionUs == C.TIME_END_OF_SOURCE) {
            return;
        }
        long bufferedDurationUs = mediaPeriod.getNextLoadPositionUs();
        if (bufferedDurationUs - rendererPositionUs < 2 * C.MICROS_PER_SECOND) {
            try {
                mediaPeriod.continueLoading(rendererPositionUs);
            } catch (Exception e) {

            }
        } else {
            Log.d("adkjfdkjf", "dkfjakjfd");
        }
    }


    private void handlePeriodPrepared(MediaPeriod mediaPeriod) throws ExoPlaybackException {
        pendingSeekPositinoUs = trimInfo.startSeconds * C.MICROS_PER_SECOND;

        if (trimInfo.endSeconds != C.TIME_END_OF_SOURCE) {
            pendingEndPositionUs = trimInfo.endSeconds * C.MICROS_PER_SECOND;
        } else {
            pendingEndPositionUs = C.TIME_END_OF_SOURCE;
        }

        if (!periodPrepared) {
            TrackGroupArray trackGroups = mediaPeriod.getTrackGroups();
//            if (trackSelector.getParameters() != null) {
//                DefaultTrackSelector.Parameters parameters =
//                        trackSelector.getParameters().buildUpon()
//                                .setForceLowestBitrate(true).build();
//                trackSelector.setParameters(parameters);
//            }
            selectorResult = trackSelector.selectTracks(new RendererCapabilities[]{rendererCapabilities},
                    trackGroups, new MediaSource.MediaPeriodId(timeline.getUidOfPeriod(0)), timeline);

            updateLoadControlTrackSelection(trackGroups, selectorResult);

            TrackSelectionArray videoSelection = selectorResult.selections;

            SampleStream[] sampleStreams = new SampleStream[1];
            mediaPeriod.selectTracks(
                    videoSelection.getAll(),
                    new boolean[1],
                    sampleStreams,
                    new boolean[1],
                    0);

            if (!selectorResult.isRendererEnabled(0)) return;

            associateNoSampleRenderersWithEmptySampleStream(sampleStreams);

            RendererConfiguration rendererConfiguration = selectorResult.rendererConfigurations[0];
            Format[] formats = getFormats(videoSelection.get(0));

            videoRenderer.enable(rendererConfiguration, formats, sampleStreams[0], rendererPositionUs, false, 0);

            videoRenderer.start();

            periodPrepared = true;
//            eventHandler.obtainMessage(1, extractorsFactory.getMp4Size()).sendToTarget();
        }

        if (pendingSeekPositinoUs >= 0) {
            seekToPeriodPosition(pendingSeekPositinoUs);
        }

        defaultHandler.sendEmptyMessage(2);
    }

    private void updateLoadControlTrackSelection(
            TrackGroupArray trackGroups, TrackSelectorResult trackSelectorResult) {
        control.onTracksSelected(new Renderer[]{videoRenderer}, trackGroups, trackSelectorResult.selections);
    }

    // seeking

    private long seekToPeriodPosition(long periodPositionUs)
            throws ExoPlaybackException {
        Log.d("speed_test", "seek to " + periodPositionUs / C.MICROS_PER_SECOND + "s");
//        videoRenderer.stop();

        // Update the holders.
        if (mediaPeriod != null) {
            periodPositionUs = getAdjustedSeekPosition(periodPositionUs);

            this.rendererPositionUs = periodPositionUs;

            periodPositionUs = mediaPeriod.seekToUs(periodPositionUs);
            mediaPeriod.discardBuffer(
                    periodPositionUs, true);

            videoRenderer.resetPosition(periodPositionUs);

            Log.d("killer_john", "seek to position");
            defaultHandler.obtainMessage(4).sendToTarget();
        }

        return periodPositionUs;
    }

    @Override
    public void onTrackSelectionsInvalidated() {

    }

    @Override
    public void onTransferInitializing(DataSource source, DataSpec dataSpec, boolean isNetwork) {

    }

    @Override
    public void onTransferStart(DataSource source, DataSpec dataSpec, boolean isNetwork) {

    }

    @Override
    public void onBytesTransferred(DataSource source, DataSpec dataSpec, boolean isNetwork, int bytesTransferred) {

    }

    @Override
    public void onTransferEnd(DataSource source, DataSpec dataSpec, boolean isNetwork) {

    }

    public void setListener(VideoListener listener) {
        this.listener = listener;
    }

    @Override
    public void onSourceInfoRefreshed(MediaSource source, Timeline timeline) {
        this.timeline = timeline;
    }


    private final class ComponentListener
            implements VideoRendererEventListener,
            MetadataOutput,
            SurfaceHolder.Callback,
            TextureView.SurfaceTextureListener
    {

        // VideoRendererEventListener implementation

        @Override
        public void onVideoEnabled(DecoderCounters counters) {
        }


        @Override
        public void onVideoInputFormatChanged(Format format) {
            eventHandler.obtainMessage(2, format).sendToTarget();
        }

        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                                       float pixelWidthHeightRatio) {
            float videoAspect = ((float) width / height) * pixelWidthHeightRatio;
            if (listener != null) {
                listener.onVideoSizeChanged(width, height,unappliedRotationDegrees, pixelWidthHeightRatio);
            }
        }

        @Override
        public void onRenderedFirstFrame(Surface surface) {

        }

        @Override
        public void onVideoDisabled(DecoderCounters counters) {
        }

        @Override
        public void onMetadata(Metadata metadata) {
//            for (MetadataOutput metadataOutput : metadataOutputs) {
//                metadataOutput.onMetadata(metadata);
//            }
        }

        // SurfaceHolder.Callback implementation

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
//            setVideoSurfaceInternal(holder.getSurface(), false);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//            maybeNotifySurfaceSizeChanged(width, height);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
//            setVideoSurfaceInternal(null, false);
//            maybeNotifySurfaceSizeChanged(/* width= */ 0, /* height= */ 0);
        }

        // TextureView.SurfaceTextureListener implementation

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
//            setVideoSurfaceInternal(new Surface(surfaceTexture), true);
//            maybeNotifySurfaceSizeChanged(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
//            maybeNotifySurfaceSizeChanged(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
//            setVideoSurfaceInternal(null, true);
//            maybeNotifySurfaceSizeChanged(/* width= */ 0, /* height= */ 0);
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
            // Do nothing.
        }

    }

    private void associateNoSampleRenderersWithEmptySampleStream(SampleStream[] sampleStreams) {
        if (rendererCapabilities.getTrackType() == C.TRACK_TYPE_NONE
                && selectorResult.isRendererEnabled(0)) {
            sampleStreams[0] = new EmptySampleStream();
        }
    }

    private static Format[] getFormats(TrackSelection newSelection) {
        // Build an array of formats contained by the selection.
        int length = newSelection != null ? newSelection.length() : 0;
        Format[] formats = new Format[length];
        for (int i = 0; i < length; i++) {
            formats[i] = newSelection.getFormat(i);
        }
        return formats;
    }

}
