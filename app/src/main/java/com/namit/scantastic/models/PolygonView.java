package com.namit.scantastic.models;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.namit.scantastic.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PolygonView extends FrameLayout {

    private final Context context;
    private PolygonView polygonView;
    private ImageView topLeftPoint;
    private ImageView topMiddlePoint;
    private ImageView topRightPoint;
    private ImageView middleLeftPoint;
    private ImageView middleRightPoint;
    private ImageView bottomLeftPoint;
    private ImageView bottomMiddlePoint;
    private ImageView bottomRightPoint;

    private Paint paint;

    public PolygonView(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public PolygonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public PolygonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init();
    }

    private void init() {
        polygonView = this;
        topLeftPoint = getImageView(0, 0);
        topRightPoint = getImageView(getWidth(), 0);
        bottomLeftPoint = getImageView(0, getHeight());
        bottomRightPoint = getImageView(getWidth(), getHeight());
        middleLeftPoint = getImageView(0, getHeight() / 2);
        middleLeftPoint.setOnTouchListener(new MidPointPolygonTouchListener(topLeftPoint, bottomLeftPoint));

        topMiddlePoint = getImageView(0, getWidth() / 2);
        topMiddlePoint.setOnTouchListener(new MidPointPolygonTouchListener(topLeftPoint, topRightPoint));

        bottomMiddlePoint = getImageView(0, getHeight() / 2);
        bottomMiddlePoint.setOnTouchListener(new MidPointPolygonTouchListener(bottomLeftPoint, bottomRightPoint));

        middleRightPoint = getImageView(0, getHeight() / 2);
        middleRightPoint.setOnTouchListener(new MidPointPolygonTouchListener(topRightPoint, bottomRightPoint));

        addView(topLeftPoint);
        addView(topRightPoint);
        addView(middleLeftPoint);
        addView(topMiddlePoint);
        addView(bottomMiddlePoint);
        addView(middleRightPoint);
        addView(bottomLeftPoint);
        addView(bottomRightPoint);
        initPaint();
    }

    private void initPaint() {
        paint = new Paint();
        paint.setColor(ContextCompat.getColor(context, R.color.colorAccent));
        paint.setStrokeWidth(2);
        paint.setAntiAlias(true);
    }

    private ImageView getImageView(int x, int y) {
        ImageView imageView = new ImageView(context);
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        imageView.setLayoutParams(layoutParams);
        imageView.setImageResource(R.drawable.circle_point);
        imageView.setX(x);
        imageView.setY(y);
        imageView.setOnTouchListener(new PolygonTouchListener());
        return imageView;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        canvas.drawLine(topLeftPoint.getX() + (topLeftPoint.getWidth() / 2), topLeftPoint.getY() + (topLeftPoint.getHeight() / 2), bottomLeftPoint.getX() + (bottomLeftPoint.getWidth() / 2), bottomLeftPoint.getY() + (bottomLeftPoint.getHeight() / 2), paint);
        canvas.drawLine(topLeftPoint.getX() + (topLeftPoint.getWidth() / 2), topLeftPoint.getY() + (topLeftPoint.getHeight() / 2), topRightPoint.getX() + (topRightPoint.getWidth() / 2), topRightPoint.getY() + (topRightPoint.getHeight() / 2), paint);
        canvas.drawLine(topRightPoint.getX() + (topRightPoint.getWidth() / 2), topRightPoint.getY() + (topRightPoint.getHeight() / 2), bottomRightPoint.getX() + (bottomRightPoint.getWidth() / 2), bottomRightPoint.getY() + (bottomRightPoint.getHeight() / 2), paint);
        canvas.drawLine(bottomLeftPoint.getX() + (bottomLeftPoint.getWidth() / 2), bottomLeftPoint.getY() + (bottomLeftPoint.getHeight() / 2), bottomRightPoint.getX() + (bottomRightPoint.getWidth() / 2), bottomRightPoint.getY() + (bottomRightPoint.getHeight() / 2), paint);
        middleLeftPoint.setX(bottomLeftPoint.getX() - ((bottomLeftPoint.getX() - topLeftPoint.getX()) / 2));
        middleLeftPoint.setY(bottomLeftPoint.getY() - ((bottomLeftPoint.getY() - topLeftPoint.getY()) / 2));
        middleRightPoint.setX(bottomRightPoint.getX() - ((bottomRightPoint.getX() - topRightPoint.getX()) / 2));
        middleRightPoint.setY(bottomRightPoint.getY() - ((bottomRightPoint.getY() - topRightPoint.getY()) / 2));
        bottomMiddlePoint.setX(bottomRightPoint.getX() - ((bottomRightPoint.getX() - bottomLeftPoint.getX()) / 2));
        bottomMiddlePoint.setY(bottomRightPoint.getY() - ((bottomRightPoint.getY() - bottomLeftPoint.getY()) / 2));
        topMiddlePoint.setX(topRightPoint.getX() - ((topRightPoint.getX() - topLeftPoint.getX()) / 2));
        topMiddlePoint.setY(topRightPoint.getY() - ((topRightPoint.getY() - topLeftPoint.getY()) / 2));
    }

    @Override
    protected void attachViewToParent(View child, int index, ViewGroup.LayoutParams params) {
        super.attachViewToParent(child, index, params);
    }


    public Map<Integer, PointF> getPoints() {
        List<PointF> points = new ArrayList<PointF>();
        points.add(new PointF(topLeftPoint.getX(), topLeftPoint.getY()));
        points.add(new PointF(topRightPoint.getX(), topRightPoint.getY()));
        points.add(new PointF(bottomLeftPoint.getX(), bottomLeftPoint.getY()));
        points.add(new PointF(bottomRightPoint.getX(), bottomRightPoint.getY()));
        return getOrderedPoints(points);
    }

    public Map<Integer, PointF> getOrderedPoints(List<PointF> points) {
        PointF centerPoint = new PointF();
        int size = points.size();
        for (PointF pointF : points) {
            centerPoint.x += pointF.x / size;
            centerPoint.y += pointF.y / size;
        }
        Map<Integer, PointF> orderedPoints = new HashMap<>();
        for (PointF pointF : points) {
            int index = -1;
            if (pointF.x < centerPoint.x && pointF.y < centerPoint.y) {
                index = 0;
            } else if (pointF.x > centerPoint.x && pointF.y < centerPoint.y) {
                index = 1;
            } else if (pointF.x < centerPoint.x && pointF.y > centerPoint.y) {
                index = 2;
            } else if (pointF.x > centerPoint.x && pointF.y > centerPoint.y) {
                index = 3;
            }
            orderedPoints.put(index, pointF);
        }
        return orderedPoints;
    }


    public void setPoints(Map<Integer, PointF> pointFMap) {
        if (pointFMap.size() == 4) {
            setPointsCoordinates(pointFMap);
        }
    }

    private void setPointsCoordinates(Map<Integer, PointF> pointFMap) {
        topLeftPoint.setX(pointFMap.get(0).x);
        topLeftPoint.setY(pointFMap.get(0).y);

        topRightPoint.setX(pointFMap.get(1).x);
        topRightPoint.setY(pointFMap.get(1).y);

        bottomLeftPoint.setX(pointFMap.get(2).x);
        bottomLeftPoint.setY(pointFMap.get(2).y);

        bottomRightPoint.setX(pointFMap.get(3).x);
        bottomRightPoint.setY(pointFMap.get(3).y);
    }


    private class PolygonTouchListener implements OnTouchListener {

        PointF DownPT = new PointF(); // Record Mouse Position When Pressed Down
        PointF StartPT = new PointF(); // Record Start Position of 'img'

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int eid = event.getAction();
            switch (eid) {
                case MotionEvent.ACTION_MOVE:
                    PointF mv = new PointF(event.getX() - DownPT.x, event.getY() - DownPT.y);
                    if (((StartPT.x + mv.x + v.getWidth()) < polygonView.getWidth() && (StartPT.y + mv.y + v.getHeight() < polygonView.getHeight())) && ((StartPT.x + mv.x) > 0 && StartPT.y + mv.y > 0)) {
                        v.setX((int) (StartPT.x + mv.x));
                        v.setY((int) (StartPT.y + mv.y));
                        StartPT = new PointF(v.getX(), v.getY());
                    }
                    break;
                case MotionEvent.ACTION_DOWN:
                    DownPT.x = event.getX();
                    DownPT.y = event.getY();
                    StartPT = new PointF(v.getX(), v.getY());
                    break;
                case MotionEvent.ACTION_UP:
                    int color = 0;
                    if (isValidShape(getPoints())) {
                        color = getResources().getColor(R.color.colorPrimary);
                    } else {
                        color = getResources().getColor(R.color.colorAccent);
                    }
                    paint.setColor(color);
                    break;
                default:
                    break;
            }
            polygonView.invalidate();
            return true;
        }
    }

    private class MidPointPolygonTouchListener implements OnTouchListener {

        PointF DownPT = new PointF(); // Record Mouse Position When Pressed Down
        PointF StartPT = new PointF(); // Record Start Position of 'img'

        private ImageView edge1;
        private ImageView edge2;

        public MidPointPolygonTouchListener(ImageView edge1, ImageView edge2) {
            this.edge1 = edge1;
            this.edge2 = edge2;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int eid = event.getAction();
            switch (eid) {
                case MotionEvent.ACTION_MOVE:
                    PointF mv = new PointF(event.getX() - DownPT.x, event.getY() - DownPT.y);

                    if (Math.abs(edge1.getX() - edge2.getX()) > Math.abs(edge1.getY() - edge2.getY())) {
                        if (((edge2.getY() + mv.y + v.getHeight() < polygonView.getHeight()) && (edge2.getY() + mv.y > 0))) {
                            v.setX((int) (StartPT.y + mv.y));
                            StartPT = new PointF(v.getX(), v.getY());
                            edge2.setY((int) (edge2.getY() + mv.y));
                        }
                        if (((edge1.getY() + mv.y + v.getHeight() < polygonView.getHeight()) && (edge1.getY() + mv.y > 0))) {
                            v.setX((int) (StartPT.y + mv.y));
                            StartPT = new PointF(v.getX(), v.getY());
                            edge1.setY((int) (edge1.getY() + mv.y));
                        }
                    } else {
                        if ((edge2.getX() + mv.x + v.getWidth() < polygonView.getWidth()) && (edge2.getX() + mv.x > 0)) {
                            v.setX((int) (StartPT.x + mv.x));
                            StartPT = new PointF(v.getX(), v.getY());
                            edge2.setX((int) (edge2.getX() + mv.x));
                        }
                        if ((edge1.getX() + mv.x + v.getWidth() < polygonView.getWidth()) && (edge1.getX() + mv.x > 0)) {
                            v.setX((int) (StartPT.x + mv.x));
                            StartPT = new PointF(v.getX(), v.getY());
                            edge1.setX((int) (edge1.getX() + mv.x));
                        }
                    }

                    break;
                case MotionEvent.ACTION_DOWN:
                    DownPT.x = event.getX();
                    DownPT.y = event.getY();
                    StartPT = new PointF(v.getX(), v.getY());
                    break;
                case MotionEvent.ACTION_UP:
                    int color = 0;
                    if (isValidShape(getPoints())) {
                        color = getResources().getColor(R.color.colorPrimary);
                    } else {
                        color = getResources().getColor(R.color.colorAccent);
                    }
                    paint.setColor(color);
                    break;
                default:
                    break;
            }
            polygonView.invalidate();
            return true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    public boolean isValidShape(Map<Integer, PointF> pointFMap) {
        return pointFMap.size() == 4;
    }

}
