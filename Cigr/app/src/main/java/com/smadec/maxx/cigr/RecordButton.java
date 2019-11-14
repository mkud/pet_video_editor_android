package com.smadec.maxx.cigr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;

public class RecordButton extends View {
    public static final int STATUS_INIT = 1;
    public static final int STATUS_FOCUSING = 2;
    public static final int STATUS_RECORDING = 3;
    private static final int START_ANGLE_POINT = 0;
    public int SIZE_DIAMETER = 200;
    public int SIZE_STROKE_WIDTH = 15;
    private int mCurrentStatus = STATUS_INIT;
    private Paint paintRecording;
    private Paint paintFocusing;
    private Paint paintInit;
    private Paint paintCenter;
    private RectF rect;

    private float angle;
    private CircleAngleAnimation animation;

    private RecordButtonClickListener mClickListener;
    private long mTime;
    private int mTouchCount = 0;

    public RecordButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        paintRecording = new Paint();
        paintRecording.setAntiAlias(true);
        paintRecording.setStyle(Paint.Style.STROKE);
        paintRecording.setStrokeWidth(SIZE_STROKE_WIDTH);
        //Circle color
        paintRecording.setColor(Color.RED);

        paintFocusing = new Paint();
        paintFocusing.setAntiAlias(true);
        paintFocusing.setStyle(Paint.Style.STROKE);
        paintFocusing.setStrokeWidth(SIZE_STROKE_WIDTH);
        //Circle color
        paintFocusing.setColor(0x7FFF0000);

        paintInit = new Paint();
        paintInit.setAntiAlias(true);
        paintInit.setStyle(Paint.Style.STROKE);
        paintInit.setStrokeWidth(SIZE_STROKE_WIDTH);
        //Circle color
        paintInit.setColor(Color.GRAY);

        paintCenter = new Paint();
        paintCenter.setAntiAlias(true);
        paintCenter.setStyle(Paint.Style.FILL);
        //Circle color
        paintCenter.setColor(Color.LTGRAY);

        rect = new RectF(SIZE_STROKE_WIDTH, SIZE_STROKE_WIDTH, SIZE_DIAMETER - SIZE_STROKE_WIDTH, SIZE_DIAMETER - SIZE_STROKE_WIDTH);

        //Initial Angle (optional, it can be zero)
        angle = START_ANGLE_POINT;
        animation = new CircleAngleAnimation();
        animation.setDuration(5000);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mCurrentStatus = STATUS_INIT;
                angle = START_ANGLE_POINT;
                paintFocusing.setColor(0x7FFF0000);
                paintRecording.setColor(Color.RED);
                invalidate();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    public void setClickListener(RecordButtonClickListener clickListener) {
        mClickListener = clickListener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        SIZE_DIAMETER = View.MeasureSpec.getSize(widthMeasureSpec);
        SIZE_STROKE_WIDTH = SIZE_DIAMETER / 13;
        //noinspection SuspiciousNameCombination
        rect.set(SIZE_STROKE_WIDTH, SIZE_STROKE_WIDTH, SIZE_DIAMETER - SIZE_STROKE_WIDTH, SIZE_DIAMETER - SIZE_STROKE_WIDTH);

        setMeasuredDimension(SIZE_DIAMETER, SIZE_DIAMETER);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        switch (mCurrentStatus) {
            case STATUS_INIT:
                canvas.drawArc(rect, 0, 360, false, paintInit);
                break;
            case STATUS_FOCUSING:
                canvas.drawArc(rect, 0, 360, false, paintFocusing);
                break;
            case STATUS_RECORDING:
                canvas.drawArc(rect, 0, 360, false, paintFocusing);
                canvas.drawArc(rect, START_ANGLE_POINT, angle, false, paintRecording);
                break;
        }
        canvas.drawCircle((float) SIZE_DIAMETER / 2, (float) SIZE_DIAMETER / 2,
                (float) SIZE_DIAMETER / 2 - (2 * SIZE_STROKE_WIDTH),
                paintCenter);
    }

    public void ToFocusing() {
        mCurrentStatus = STATUS_FOCUSING;
        invalidate();
    }

    public void StopRecording() {
        clearAnimation();
        mCurrentStatus = STATUS_INIT;
        angle = START_ANGLE_POINT;
        invalidate();
    }

    public void StartRecording() {
        mCurrentStatus = STATUS_RECORDING;
        startAnimation(animation);
    }

    public void CanFinish() {
        if (paintRecording.getColor() == Color.RED) {
            paintFocusing.setColor(0x7F00FF00);
            paintRecording.setColor(Color.GREEN);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int eventMasked = event.getActionMasked();
        switch (eventMasked) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (++mTouchCount == 1) {
                    mClickListener.onClicked();
                    mTime = System.currentTimeMillis();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                if (--mTouchCount == 0) {
                    if (System.currentTimeMillis() - mTime > 500)
                        mClickListener.onClicked();
                }
                break;
        }
        return true;
    }

    public interface RecordButtonClickListener {
        void onClicked();
    }

    private class CircleAngleAnimation extends Animation {

        CircleAngleAnimation() {
            setInterpolator(new LinearInterpolator());
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation transformation) {
            angle = (360 * interpolatedTime);
            requestLayout();
        }
    }
}
