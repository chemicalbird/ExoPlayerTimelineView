package com.video.timeline.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.video.timeline.FetchCallback;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class MRetriever {
    private final MediaMetadataRetriever mediaMetadataRetriever;
    private Context context;
    private final int size;
    private final ExecutorService threadPoolExecutor;

    private HashMap<Integer, Future> tasks = new HashMap<>();

    private String currentPreparedSource;

    public MRetriever(Context context, int desiredSize, @NonNull ExecutorService executor) {
        this.context = context;
        this.size = desiredSize;
        mediaMetadataRetriever = new MediaMetadataRetriever();

        threadPoolExecutor = executor;
    }

    public void frameAt(String source, long timeMs, FetchCallback<Bitmap> fetchCallback, int hashcode) {

        Future task = tasks.get(hashcode);
        if (task != null) {
            task.cancel(false);
        }

        Future future = threadPoolExecutor.submit(() -> {
            if (!source.equals(currentPreparedSource)) {
                mediaMetadataRetriever.setDataSource(context, Uri.parse(source));
            }
            fetchCallback.onSuccess(getScaledFrameAt(timeMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC));
            currentPreparedSource = source;
        });

        tasks.put(hashcode, future);
    }

    public void setSource(String media) {
        mediaMetadataRetriever.setDataSource(context, Uri.parse(media));
    }

    Bitmap getScaledFrameAt(long time, int option) {
        Bitmap bitmap = mediaMetadataRetriever.getFrameAtTime(time, option);

        try {
            int targetWidth;
            int targetHeight;
            if (bitmap.getHeight() > bitmap.getWidth()) {
                targetHeight = size;
                float percentage = size * 1F / bitmap.getHeight();
                targetWidth = (int) (bitmap.getWidth() * percentage);
            } else {
                targetWidth = size;
                float percentage = size * 1F / bitmap.getWidth();
                targetHeight = (int) (bitmap.getHeight() * percentage);
            }
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false);
            bitmap.recycle();
            return scaledBitmap;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bitmap;
    }

}
