package com.example.timlineview;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.provider.MediaStore;

import androidx.core.app.ActivityCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import bolts.Task;

public class Android {

    public static Task<List<String>> queryRecentVideos(Context c, int limit) {
        return
                Task.call(() -> {
                    String sortOrder = MediaStore.Video.Media.DATE_MODIFIED + " DESC Limit " + limit;
                    Cursor cursor = c.getContentResolver().query(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            null,
                            null,
                            null,
                            sortOrder
                    );

                    List<String> files = new ArrayList<>();
                    while (cursor != null && cursor.moveToNext()) {

                        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
                        if (new File(path).exists()) {
                            files.add(path);
                        }
                    }

                    return files;
                }, Task.BACKGROUND_EXECUTOR);

    }

    public static int dpToPx(Context context, float dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    public static boolean checkPermission(Activity activity, String permission, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{permission}, requestCode);
                return false;
            }
        }

        return true;
    }
}
