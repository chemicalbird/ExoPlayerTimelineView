package com.video.timeline;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import java.util.ArrayList;
import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VHolder> {

    private List<String> data;
    private int sizeW;
    private ImageLoader imageLoader;

    VideoAdapter(int size, ImageLoader imageLoader) {
        this.imageLoader = imageLoader;
        this.data = new ArrayList<>();
        this.sizeW = size;
    }

    @NonNull
    @Override
    public VHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(parent.getContext());
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(sizeW, sizeW));
        return new VHolder(imageView);
    }

    @Override
    public void onBindViewHolder(VHolder holder, final int position) {
        String path = data.get(holder.getAdapterPosition());
        if (imageLoader != null) {
            imageLoader.load(path, holder.imageView);
        } else {
            Loggy.d("Probably forgot to set any ImageLoader");
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    void addPath(String filePath) {
        data.add(filePath);
        notifyItemInserted(data.size() - 1);
    }

    static class VHolder extends ViewHolder {
        ImageView imageView;
        VHolder(@NonNull View itemView) {
            super(itemView);
            imageView = (ImageView) itemView;
        }
    }

}