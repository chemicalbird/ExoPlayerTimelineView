package com.example.timlineview;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.video.timeline.ImageLoader;
import com.video.timeline.RetroInstance;

public class VideoFrameAdapter extends RecyclerView.Adapter<VideoFrameAdapter.Holder> {
    private final ImageLoader imageLoader;
    private final long frameDuration;
    private Context context;
    int count;

    private RetroInstance retroInstance;

    public VideoFrameAdapter(RetroInstance retroInstance, int frameDuration, long videoDuration, ImageLoader imageLoader) {
        this.frameDuration = frameDuration;
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

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ImageView imageView = (ImageView)holder.itemView;
        imageView.setImageBitmap(null);
        retroInstance.load(position * frameDuration, holder.hashCode(),
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
