package com.namit.scantastic;

import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.namit.scantastic.models.PolygonView;
import com.namit.scantastic.utils.AppConstants;
import com.namit.scantastic.utils.ColorMatrixUtil;
import com.namit.scantastic.utils.MathUtils;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CropActivity extends AppCompatActivity {

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("Scanner");
    }

    //VIEWS
    @BindView(R.id.polygon_view) PolygonView polygonView;
    @BindView(R.id.img_crop) ImageView imageView;
    @BindView(R.id.seekbar_brightness) SeekBar brightnessSeekbar;
    @BindView(R.id.seekbar_contrast) SeekBar contrastSeekbar;
    @BindView(R.id.seekbar_saturation) SeekBar saturationSeekbar;

    //IMAGE PROCESSING
    private NativeClass nativeClass = new NativeClass();
    private Bitmap currentImage;
    private ColorMatrix contrast;
    private ColorMatrix brightness;
    private ColorMatrix saturation;
    private boolean previouslyCropped;
    private boolean initialized;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);
        ButterKnife.bind(this);
        setClickListeners();
        if(AppConstants.currentCrop == null){
            setResult(RESULT_CANCELED);
            finish();
        }else if (AppConstants.currentCrop.getCropPoints() != null){
            previouslyCropped = true;
            currentImage = AppConstants.currentCrop.getOriginalBitmap();
            setupImage();
        }else if(AppConstants.currentCrop.getOriginalBitmap() != null){
            currentImage = AppConstants.currentCrop.getOriginalBitmap();
            setupImage();
        }else{
            Glide.with(this)
                    .asBitmap()
                    .load(AppConstants.currentCrop.getOriginalUri())
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            AppConstants.currentCrop.setOriginalBitmap(resource);
                            currentImage = AppConstants.currentCrop.getOriginalBitmap();
                            setupImage();
                        }
                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {}
                    });
        }

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && !initialized && currentImage != null) {
            if(previouslyCropped){
                updatePolygonWithPoints(AppConstants.currentCrop.getCropPoints());
            }else{
                Map<Integer, PointF> pointFs = getEdgePoints(currentImage);
                updatePolygonWithPoints(pointFs);
            }
            initialized = true;
        }

    }

    private void setClickListeners() {
        brightnessSeekbar.setOnSeekBarChangeListener(getBrightnessListener());
        contrastSeekbar.setOnSeekBarChangeListener(getContrastListener());
        saturationSeekbar.setOnSeekBarChangeListener(getSaturationListener());
        View.OnClickListener menuClickListener = view -> {
            AppConstants.currentCrop.setCroppedBitmap(getCroppedImage());
            AppConstants.currentCrop.setCropPoints(polygonView.getPoints());
            setResult(RESULT_OK);
            finish();
        };
        findViewById(R.id.nav_done).setOnClickListener(menuClickListener);
    }

    private SeekBar.OnSeekBarChangeListener getBrightnessListener() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                if (currentImage == null) {
                    return;
                }
                brightness = ColorMatrixUtil.brightnessMatrix(progress);
                AppConstants.currentCrop.setBrightness(progress);
                setCombinedFilter();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
    }

    private SeekBar.OnSeekBarChangeListener getContrastListener() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                if (currentImage == null) {
                    return;
                }
                contrast = ColorMatrixUtil.contrastMatrix(progress);
                AppConstants.currentCrop.setContrast(progress);
                setCombinedFilter();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
    }

    private SeekBar.OnSeekBarChangeListener getSaturationListener() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                if(currentImage == null){
                    return;
                }
                saturation = ColorMatrixUtil.saturationMatrix(progress);
                AppConstants.currentCrop.setSaturation(progress);
                setCombinedFilter();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
    }

    private void setCombinedFilter() {
        imageView.setColorFilter(ColorMatrixUtil.combine3MatrixToColorFilter(brightness, contrast, saturation));
    }

    private void updatePolygonWithPoints(Map<Integer, PointF> pointFs) {
        polygonView.setPoints(pointFs);
        polygonView.setVisibility(View.VISIBLE);
        int padding = (int) getResources().getDimension(R.dimen.scanPadding);
        int height = imageView.getMeasuredHeight();
        int imageHeight = currentImage.getHeight();
        int imageWidth = currentImage.getWidth();
        float scaleRatio = (float) height / imageHeight;
        int calculatedWidth = (int) (scaleRatio * imageWidth);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(calculatedWidth + 2 * padding, height + 2 * padding);
        layoutParams.gravity = Gravity.CENTER;

        polygonView.setLayoutParams(layoutParams);
    }

    private void setupImage() {
        contrast = ColorMatrixUtil.contrastMatrix(AppConstants.currentCrop.getContrast());
        brightness = ColorMatrixUtil.brightnessMatrix(AppConstants.currentCrop.getBrightness());
        saturation = ColorMatrixUtil.saturationMatrix(AppConstants.currentCrop.getSaturation());
        contrastSeekbar.setProgress(AppConstants.currentCrop.getContrast());
        brightnessSeekbar.setProgress(AppConstants.currentCrop.getBrightness());
        saturationSeekbar.setProgress(AppConstants.currentCrop.getSaturation());
        imageView.setImageBitmap(currentImage);
    }

    private Map<Integer, PointF> getEdgePoints(Bitmap tempBitmap) {
        List<PointF> pointFs = getContourEdgePoints(tempBitmap);
        if(pointFs == null){
            return getOutlinePoints(tempBitmap);
        }
        return orderedValidEdgePoints(tempBitmap, pointFs);
    }


    private List<PointF> getContourEdgePoints(Bitmap tempBitmap) {
        MatOfPoint2f point2f = nativeClass.getPoint(tempBitmap);
        if(point2f == null){
            return null;
        }
        float scaleRatio = (float) imageView.getMeasuredHeight() / currentImage.getHeight();
        point2f = MathUtils.scaleRectangle(point2f, scaleRatio);
        List<Point> points = Arrays.asList(point2f.toArray());
        List<PointF> result = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            result.add(new PointF(((float) points.get(i).x), ((float) points.get(i).y)));
        }
        return result;
    }

    private Map<Integer, PointF> getOutlinePoints(Bitmap tempBitmap) {
        Map<Integer, PointF> outlinePoints = new HashMap<>();
        outlinePoints.put(0, new PointF(0, 0));
        outlinePoints.put(1, new PointF(tempBitmap.getWidth(), 0));
        outlinePoints.put(2, new PointF(0, tempBitmap.getHeight()));
        outlinePoints.put(3, new PointF(tempBitmap.getWidth(), tempBitmap.getHeight()));
        return outlinePoints;
    }

    private Map<Integer, PointF> orderedValidEdgePoints(Bitmap tempBitmap, List<PointF> pointFs) {
        Map<Integer, PointF> orderedPoints = polygonView.getOrderedPoints(pointFs);
        if (!polygonView.isValidShape(orderedPoints)) {
            orderedPoints = getOutlinePoints(tempBitmap);
        }
        return orderedPoints;
    }

    protected Bitmap getCroppedImage() {
        Map<Integer, PointF> points = polygonView.getPoints();

        float xRatio = (float) currentImage.getWidth() / imageView.getWidth();
        float yRatio = (float) currentImage.getHeight() / imageView.getHeight();
        float x1 = (points.get(0).x) * xRatio;
        float x2 = (points.get(1).x) * xRatio;
        float x3 = (points.get(2).x) * xRatio;
        float x4 = (points.get(3).x) * xRatio;
        float y1 = (points.get(0).y) * yRatio;
        float y2 = (points.get(1).y) * yRatio;
        float y3 = (points.get(2).y) * yRatio;
        float y4 = (points.get(3).y) * yRatio;

        Bitmap croppedBitmap = nativeClass.getScannedBitmap(currentImage, x1, y1, x2, y2, x3, y3, x4, y4);
        ColorMatrixColorFilter filter = ColorMatrixUtil.combine3MatrixToColorFilter(brightness, contrast, saturation);
        return ColorMatrixUtil.getBitmap(croppedBitmap, filter);
    }

}
