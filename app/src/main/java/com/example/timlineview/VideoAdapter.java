package com.example.timlineview;

import android.content.Context;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import java.util.List;

import bolts.Task;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VHolder> {

    private List<String> data;
    private ItemSelectListener itemSelectListener;
    private int sizeW;
    private int sizeH;

    public VideoAdapter(Context context ,List<String> d, ItemSelectListener itemSelectListener) {
        this.data = d;
        this.itemSelectListener = itemSelectListener;
        this.sizeW = Android.dpToPx(context, 120);
    }

    @NonNull
    @Override
    public VHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(parent.getContext());
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(sizeW, sizeW));
        VHolder holder = new VHolder(imageView);
        holder.itemView.setOnClickListener(view -> {
            if (itemSelectListener != null) {
                itemSelectListener.onSelect(data.get(holder.getAdapterPosition()));
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(VHolder holder, final int position) {
        String path = data.get(holder.getAdapterPosition());

        holder.imageView.setImageBitmap(null);
        Task.call(() -> ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.MINI_KIND),
                Task.BACKGROUND_EXECUTOR).continueWith(task -> {
            if (task.getResult() != null && holder.getAdapterPosition() == position) {
                holder.imageView.setImageBitmap(task.getResult());
            }
            return null;
        }, Task.UI_THREAD_EXECUTOR);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VHolder extends ViewHolder {
        ImageView imageView;
        VHolder(@NonNull View itemView) {
            super(itemView);
            imageView = (ImageView) itemView;
        }
    }

}