package com.video.timeline;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class GlUtils {

    public static String savePixelBuffer(ByteBuffer pixelBuffer, int width, int height) throws IOException {
        File parent = new File(Environment.getExternalStorageDirectory() + "/AScreen");
        if (parent.exists() || parent.mkdirs()) {
            reverseBuf(pixelBuffer, width, height);
            File file = new File(parent, UUID.randomUUID() + ".jpg");
            Log.d("pbo_test", "Saving: " + file.getName());
            BufferedOutputStream bos = null;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(file.getAbsolutePath()));
                Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bmp.copyPixelsFromBuffer(pixelBuffer);
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, bos);
                bmp.recycle();
            } finally {
                if (bos != null) bos.close();
            }

            return file.getAbsolutePath();
        }

        return null;
    }

    private static void reverseBuf(ByteBuffer buf, int width, int height) {
        long ts = System.currentTimeMillis();
        int i = 0;
        byte[] tmp = new byte[width * 4];
        while (i++ < height / 2) {
            buf.get(tmp);
            System.arraycopy(buf.array(), buf.limit() - buf.position(), buf.array(), buf.position() - width * 4, width * 4);
            System.arraycopy(tmp, 0, buf.array(), buf.limit() - buf.position(), width * 4);
        }
        buf.rewind();
        Log.d("pbo_test", "reverseBuf took " + (System.currentTimeMillis() - ts) + "ms");
    }
}
