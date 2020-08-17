package com.video.timeline.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.video.timeline.tools.FileHelper;
import com.video.timeline.ImageLoader;
import com.video.timeline.tools.Loggy;
import com.video.timeline.tools.MediaHelper;
import com.video.timeline.VideoMetadata;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MediaRetrieverAdapter extends RecyclerView.Adapter<MediaRetrieverAdapter.Holder> {
    private int frameDuration;
    private final int frameSize;
    private final ImageLoader imageLoader;
    private Context context;
    private final String mediaUri;
    private final String mediaId;
    VideoMetadata videoMetadata;
    int count;
    private final File cacheDir;

    private Handler handler = new Handler();

    private MRetriever mediaMetadataRetriever;

    private ExecutorService threadPoolExecutor;
    private HashMap<Integer, Future> tasks = new HashMap<>();


    public MediaRetrieverAdapter(Context c, String mediaUri,
                                 int frameDuration, int frameSize, ImageLoader imageLoader) {
        context = c;
        this.mediaUri = mediaUri;
        this.frameDuration = frameDuration;
        this.frameSize = frameSize;
        this.imageLoader = imageLoader;

        videoMetadata = new VideoMetadata();
        MediaHelper.getVideoMets(c, mediaUri, videoMetadata);
        count = (int) (videoMetadata.getDurationMs() / frameDuration);
        mediaId = mediaUri.substring(mediaUri.lastIndexOf('/') + 1);

        cacheDir = new File(c.getCacheDir(), ".thumbs");
        cacheDir.mkdir();

        threadPoolExecutor = Executors.newFixedThreadPool(1);

        mediaMetadataRetriever = new MRetriever(context, frameSize, Executors.newFixedThreadPool(1));
        mediaMetadataRetriever.setSource(mediaUri);
    }

    private int getIdentifier(int index) {
        return (int) (videoMetadata.getDurationMs() * index / count);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(parent.getContext());
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(frameSize, frameSize));
        return new Holder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Future task = tasks.get(holder.hashCode());
        if (task != null) {
            task.cancel(false);
        }
        ImageView imageView = (ImageView)holder.itemView;
        File cache = FileHelper.getCachedFile(cacheDir, mediaId, getIdentifier(position));
        imageLoader.load(cache, imageView);

        if (!cache.exists()) {
            Future future = threadPoolExecutor.submit(() -> {
                Loggy.d("Frame At: " + position * frameDuration);
                Bitmap frame = mediaMetadataRetriever.getScaledFrameAt(position * frameDuration * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if (frame != null) {
                    FileHelper.saveBitmapToFile(cache, frame);
                    frame.recycle();
                    handler.post(() -> notifyItemChanged(position));
                }
            });
            tasks.put(holder.hashCode(), future);
        } else {
//            imageLoader.load(cache.getAbsolutePath(), imageView);
        }
    }


    @Override
    public int getItemCount() {
        return count;
    }

    static class Holder extends RecyclerView.ViewHolder {

        public Holder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
