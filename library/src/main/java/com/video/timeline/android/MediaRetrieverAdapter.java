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

    private MediaMetadataRetriever mediaMetadataRetriever;

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
            if (mediaMetadataRetriever == null) {
                mediaMetadataRetriever = new MediaMetadataRetriever();
                mediaMetadataRetriever.setDataSource(context, Uri.parse(mediaUri));
            }
            Future future = threadPoolExecutor.submit(() -> {
                Bitmap bitmap = mediaMetadataRetriever.getFrameAtTime(position * frameDuration * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                Loggy.d("Frame At: " + position * frameDuration);
                try {
                    int targetWidth;
                    int targetHeight;
                    if (bitmap.getHeight() > bitmap.getWidth()) {
                        targetHeight = frameSize;
                        float percentage = frameSize * 1F / bitmap.getHeight();
                        targetWidth = (int) (bitmap.getWidth() * percentage);
                    } else {
                        targetWidth = frameSize;
                        float percentage = frameSize * 1F / bitmap.getWidth();
                        targetHeight = (int) (bitmap.getHeight() * percentage);
                    }
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false);
                    bitmap.recycle();
                    FileHelper.saveBitmapToFile(cache, scaledBitmap);
                    scaledBitmap.recycle();
                    handler.post(() -> notifyItemChanged(position));
                } catch (Exception e) {
                    e.printStackTrace();
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
