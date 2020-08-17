package com.example.timlineview;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.video.timeline.ImageLoader;
import com.video.timeline.RetroInstance;
import com.video.timeline.VideoMetadata;

import java.util.ArrayList;
import java.util.List;

public class VideoFrameAdapter2 extends RecyclerView.Adapter<VideoFrameAdapter2.Holder> {
    private final ImageLoader imageLoader;
    private List<String> medias;
    private List<VideoMetadata> infos;
    private final long frameDuration;
    private Context context;
    int count;

    private RetroInstance retroInstance;

    public VideoFrameAdapter2(RetroInstance retroInstance,
                             int frameDuration,
                             ImageLoader imageLoader,
                             List<String> medias,
                             List<VideoMetadata> infos) {
        this.frameDuration = frameDuration;
        this.imageLoader = imageLoader;
        this.medias = medias;
        this.infos = infos;

        long duration = 0;
        for (VideoMetadata info: infos) {
            Log.d("2_study", info.getDurationMs() + "");
            duration += info.getDurationMs();
        }

        count = (int) (duration / frameDuration);

        this.retroInstance = retroInstance;
    }

    private String getMediaForPosition(int position) {
        int len = 0;
        for (VideoMetadata info: infos) {
            len += (int) (info.getDurationMs() / frameDuration);
            if (position <= len) {
                return medias.get(infos.indexOf(info));
            }
        }

        return medias.get(medias.size() - 1);
    }

    private int offset(String media) {
        int total = 0;
        for (String item: medias) {
            if (item == media) {
                return total;
            }
            total += infos.get(medias.indexOf(item)).getDurationMs() / frameDuration;
        }

        return total;
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
        String video = getMediaForPosition(position);
        retroInstance.load(video,
                (position - offset(video)) * frameDuration,
                holder.hashCode(),
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
