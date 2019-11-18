package com.namit.scantastic;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.FlashMode;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.namit.scantastic.models.CapturedImage;
import com.namit.scantastic.utils.AppConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.namit.scantastic.utils.AppConstants.IMG_STORAGE_DIR;
import static com.namit.scantastic.vision.ScannedImageReader.DATE_REGEX;
import static com.namit.scantastic.vision.ScannedImageReader.PAGE_BORDER_REGEX;
import static com.namit.scantastic.vision.ScannedImageReader.TITLE_REGEX;
import static com.namit.scantastic.vision.ScannedImageReader.TOPIC_REGEX;

public class CaptureActivity extends AppCompatActivity implements View.OnClickListener {

    //VIEWS
    @BindView(R.id.fab_capture) FloatingActionButton captureFab;
    @BindView(R.id.view_finder) TextureView viewFinder;
    @BindView(R.id.txt_title) TextView titleText;
    @BindView(R.id.txt_topic) TextView topicText;
    @BindView(R.id.txt_date) TextView dateText;


    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private FirebaseVisionTextRecognizer textRecognizer;
    private String[] REQUIRED_PERMISSIONS ={
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};
    private ImageCapture imageCapture;
    private Stack<CapturedImage> capturedImages = new Stack<>();
    private PreviewImageAdapter previewImageAdapter;
    private AtomicBoolean isBusy = new AtomicBoolean(false);
    private Preview preview;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);
        ButterKnife.bind(this);

        setupRecyclerView();
        createBottomBar();
        setupViewFinder();

        textRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
    }

    private void createBottomBar() {
        View.OnClickListener bottomBarListener = view -> {
            switch (view.getId()){
                case R.id.nav_done:
                    AppConstants.capturedImages = new ArrayList<>();
                    AppConstants.capturedImages.addAll(capturedImages);
                    Intent intent = new Intent(this, DocumentsActivity.class);
                    startActivity(intent);
                    finish();
                    break;
                case R.id.nav_delete:
                    if(capturedImages != null && !capturedImages.isEmpty()) {
                        capturedImages.pop();
                        previewImageAdapter.notifyDataSetChanged();
                    }
                    break;
                case R.id.nav_flash:
                    FlashMode newMode = imageCapture.getFlashMode().equals(FlashMode.ON) ? FlashMode.OFF : FlashMode.ON;
                    preview.enableTorch(newMode == FlashMode.ON);
                    imageCapture.setFlashMode(newMode);
                    break;
            }
        };
        findViewById(R.id.nav_done).setOnClickListener(bottomBarListener);
        findViewById(R.id.nav_delete).setOnClickListener(bottomBarListener);
        findViewById(R.id.nav_flash).setOnClickListener(bottomBarListener);
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(layoutManager);
        ImageAdapterListener imageAdapterListener = (image, position) -> {
            AppConstants.positionInAdapter = position;
            AppConstants.currentCrop = image;
            Intent intent = new Intent(CaptureActivity.this, CropActivity.class);
            startActivityForResult(intent, 777);
        };
        previewImageAdapter = new PreviewImageAdapter(this, capturedImages, imageAdapterListener);
        recyclerView.setAdapter(previewImageAdapter);
    }

    private void setupViewFinder() {
        ImageCaptureConfig captureConfig = new ImageCaptureConfig.Builder()
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                .setTargetResolution(new Size(600, 800))
                .setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
                .setLensFacing(CameraX.LensFacing.BACK)
                .setFlashMode(FlashMode.OFF)
                .build();
        imageCapture = new ImageCapture(captureConfig);

        if (allPermissionsGranted()) {
            viewFinder.post(startCamera());
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        viewFinder.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                updateTransform());
        captureFab.setOnClickListener(this);
    }

    private boolean allPermissionsGranted() {
        boolean cameraGranted = ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED;
        boolean writeGranted = ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED;
        boolean readGranted = ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED;
        return cameraGranted && writeGranted && readGranted;
    }

    private Runnable startCamera() {
        return () -> {
            preview = new Preview(new PreviewConfig.Builder().build());
            preview.setOnPreviewOutputUpdateListener(output -> {
                ViewGroup parent = (ViewGroup) viewFinder.getParent();
                parent.removeView(viewFinder);
                parent.addView(viewFinder, 0);
                viewFinder.setSurfaceTexture(output.getSurfaceTexture());
                updateTransform();
            });

            final ImageAnalysis analysis = new ImageAnalysis(new ImageAnalysisConfig.Builder()
                    .setTargetResolution(new Size(600, 800))
                    .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                    .build());

            analysis.setAnalyzer(Runnable::run, getAnalyzer());
            CameraX.bindToLifecycle(CaptureActivity.this, analysis, imageCapture, preview);
        };
    }

    private ImageAnalysis.Analyzer getAnalyzer() {
        return (image, rotationDegrees) -> {
            if (isBusy.get() || image == null || image.getImage() == null) {
                return;
            }
            isBusy.set(true);
            int rotation = degreesToFirebaseRotation(rotationDegrees);
            FirebaseVisionImage fbImage = FirebaseVisionImage.fromMediaImage(image.getImage(), rotation);
            textRecognizer.processImage(fbImage)
                    .addOnSuccessListener(this::processAnalyzerResult)
                    .addOnFailureListener(e -> {
                        Log.e(getLocalClassName(), e.getMessage());
                        isBusy.set(false);
                    });
        };
    }

    private int degreesToFirebaseRotation(int degrees) {
        switch (degrees) {
            case 0:
                return FirebaseVisionImageMetadata.ROTATION_0;
            case 90:
                return FirebaseVisionImageMetadata.ROTATION_90;
            case 180:
                return FirebaseVisionImageMetadata.ROTATION_180;
            case 270:
                return FirebaseVisionImageMetadata.ROTATION_270;
            default:
                throw new IllegalArgumentException(
                        "Rotation must be 0, 90, 180, or 270.");
        }
    }

    /**
     * Process text read from the camera stream, to assist the user in positioning the camera.
     */
    private void processAnalyzerResult(FirebaseVisionText visionText) {
        String foundText = visionText.getText();
        if(PAGE_BORDER_REGEX.matcher(foundText).find()){ //Page was marked by the user with a border sign
            componentLookup(foundText, TITLE_REGEX, titleText, R.string.txt_title);
            componentLookup(foundText, DATE_REGEX, dateText, R.string.txt_date);
            componentLookup(foundText, TOPIC_REGEX, topicText, R.string.txt_topic);
        }else{
            titleText.setVisibility(View.GONE);
            dateText.setVisibility(View.GONE);
            topicText.setVisibility(View.GONE);
        }
        isBusy.set(false);
    }

    private void componentLookup(String txt, Pattern pattern, TextView textView, int stringResource) {
        Matcher matcher = pattern.matcher(txt);
        if (matcher.find()) {
            String recognizedText = matcher.group(1).trim();
            textView.setVisibility(View.VISIBLE);
            textView.setText(String.format(getString(stringResource), recognizedText));
        } else {
            textView.setVisibility(View.GONE);
        }
    }

    private void updateTransform() {
        Matrix matrix = new Matrix();

        // Compute the center of the view finder
        float centerX = viewFinder.getWidth() / 2f;
        float centerY = viewFinder.getHeight() / 2f;

        // Correct preview output to account for display rotation
        float rotationDegrees = 0;
        switch (viewFinder.getDisplay().getRotation()) {
            case Surface.ROTATION_0: rotationDegrees = 0f; break;
            case Surface.ROTATION_90: rotationDegrees = 90f; break;
            case Surface.ROTATION_180: rotationDegrees = 180f; break;
            case Surface.ROTATION_270: rotationDegrees = 270f; break;
        }
        matrix.postRotate(-rotationDegrees, centerX, centerY);
        viewFinder.setTransform(matrix);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post(startCamera());
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.fab_capture){
            captureImage();
        }
    }

    private void captureImage() {
        captureFab.setClickable(false);
        String filename =  new Date().getTime() + "_test.png";
        File storageDirectory = new File(Environment.getExternalStorageDirectory(), IMG_STORAGE_DIR);
        if(!storageDirectory.exists()) storageDirectory.mkdir();
        File dest = new File(storageDirectory, filename);
        imageCapture.takePicture(dest, Runnable::run, new ImageCapture.OnImageSavedListener(){
            @Override
            public void onImageSaved(@NonNull File file) {
                capturedImages.add(new CapturedImage(file.getAbsolutePath()));
                runOnUiThread(() -> {
                    previewImageAdapter.notifyDataSetChanged();
                    captureFab.setClickable(true);
                });
            }

            @Override
            public void onError(@NonNull ImageCapture.ImageCaptureError imageCaptureError, @NonNull String message, @Nullable Throwable cause) {
                Log.e("Error capturing image.", message, cause);
                captureFab.setClickable(true);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode == RESULT_OK && requestCode == 777){
            capturedImages.set(AppConstants.positionInAdapter, AppConstants.currentCrop);
            previewImageAdapter.notifyDataSetChanged();
            AppConstants.currentCrop = null;
            AppConstants.positionInAdapter = -1;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
