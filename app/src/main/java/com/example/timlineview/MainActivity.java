package com.example.timlineview;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import java.util.ArrayList;

import bolts.Task;

public class MainActivity extends AppCompatActivity implements ItemSelectListener {

    private boolean groupMode;
    private ArrayList<String> videos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView rv = findViewById(R.id.video_list);
        rv.setLayoutManager(new GridLayoutManager(this, 3));

        if (Android.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, 0)) {
            Android.queryRecentVideos(this, 100).continueWith((cont) -> {
                if (cont.getResult() != null) {
                    VideoAdapter adapter = new VideoAdapter(MainActivity.this,
                            cont.getResult(), MainActivity.this);
                    rv.setAdapter(adapter);
                }
                return null;
            }, Task.UI_THREAD_EXECUTOR);
        }

        CheckBox mutlipleVCheckbox = findViewById(R.id.multiple_vids);
        mutlipleVCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                groupMode = isChecked;
            }
        });

        findViewById(R.id.multiple_sel_btn).setOnClickListener(v -> open());
    }

    @Override
    public void onSelect(String path) {
        videos.add(path);
        if (!groupMode) {
            open();
        } else {
            findViewById(R.id.multiple_sel_btn).setVisibility(videos.size() > 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void open() {
        Intent intent = new Intent(this, NewActivity.class);
        intent.putExtra("file_uri", videos);
        startActivity(intent);
        videos.clear();
    }
}
