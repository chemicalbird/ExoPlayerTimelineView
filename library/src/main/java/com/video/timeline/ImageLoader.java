package com.video.timeline;

import android.widget.ImageView;

import androidx.annotation.Nullable;

import java.io.File;

public interface ImageLoader {
    void load(@Nullable File file, ImageView into);
}
