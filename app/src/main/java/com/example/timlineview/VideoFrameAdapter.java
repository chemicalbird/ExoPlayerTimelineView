package com.example.timlineview;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.video.timeline.ImageLoader;
import com.video.timeline.RetroInstance;
import com.video.timeline.VideoMetadata;

import java.util.List;

public class VideoFrameAdapter extends RecyclerView.Adapter<VideoFrameAdapter.Holder> {
    private List<VideoMetadata> mets;
    private final ImageLoader imageLoader;
    private final long frameDuration;
    private Context context;
    int count;

    private RetroInstance retroInstance;

    public VideoFrameAdapter(RetroInstance retroInstance, int frameDuration, long videoDuration, List<VideoMetadata> mets, ImageLoader imageLoader) {
        this.frameDuration = frameDuration;
        this.mets = mets;
        this.imageLoader = imageLoader;
        count = (int) (videoDuration / frameDuration);

        this.retroInstance = retroInstance;
    }
    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(parent.getContext());
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(retroInstance.frameSize(), retroInstance.frameSize()));
        return new Holder(imageView);
    }

    private long getWindowPosition(long outPosition) {
        long len = 0;
        for (VideoMetadata metadata: mets) {
            if (outPosition <= metadata.getDurationMs() + len) {
                return outPosition - len;
            }
            len += metadata.getDurationMs();
        }

        return len;
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ImageView imageView = (ImageView)holder.itemView;
        imageView.setImageBitmap(null);
        retroInstance.load(hashCode() + "",getWindowPosition(position * frameDuration), holder.hashCode(),
                file -> imageLoader.load(file, imageView));
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
