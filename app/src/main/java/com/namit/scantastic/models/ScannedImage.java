package com.namit.scantastic.models;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

public class ScannedImage {

    private final int position;
    private final Bitmap bitmap;
    private String title;
    private String topic;
    private String date;
    private boolean isBorderPage;

    public ScannedImage(Bitmap bitmap, int position) {
        this.bitmap = bitmap;
        this.position = position;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public boolean isBorderPage() {
        return isBorderPage;
    }

    public void setBorderPage(boolean borderPage) {
        isBorderPage = borderPage;
    }

    public int getPosition() {
        return position;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString();
        //todo
    }
}
