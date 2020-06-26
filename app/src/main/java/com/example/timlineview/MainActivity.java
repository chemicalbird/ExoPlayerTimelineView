package com.example.timlineview;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;

import bolts.Task;

public class MainActivity extends AppCompatActivity implements ItemSelectListener {

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
    }

    @Override
    public void onSelect(String path) {
        Intent intent = new Intent(this, NewActivity.class);
        intent.putExtra("file_uri", path);
        startActivity(intent);
    }
}
