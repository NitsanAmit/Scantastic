package com.namit.scantastic.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

public class ColorMatrixUtil {

    private static final float SEEKBAR_MAX_VALUE = 100f;

    public static Bitmap sharpenBitmapForVisionScap(Bitmap bmp) {
        float contrast = 0.75f;
        float brightness = 170f;
        ColorMatrix cm = new ColorMatrix(new float[]
                {
                        contrast, 0, 0, 0, brightness,
                        0, contrast, 0, 0, brightness,
                        0, 0, contrast, 0, brightness,
                        0, 0, 0, 1, 0
                });
        cm.setSaturation(0.3f);
        return getBitmap(bmp, new ColorMatrixColorFilter(cm));
    }

    public static Bitmap getBitmap(Bitmap bmp, ColorMatrixColorFilter cm) {
        Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());
        Canvas canvas = new Canvas(ret);
        Paint paint = new Paint();
        paint.setColorFilter(cm);
        canvas.drawBitmap(bmp, 0, 0, paint);
        return ret;
    }


    public static ColorMatrix contrastMatrix(int value) {
        float contrast = 0.5f + (float) value / SEEKBAR_MAX_VALUE;
        return new ColorMatrix(new float[]
                {
                        contrast, 0, 0, 0, 0,
                        0, contrast, 0, 0, 0,
                        0, 0, contrast, 0, 0,
                        0, 0, 0, 1, 0
                });
    }

    public static ColorMatrix brightnessMatrix(int value) {
        float brightness = 50f + (float) value;
        return new ColorMatrix(new float[]
                {
                        1, 0, 0, 0, brightness,
                        0, 1, 0, 0, brightness,
                        0, 0, 1, 0, brightness,
                        0, 0, 0, 1, 0
                });
    }

    public static ColorMatrix saturationMatrix(int value) {
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(1f - (float) (value) / SEEKBAR_MAX_VALUE);
        return colorMatrix;
    }

    public static ColorMatrixColorFilter combine3MatrixToColorFilter(ColorMatrix brightness, ColorMatrix contrast, ColorMatrix saturation) {
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setConcat(brightness, saturation);
        colorMatrix.setConcat(colorMatrix, contrast);
        return new ColorMatrixColorFilter(colorMatrix);
    }
}
