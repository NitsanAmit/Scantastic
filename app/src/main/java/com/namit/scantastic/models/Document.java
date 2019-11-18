package com.namit.scantastic.models;

import java.util.List;

public class Document {

    private String title;
    private String topic;
    private String date;
    private List<ScannedImage> pages;

    public Document(String title, String topic, String date) {
        this.title = title;
        this.topic = topic;
        this.date = date;
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

    public List<ScannedImage> getPages() {
        return pages;
    }

    public void setPages(List<ScannedImage> pages) {
        this.pages = pages;
    }

}
