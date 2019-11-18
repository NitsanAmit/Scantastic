package com.namit.scantastic.vision;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.document.FirebaseVisionCloudDocumentRecognizerOptions;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentTextRecognizer;
import com.namit.scantastic.models.ScannedImage;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScannedImageReader {

    public static final Pattern TITLE_REGEX = Pattern.compile("#(.+)#"); // title sign is: #title#
    public static final Pattern DATE_REGEX = Pattern.compile("@(\\s*?\\d+\\s*?\\.\\s*?\\d+\\s*?(\\.\\s*?\\d+\\s*?)?)@"); // date sign is: @date@
    public static final Pattern TOPIC_REGEX = Pattern.compile("\\*(.+)\\*"); // topic sign is: *topic*
    public static final Pattern PAGE_BORDER_REGEX = Pattern.compile("A(.*)?R(.*)?Q"); //border sign is: ARQ
    public static final String[] LANGUAGES = {"en", "iw"};

    private FirebaseVisionDocumentTextRecognizer detector;
    private final OnImageScannedListener imageScannedListener;

    public ScannedImageReader(OnImageScannedListener listener) {
        this.imageScannedListener = listener;
        FirebaseVisionCloudDocumentRecognizerOptions options =
                new FirebaseVisionCloudDocumentRecognizerOptions.Builder()
                        .setLanguageHints(Arrays.asList(LANGUAGES))
                        .build();
        this.detector = FirebaseVision.getInstance().getCloudDocumentTextRecognizer(options);
    }

    public void readImage(Bitmap bitmap, int position){
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        detector.processImage(image)
                .addOnSuccessListener(firebaseVisionText -> parseText(bitmap, firebaseVisionText.getText(), position))
                .addOnFailureListener(e -> Log.e("ERROR @ IMAGE READER.", e.getMessage(), e));
    }

    private void parseText(Bitmap bitmap, String text, int position) {
        text = text.replaceAll("\n","");
        ScannedImage scannedImage = new ScannedImage(bitmap, position);
        if(PAGE_BORDER_REGEX.matcher(text).find()){ // user marked page with border sign
            scannedImage.setBorderPage(true);
            Matcher titleMatcher = TITLE_REGEX.matcher(text);
            if(titleMatcher.find()){
                String title = titleMatcher.group(1).trim();
                scannedImage.setTitle(title);
            }
            Matcher dateMatcher = DATE_REGEX.matcher(text);
            if(dateMatcher.find()) {
                String date = dateMatcher.group(1).trim();
                scannedImage.setDate(date);
            }
            Matcher topicMatcher = TOPIC_REGEX.matcher(text);
            if(topicMatcher.find()) {
                String topic = topicMatcher.group(1).trim();
                scannedImage.setTopic(topic);
            }
        }
        imageScannedListener.imageScanned(scannedImage);
    }
}
