package com.smadec.maxx.cigr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar;
import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBarWrapper;
import com.polyak.iconswitch.IconSwitch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class VideoEditActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, SeekBar.OnSeekBarChangeListener {

    private static final float TOUCH_TOLERANCE = 4;
    private static final String SETTINGS_TUTORIAL_PASSED = "tutorial";
    Matrix matrixVideoReturnToNormalScale;
    MyView contentView;
    private int mBrushSize = 110;
    private boolean doubleBackToExitPressedOnce = false;
    private TextureView preview;
    private Bitmap mBitmapBackground;
    private Bitmap mBitmapTransparent;
    private FrameLayout mVideoFrameLayout;
    private float mfMaxScaleFactor, mfMinScaleFactor;
    private IconSwitch mFreezeSwitch;
    private VerticalSeekBarWrapper seekBarWrapper;
    private VerticalSeekBar seekBar;

    private FrameLayout frameLayoutTutorial;
    private ImageView imageViewTutorial;
    private boolean bTutorialShowed = false;

    private FirebaseAnalytics mFirebaseAnalytics;
    private AdView mAdView;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_edit);
        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        mAdView = findViewById(R.id.adView3);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        seekBarWrapper = findViewById(R.id.mySeekBarWrapper);
        seekBar = findViewById(R.id.mySeekBar);
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setProgress(50);

        mFreezeSwitch = findViewById(R.id.icon_switch_freeze);
        mFreezeSwitch.setChecked(IconSwitch.Checked.RIGHT);
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("bitmap_transparent")) {
                BitmapFactory.Options opt = new BitmapFactory.Options();
                opt.inMutable = true;
                mBitmapTransparent = BitmapFactory.decodeFile(getExternalFilesDir(null).getAbsolutePath() + "/cigr_first_frame_mask_.png", opt);
            }
            mFreezeSwitch.setChecked(savedInstanceState.getInt("freeze") == 0 ? IconSwitch.Checked.LEFT : IconSwitch.Checked.RIGHT);
        }
        mFreezeSwitch.setCheckedChangeListener(new IconSwitch.CheckedChangeListener() {
            @Override
            public void onCheckChanged(IconSwitch.Checked current) {
                seekBarWrapper.setVisibility(View.INVISIBLE);
                contentView.SetFreeze(current == IconSwitch.Checked.LEFT);
            }
        });
        mVideoFrameLayout = findViewById(R.id.LayoutVideo);

        preview = findViewById(R.id.TextureView01);
        preview.setSurfaceTextureListener(this);

        if (!getPreferences(Context.MODE_PRIVATE).getBoolean(SETTINGS_TUTORIAL_PASSED, false)) {
            frameLayoutTutorial = findViewById(R.id.LayoutTutorialVideoEdit);
            frameLayoutTutorial.setVisibility(View.VISIBLE);
            imageViewTutorial = findViewById(R.id.imageFingerTutorialVideoEdit);
            Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(this, R.anim.moving_finger_tutorial);
            imageViewTutorial.startAnimation(hyperspaceJumpAnimation);
            mFreezeSwitch.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
            bTutorialShowed = true;
        }

    }

    @Override
    public void onSaveInstanceState(Bundle toSave) {
        super.onSaveInstanceState(toSave);
        SaveBitmapTransparentToFile("/cigr_first_frame_mask_.png");
        toSave.putBoolean("bitmap_transparent", true);
        toSave.putInt("freeze", mFreezeSwitch.getChecked() == IconSwitch.Checked.LEFT ? 0 : 1);
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.press_back_again, Toast.LENGTH_SHORT)
                .show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 3000);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (contentView == null) {
            mBitmapBackground = BitmapFactory.decodeFile(getExternalFilesDir(null).getAbsolutePath() + "/cigr_first_frame.png");
            Matrix toScreenFitMatrix = new Matrix();
            float fInitScaleFactor;
            if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // landscape
                toScreenFitMatrix.setScale((float) height / mBitmapBackground.getHeight(), (float) height / mBitmapBackground.getHeight(), 0, 0);
                toScreenFitMatrix.postTranslate((width - ((float) mBitmapBackground.getWidth() * height / mBitmapBackground.getHeight())) / 2, 0);
                fInitScaleFactor = (float) height / mBitmapBackground.getHeight();
            } else {
                // portrait
                toScreenFitMatrix.setScale((float) width / mBitmapBackground.getWidth(), (float) width / mBitmapBackground.getWidth(), 0, 0);
                toScreenFitMatrix.postTranslate(0, (height - ((float) mBitmapBackground.getHeight() * width / mBitmapBackground.getWidth())) / 2);
                fInitScaleFactor = (float) width / mBitmapBackground.getWidth();
            }
            mfMinScaleFactor = fInitScaleFactor / 1.2f;
            mfMaxScaleFactor = fInitScaleFactor * 3f;

            contentView = new MyView(this, toScreenFitMatrix, fInitScaleFactor);
            mVideoFrameLayout.addView(contentView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            contentView.SetFreeze(mFreezeSwitch.getChecked() == IconSwitch.Checked.LEFT);
        }

        MediaPlayer mMediaPlayer = new MediaPlayer();
        // TODO: need append registry of this event and solve problem
        for (int i = 0; i < 2; i++) {
            try {
                mMediaPlayer.setDataSource(getExternalFilesDir(null).getAbsolutePath() + "/cigr_stab.mp4");
                mMediaPlayer.setSurface(new Surface(surface));
                mMediaPlayer.prepare();
                mMediaPlayer.start();
                mMediaPlayer.setLooping(true);
                break;
            } catch (IOException e) {
                mMediaPlayer.release();
                if (i == 1) {
                    finish();
                    return;
                }
                mMediaPlayer = new MediaPlayer();
            }
        }

        matrixVideoReturnToNormalScale = new Matrix();
        matrixVideoReturnToNormalScale.preScale((float) mMediaPlayer.getVideoWidth() / width, (float) mMediaPlayer.getVideoHeight() / height, 0, 0);

        Matrix firstTimeMatrix = new Matrix(matrixVideoReturnToNormalScale);
        firstTimeMatrix.postConcat(contentView.drawMatrix);
        preview.setTransform(firstTimeMatrix);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private void SaveBitmapTransparentToFile(String file_name) {
        File outputFile = new File(getExternalFilesDir(null).getAbsolutePath() + file_name);
        try {
            //noinspection ResultOfMethodCallIgnored
            outputFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            FileOutputStream fos = new FileOutputStream(outputFile);
            mBitmapTransparent.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onClickNext(View view) {
        if (bTutorialShowed) return;

        SaveBitmapTransparentToFile("/cigr_first_frame_mask.png");
        // Start creating result video

        //Getting logo on watermark on result video
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.logo_watermark);
        try {
            FileOutputStream outStream = new FileOutputStream(new File(getExternalFilesDir(null).getAbsolutePath() + "/logo_watermark.png"));
            bm.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        bm.recycle();

        //Starting generate result video
        JNIMainLib.MakeResultVideo(getExternalFilesDir(null).getAbsolutePath() + "/cigr_first_frame.png",
                getExternalFilesDir(null).getAbsolutePath() + "/cigr_first_frame_mask.png",
                getExternalFilesDir(null).getAbsolutePath() + "/cigr_stab.mp4",
                getExternalFilesDir(null).getAbsolutePath() + "/logo_watermark.png",
                getExternalFilesDir(null).getAbsolutePath() + "/cigr_result.mp4");

        Intent i = new Intent(getApplicationContext(), ResultVideoActivity.class);
        startActivity(i);

    }

    public void onClickClear(View view) {
        if (bTutorialShowed) return;

        contentView.Clear();
        seekBarWrapper.setVisibility(View.INVISIBLE);
    }

    public void onClickBrush(View view) {
        if (bTutorialShowed) return;

        seekBarWrapper.setVisibility(seekBarWrapper.getVisibility() == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mBrushSize = progress + 60;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    public void onClickTutorialClose(View view) {
        getPreferences(Context.MODE_PRIVATE).edit().putBoolean(SETTINGS_TUTORIAL_PASSED, true).apply();
        imageViewTutorial.clearAnimation();
        frameLayoutTutorial.setVisibility(View.INVISIBLE);
        bTutorialShowed = false;
        mFreezeSwitch.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
    }

    public class MyView extends View {
        RectF rectForShadow;
        RectF drawRect = new RectF();
        Matrix drawMatrix;
        Matrix drawMatrixInvert = new Matrix();
        Path transformedPath = new Path();
        float lastFocusX;
        float lastFocusY;
        int iCountTouch = 0;
        private Paint mPaint;
        private Paint mPaintUnfreeze;
        private Paint mPaintFreeze;
        private Paint mPaintShadow;
        private Paint mPaintTransparentBitmap;
        private Canvas mCanvasTransparent;
        private MultiLinePathManager multiLinePathManager;
        private ScaleGestureDetector mScaleDetector;
        private float mScaleFactor;
        private boolean mbGesture = false;
        private RectF rectOfBitmap = new RectF();

        public MyView(Context c, Matrix initDrawMatrix, float initScaleFactor) {
            super(c);
            mScaleFactor = initScaleFactor;
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            mScaleDetector = new ScaleGestureDetector(c, new ScaleGestureListener());
            drawMatrix = new Matrix(initDrawMatrix);
            drawMatrix.invert(drawMatrixInvert);

            setId(R.id.CanvasId);

            mPaintShadow = new Paint();
            mPaintShadow.setAntiAlias(true);
            mPaintShadow.setColor(0x7f000000);

            mPaintTransparentBitmap = new Paint();
            mPaintTransparentBitmap.setAntiAlias(true);
            mPaintTransparentBitmap.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));

            mPaintUnfreeze = new Paint();
            mPaintUnfreeze.setAntiAlias(true);
            mPaintUnfreeze.setColor(0x00000000);
            mPaintUnfreeze.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
            mPaintUnfreeze.setStyle(Paint.Style.STROKE);
            mPaintUnfreeze.setStrokeJoin(Paint.Join.ROUND);
            mPaintUnfreeze.setStrokeCap(Paint.Cap.ROUND);
            mPaintUnfreeze.setStrokeWidth(mBrushSize);

            mPaintFreeze = new Paint();
            mPaintFreeze.setAntiAlias(true);
            mPaintFreeze.setColor(0xFFFFFFFF);
            mPaintFreeze.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
            mPaintFreeze.setStyle(Paint.Style.STROKE);
            mPaintFreeze.setStrokeJoin(Paint.Join.ROUND);
            mPaintFreeze.setStrokeCap(Paint.Cap.ROUND);
            mPaintFreeze.setStrokeWidth(mBrushSize);

            mPaint = mPaintUnfreeze;

            if (mBitmapTransparent == null) {
                mBitmapTransparent = Bitmap.createBitmap(mBitmapBackground.getWidth(), mBitmapBackground.getHeight(), Bitmap.Config.ARGB_8888);
                mCanvasTransparent = new Canvas(mBitmapTransparent);
                mCanvasTransparent.drawColor(0xFFFFFFFF, PorterDuff.Mode.SRC);
            } else
                mCanvasTransparent = new Canvas(mBitmapTransparent);
            rectForShadow = new RectF(0, 0, mBitmapBackground.getWidth(), mBitmapBackground.getHeight());
            int MAX_POINTERS = 10;
            multiLinePathManager = new MultiLinePathManager(MAX_POINTERS);
        }

        public void Clear() {
            mCanvasTransparent.drawColor(0xFFFFFFFF, PorterDuff.Mode.SRC);
            invalidate();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
        }

        public void SetFreeze(boolean bFreeze) {
            mPaint = bFreeze ? mPaintFreeze : mPaintUnfreeze;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (iCountTouch > 0) {
                drawMatrix.mapRect(drawRect, rectForShadow);
                canvas.drawRect(drawRect, mPaintShadow);
            } else
                canvas.drawBitmap(mBitmapBackground, drawMatrix, null);
            canvas.drawBitmap(mBitmapTransparent, drawMatrix, mPaintTransparentBitmap);
            for (int i = 0; i < multiLinePathManager.superMultiPaths.length; i++) {
                transformedPath.rewind();
                transformedPath.addPath(multiLinePathManager.superMultiPaths[i]);
                transformedPath.transform(drawMatrix, null);
                mPaint.setStrokeWidth(mBrushSize);
                mPaintFreeze.setColor(0x7f000000);
                canvas.drawPath(transformedPath, mPaint);
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (bTutorialShowed) return false;

            seekBarWrapper.setVisibility(View.INVISIBLE);
            if (!mScaleDetector.onTouchEvent(event)) return false;
            if (mbGesture) {
                multiLinePathManager.clear();
                return true;
            }
            LinePath linePath;
            int index;
            int id;
            int eventMasked = event.getActionMasked();
            event.transform(drawMatrixInvert);
            switch (eventMasked) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN: {
                    index = event.getActionIndex();
                    id = event.getPointerId(index);

                    linePath = multiLinePathManager.addLinePathWithPointer(id);
                    if (linePath != null) {
                        linePath.touchStart(event.getX(index), event.getY(index));
                    } else {
                        Log.e("anupam", "Too many fingers!");
                    }
                    iCountTouch++;
                    break;
                }
                case MotionEvent.ACTION_MOVE:
                    for (int i = 0; i < event.getPointerCount(); i++) {
                        id = event.getPointerId(i);
                        index = event.findPointerIndex(id);
                        linePath = multiLinePathManager.findLinePathFromPointer(id);
                        if (linePath != null) {
                            linePath.touchMove(event.getX(index), event.getY(index));
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_CANCEL:
                    iCountTouch--;
                    index = event.getActionIndex();
                    id = event.getPointerId(index);
                    linePath = multiLinePathManager.findLinePathFromPointer(id);
                    if (linePath != null) {
                        linePath.lineTo(linePath.getLastX(), linePath.getLastY());

                        mPaint.setStrokeWidth(mBrushSize / mScaleFactor);
                        // Commit the path to our offscreen
                        mPaintFreeze.setColor(0xFFFFFFFF);
                        mCanvasTransparent.drawPath(linePath, mPaint);

                        // Kill this so we don't double draw
                        linePath.reset();

                        // Allow this LinePath to be associated to another idPointer
                        linePath.disassociateFromPointer();
                    }
                    break;
            }
            invalidate();
            return true;
        }

        public class ScaleGestureListener implements ScaleGestureDetector.OnScaleGestureListener {

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                lastFocusX = detector.getFocusX();
                lastFocusY = detector.getFocusY();
                mbGesture = true;
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float fUsedScaledFactor = detector.getScaleFactor();
                if (mScaleFactor * fUsedScaledFactor < mfMinScaleFactor) {
                    fUsedScaledFactor = mfMinScaleFactor / mScaleFactor;
                }
                if (mScaleFactor * detector.getScaleFactor() > mfMaxScaleFactor) {
                    fUsedScaledFactor = mfMaxScaleFactor / mScaleFactor;
                }
                Matrix transformationMatrix = new Matrix();
                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();

                //Zoom focus is where the fingers are centered,
                transformationMatrix.postTranslate(-focusX, -focusY);

                transformationMatrix.postScale(fUsedScaledFactor, fUsedScaledFactor);
                mScaleFactor *= fUsedScaledFactor;
/* Adding focus shift to allow for scrolling with two pointers down. Remove it to skip this functionality. This could be done in fewer lines, but for clarity I do it this way here */
                //Edited after comment by chochim
                float focusShiftX = focusX - lastFocusX;
                float focusShiftY = focusY - lastFocusY;
                transformationMatrix.postTranslate(focusX + focusShiftX, focusY + focusShiftY);
                drawMatrix.postConcat(transformationMatrix);

                rectOfBitmap.set(0, 0, mBitmapBackground.getWidth(), mBitmapBackground.getHeight());
                drawMatrix.mapRect(rectOfBitmap);
                if (rectOfBitmap.left > (contentView.getWidth() / 2)) {
                    drawMatrix.postTranslate((contentView.getWidth() / 2) - rectOfBitmap.left, 0);
                    rectOfBitmap.offset((contentView.getWidth() / 2) - rectOfBitmap.left, 0);
                }
                if (rectOfBitmap.right < (contentView.getWidth() / 2)) {
                    drawMatrix.postTranslate((contentView.getWidth() / 2) - rectOfBitmap.right, 0);
                    rectOfBitmap.offset((contentView.getWidth() / 2) - rectOfBitmap.right, 0);
                }
                if (rectOfBitmap.top > (contentView.getHeight() / 2)) {
                    drawMatrix.postTranslate(0, (contentView.getHeight() / 2) - rectOfBitmap.top);
                    rectOfBitmap.offset(0, (contentView.getHeight() / 2) - rectOfBitmap.top);
                }
                if (rectOfBitmap.bottom < (contentView.getHeight() / 2)) {
                    drawMatrix.postTranslate(0, (contentView.getHeight() / 2) - rectOfBitmap.bottom);
                    rectOfBitmap.offset(0, (contentView.getHeight() / 2) - rectOfBitmap.bottom);
                }

                drawMatrix.invert(drawMatrixInvert);
                lastFocusX = focusX;
                lastFocusY = focusY;
                invalidate();
                Matrix newMatrix = new Matrix(matrixVideoReturnToNormalScale);
                newMatrix.postConcat(drawMatrix);
                preview.setTransform(newMatrix);
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                mbGesture = false;
            }
        }

        private class LinePath extends Path {
            private Integer idPointer;
            private float lastX;
            private float lastY;

            LinePath() {
                this.idPointer = null;
            }

            float getLastX() {
                return lastX;
            }

            float getLastY() {
                return lastY;
            }

            void touchStart(float x, float y) {
                this.reset();
                this.moveTo(x, y);
                this.lastX = x;
                this.lastY = y;
            }

            void touchMove(float x, float y) {
                float dx = Math.abs(x - lastX);
                float dy = Math.abs(y - lastY);
                if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                    this.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2);
                    lastX = x;
                    lastY = y;
                }
            }

            boolean isDisassociatedFromPointer() {
                return idPointer == null;
            }

            boolean isAssociatedToPointer(int idPointer) {
                return this.idPointer != null
                        && this.idPointer == idPointer;
            }

            void disassociateFromPointer() {
                idPointer = null;
            }

            void associateToPointer(int idPointer) {
                this.idPointer = idPointer;
            }
        }

        private class MultiLinePathManager {
            LinePath[] superMultiPaths;

            MultiLinePathManager(int maxPointers) {
                superMultiPaths = new LinePath[maxPointers];
                for (int i = 0; i < maxPointers; i++) {
                    superMultiPaths[i] = new LinePath();
                }
            }

            LinePath findLinePathFromPointer(int idPointer) {
                for (LinePath superMultiPath : superMultiPaths) {
                    if (superMultiPath.isAssociatedToPointer(idPointer)) {
                        return superMultiPath;
                    }
                }
                return null;
            }

            LinePath addLinePathWithPointer(int idPointer) {
                for (LinePath superMultiPath : superMultiPaths) {
                    if (superMultiPath.isDisassociatedFromPointer()) {
                        superMultiPath.associateToPointer(idPointer);
                        return superMultiPath;
                    }
                }
                return null;
            }

            void clear() {
                for (LinePath superMultiPath : superMultiPaths) {
                    superMultiPath.rewind();
                    superMultiPath.disassociateFromPointer();
                }
            }
        }
    }
}
