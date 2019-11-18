package com.namit.scantastic.vision;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.namit.scantastic.models.CapturedImage;
import com.namit.scantastic.models.Document;
import com.namit.scantastic.models.ScannedImage;
import com.namit.scantastic.utils.ColorMatrixUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class VisionManager implements OnImageScannedListener {

    private final Context context;
    private List<ScannedImage> scannedImages;
    private AtomicInteger imagesCounter;
    private OnDocumentsReadyListener listener;

    public VisionManager(Context context, OnDocumentsReadyListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void readImages(List<CapturedImage> images){
        scannedImages = new ArrayList<>();
        imagesCounter = new AtomicInteger(images.size());
        ScannedImageReader reader = new ScannedImageReader(this);
        Executors.newSingleThreadExecutor().execute(() -> {
            for (int i = 0; i < images.size(); i++) {
                CapturedImage capturedImage = images.get(i);
                if(capturedImage.getCroppedBitmap() != null){
                    reader.readImage(capturedImage.getCroppedBitmap(), i);
                }else if(capturedImage.getOriginalBitmap() != null){
                    reader.readImage(capturedImage.getOriginalBitmap(), i);
                }else{
                    Uri uri = FileProvider.getUriForFile(context, "com.namit.scantastic.fileprovider", new File(capturedImage.getOriginalUri()));
                    int finalI = i;
                    Glide.with(context).asBitmap().load(uri).into(new CustomTarget<Bitmap>(){
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            Bitmap adjustedBitmap = ColorMatrixUtil.sharpenBitmapForVisionScap(resource);
                            reader.readImage(adjustedBitmap, finalI);
                        }
                        @Override public void onLoadCleared(@Nullable Drawable placeholder) {}
                    });
                }
            }
        });
    }

    @Override
    public void imageScanned(ScannedImage image) {
        scannedImages.add(image);
        if(imagesCounter.decrementAndGet() == 0){
            Collections.sort(scannedImages, (o1, o2)-> Integer.compare(o1.getPosition(), o2.getPosition()));
            createDocuments();
        }
    }

    private void createDocuments() {
        List<Document> documents = new ArrayList<>();
        int i=0;
        while (i<scannedImages.size()) {
            ScannedImage scannedImage = scannedImages.get(i);
            if (scannedImage.isBorderPage()) {
                int pageCounter = 1;
                while ((i + pageCounter) < scannedImages.size() && !scannedImages.get(i + pageCounter).isBorderPage()) {
                    pageCounter++;
                }
                if((i + pageCounter) < scannedImages.size() && scannedImages.get(i + pageCounter).getTitle() == null){
                    pageCounter++;
                }
                Document document = new Document(scannedImage.getTitle(), scannedImage.getTopic(), scannedImage.getDate());
                document.setPages(scannedImages.subList(i, i + pageCounter));
                documents.add(document);
                i= i+pageCounter;
            }else{
                i++;
            }
        }
        listener.documentsReady(documents);
    }
}
