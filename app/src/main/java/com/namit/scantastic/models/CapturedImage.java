package com.namit.scantastic.models;

import android.graphics.Bitmap;
import android.graphics.PointF;

import java.util.Map;

public class CapturedImage {

    private Bitmap originalBitmap;
    private Bitmap croppedBitmap;
    private Map<Integer, PointF> cropPoints;
    private final String originalUri;
    private int brightness;
    private int contrast;
    private int saturation;

    public CapturedImage(String originalUri) {
        this.originalUri = originalUri;
        this.brightness = 0;
        this.saturation = 0;
        this.contrast = 0;
    }

    public Bitmap getOriginalBitmap() {
        return originalBitmap;
    }

    public void setOriginalBitmap(Bitmap originalBitmap) {
        this.originalBitmap = originalBitmap;
    }

    public Bitmap getCroppedBitmap() {
        return croppedBitmap;
    }

    public void setCroppedBitmap(Bitmap croppedBitmap) {
        this.croppedBitmap = croppedBitmap;
    }

    public Map<Integer, PointF> getCropPoints() {
        return cropPoints;
    }

    public void setCropPoints(Map<Integer, PointF> cropPoints) {
        this.cropPoints = cropPoints;
    }

    public String getOriginalUri() {
        return originalUri;
    }

    public int getBrightness() {
        return brightness;
    }

    public void setBrightness(int brightness) {
        this.brightness = brightness;
    }

    public int getContrast() {
        return contrast;
    }

    public void setContrast(int contrast) {
        this.contrast = contrast;
    }

    public int getSaturation() {
        return saturation;
    }

    public void setSaturation(int saturation) {
        this.saturation = saturation;
    }
}
