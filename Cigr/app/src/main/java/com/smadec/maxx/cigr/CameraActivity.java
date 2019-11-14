package com.smadec.maxx.cigr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback, Camera.AutoFocusCallback, View.OnTouchListener, TextureView.SurfaceTextureListener, RecordButton.RecordButtonClickListener {

    public static final int MY_PERMISSIONS_REQUEST_CAMERA = 100;
    public static final int CAMERA_ID_BACK = 99;
    public static final int CAMERA_ID_FRONT = 98;
    public static final int DIALOG_PERMISSION_RATIONALE_CAMERA = 100;
    public static final int DIALOG_PERMISSION_WRONG_CAMERA = 103;
    private static final float FOCUS_AREA_SIZE = 75000f;
    static private final String CAMERA_INDEX_STORAGE_KEY = "cam_index";
    private static final int CURRENT_STATUS_OBSERVE = 100;
    private static final int CURRENT_STATUS_FOCUSING = 101;
    private static final int CURRENT_STATUS_RECORDING = 102;
    private static final int CURRENT_STATUS_CANCEL_RECORDING = 103;
    static private final String SETTINGS_TUTORIAL_PASSED = "tutorial";
    static private final String SETTINGS_CANNOT_FOCUS[] = {"cannot_focus_back", "cannot_focus_front"};
    private int cameraIndex = CAMERA_ID_BACK;
    private int cameraOrientation = 0;
    private int cameraOrientationResult = 0;
    private int mCurrentStatus = CURRENT_STATUS_OBSERVE;
    private Handler mCameraResetFocusingHandler;
    private long m_lCurrentTimeInMS = 0;
    private FrameData mBuffer[];
    private int mChainIdx = 0;
    private boolean mCameraFrameReady = false;
    private Thread mThread;
    private boolean m_bPreviewReady = false;
    private Camera camera;
    private SurfaceHolder surfaceHolder;
    private SurfaceView preview;
    private TextureView previewFront;
    private final Runnable mCameraResetFocusingRunnable = new Runnable() {
        public void run() {
            try {
                if (mCurrentStatus == CURRENT_STATUS_OBSERVE) {
                    CameraSetToContinuousPicture();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    private FirebaseAnalytics mFirebaseAnalytics;
    private RecordButton recordButton;
    private Camera.Size mPreviewSize;
    private DisplayOrientationDetector mDisplayOrientationDetector;
    private boolean bCannotFocus[] = {false, false};

    private AdView mAdView;

    public CameraActivity() {
        new MyExceptionHandler(this);

        mBuffer = new FrameData[3];
        for (int i = 0; i < mBuffer.length; i++)
            mBuffer[i] = new FrameData();
    }

    private void CameraSetToContinuousPicture() {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            if (parameters.isAutoExposureLockSupported()) {
                parameters.setAutoExposureLock(false);
            }
            if (parameters.isAutoWhiteBalanceLockSupported()) {
                parameters.setAutoWhiteBalanceLock(false);
            }
            try {
                camera.setParameters(parameters);
            } catch (Exception e) {

            }
            parameters.setFocusAreas(null);
            parameters.setMeteringAreas(null);
            ClearFocusRect();
            try {
                camera.setParameters(parameters);
            } catch (Exception e) {

            }
        }
    }

    private void ClearFocusRect() {
        Canvas canvas = previewFront.lockCanvas();
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        previewFront.unlockCanvasAndPost(canvas);
    }

    private void DrawFocusRect(float posX, float posY) {
        Canvas canvas = previewFront.lockCanvas();
        if (canvas == null)
            return;
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(3);
        canvas.drawRect(posX - 50, posY - 50, posX + 50, posY + 50, paint);

        previewFront.unlockCanvasAndPost(canvas);
    }

    private void OpenCamera() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (((cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) && (cameraIndex == CAMERA_ID_BACK)) ||
                    ((cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) && (cameraIndex == CAMERA_ID_FRONT))) {
                cameraOrientation = cameraInfo.orientation;
                camera = Camera.open(camIdx);
                recordButton.StopRecording();
                mCurrentStatus = CURRENT_STATUS_OBSERVE;
                break;
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            cameraIndex = savedInstanceState.getInt(CAMERA_INDEX_STORAGE_KEY);
        }
        super.onCreate(savedInstanceState);
        mDisplayOrientationDetector = new DisplayOrientationDetector(this) {
            @Override
            public void onDisplayOrientationChanged(int displayOrientation) {
                setCameraDisplayOrientation();
            }
        };
        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        // if you want the application to always have portrait orientation
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // if we want the application to be fullscreen
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //want the screen to remain on
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // and without title
        //requestWindowFeature(Window.FEATURE_NO_TITLE);

        mCameraResetFocusingHandler = new Handler();

        setContentView(R.layout.activity_camera);
        recordButton = findViewById(R.id.circle);
        recordButton.setClickListener(this);

        MobileAds.initialize(this, "ca-app-pub-9720148464010296~4539040693");

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        bCannotFocus[0] = sharedPref.getBoolean(SETTINGS_CANNOT_FOCUS[0], false);
        bCannotFocus[1] = sharedPref.getBoolean(SETTINGS_CANNOT_FOCUS[1], false);
        if (!sharedPref.getBoolean(SETTINGS_TUTORIAL_PASSED, false)) {
            sharedPref.edit().putBoolean(SETTINGS_TUTORIAL_PASSED, true).apply();
            Intent i = new Intent(getApplicationContext(), TutorialActivity.class);
            startActivity(i);
            finish();
            return;
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                ShowAlertDialog(DIALOG_PERMISSION_RATIONALE_CAMERA);
            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_CAMERA);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            prepareSurfaceForOpenCamera();
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CAMERA_INDEX_STORAGE_KEY, cameraIndex);

    }

    private void CancelRecord() {
        mCurrentStatus = CURRENT_STATUS_CANCEL_RECORDING;
        recordButton.StopRecording();
        CameraSetToContinuousPicture();
        synchronized (this) {
            this.notify();
        }
    }

    @Override
    public void onBackPressed() {
        if (mCurrentStatus == CURRENT_STATUS_FOCUSING) {
            camera.cancelAutoFocus();
            mCurrentStatus = CURRENT_STATUS_OBSERVE;
            recordButton.StopRecording();
            CameraSetToContinuousPicture();
            return;
        }
        if (mCurrentStatus == CURRENT_STATUS_RECORDING) {
            CancelRecord();
            return;
        }
        super.onBackPressed();
    }

    private void prepareSurfaceForOpenCamera() {
        preview = findViewById(R.id.SurfaceView01);
        previewFront = findViewById(R.id.TextureViewCameraFront);
        previewFront.setOnTouchListener(this);
        previewFront.setSurfaceTextureListener(this);
        surfaceHolder = preview.getHolder();
        //noinspection deprecation
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.addCallback(this);
    }

    protected void CloseCamera() {
        if (camera != null) {
            synchronized (this) {
                if (mCurrentStatus == CURRENT_STATUS_FOCUSING) {
                    camera.cancelAutoFocus();
                    mCurrentStatus = CURRENT_STATUS_OBSERVE;
                    recordButton.StopRecording();
                }
                if (mCurrentStatus == CURRENT_STATUS_RECORDING) {
                    mCurrentStatus = CURRENT_STATUS_CANCEL_RECORDING;
                    recordButton.StopRecording();
                }
                this.notify();
            }
            try {
                if (mThread != null) {
                    mThread.join();
                    mThread = null;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            JNIMainLib.EncoderUninit(false);
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDisplayOrientationDetector.enable(getWindowManager().getDefaultDisplay());
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            OpenCamera();
            if (m_bPreviewReady)
                InitCameraPreview(surfaceHolder);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    OpenCamera();
                    prepareSurfaceForOpenCamera();
                    preview.setVisibility(View.INVISIBLE);
                    preview.setVisibility(View.VISIBLE);
                } else {
                    ShowAlertDialog(DIALOG_PERMISSION_WRONG_CAMERA);
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
        }
    }

    private Camera.Size getBestPreviewSize(Camera.Parameters parameters) {
        List<Camera.Size> xxx = parameters.getSupportedPreviewSizes();
        Camera.Size ret = null;
        for (Camera.Size val : xxx) {
            if (((val.width * 3) == (val.height * 4)) && (val.height <= 1080) && ((ret == null) || (ret.width < val.width)))
                ret = val;
        }
        return ret;
    }

    void setCameraDisplayOrientation() {
        if (camera != null) {
            // determine how much the screen is rotated from its normal position
            int degrees = mDisplayOrientationDetector.getLastKnownDisplayOrientation();
            cameraOrientationResult = 0;

            // back camera
            if (cameraIndex == CAMERA_ID_BACK) {
                cameraOrientationResult = ((360 - degrees) + cameraOrientation);
            } else
                // front camera
                if (cameraIndex == CAMERA_ID_FRONT) {
                    cameraOrientationResult = ((360 - degrees) - cameraOrientation);
                    cameraOrientationResult += 360;
                }
            cameraOrientationResult %= 360;
            camera.setDisplayOrientation(cameraOrientationResult);
        }
    }

    private void InitCameraPreview(SurfaceHolder holder) {

        Camera.Parameters parameters = camera.getParameters();
        mPreviewSize = getBestPreviewSize(parameters);
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);

        int previewSurfaceWidth = preview.getWidth();
        int previewSurfaceHeight = preview.getHeight();

        ViewGroup.LayoutParams lp = preview.getLayoutParams();

        // here we adjust the size of the displayed preview so that there are no distortions

        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // landscape
            lp.width = previewSurfaceHeight * mPreviewSize.width / mPreviewSize.height;
            lp.height = previewSurfaceHeight;
        } else {
            // portrait view
            lp.height = (previewSurfaceWidth * mPreviewSize.width / mPreviewSize.height);
            lp.width = previewSurfaceWidth;
        }
        setCameraDisplayOrientation();
        JNIMainLib.EncoderInit(mPreviewSize.width, mPreviewSize.height,
                (cameraIndex == CAMERA_ID_FRONT) ? (360 - cameraOrientationResult) % 360 : cameraOrientationResult,
                getExternalFilesDir(null).getAbsolutePath());

        preview.setLayoutParams(lp);
        ViewGroup.LayoutParams lp2 = previewFront.getLayoutParams();
        lp2.height = lp.height;
        lp2.width = lp.width;
        previewFront.setLayoutParams(lp2);

        if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        parameters.setPreviewFormat(ImageFormat.YV12);

        for (FrameData aMBuffer : mBuffer)
            aMBuffer.Init(mPreviewSize.width * mPreviewSize.height * ImageFormat.getBitsPerPixel(parameters.getPreviewFormat()) / 8);

        camera.setParameters(parameters);
        camera.addCallbackBuffer(mBuffer[2].GetBuffer());
        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        camera.setPreviewCallbackWithBuffer(this);
        camera.startPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDisplayOrientationDetector.disable();
        CloseCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        m_bPreviewReady = true;
        if (camera != null) {
            InitCameraPreview(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        m_bPreviewReady = false;
        CloseCamera();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
                onClicked();
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void onClicked() {
        switch (mCurrentStatus) {
            case CURRENT_STATUS_RECORDING:
                if ((System.currentTimeMillis() - m_lCurrentTimeInMS) < 3200) {
                    CancelRecord();
                    Toast.makeText(this, R.string.toast_video_to_short, Toast.LENGTH_SHORT).show();
                } else
                    m_lCurrentTimeInMS = 0;
                break;
            case CURRENT_STATUS_FOCUSING:
                break;
            case CURRENT_STATUS_OBSERVE:
                if (camera == null)
                    return;
                recordButton.ToFocusing();
                mCurrentStatus = CURRENT_STATUS_FOCUSING;
                Camera.Parameters parameters = camera.getParameters();
                if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                camera.setParameters(parameters);
                camera.autoFocus(this);
                break;
        }
    }

    /**
     * Convert touch position x:y to {@link android.hardware.Camera.Area} position -1000:-1000 to 1000:1000.
     */
    private Rect calculateTapArea(float x, float y) {
        float cameraX = 0, cameraY = 0;
        if (cameraIndex == CAMERA_ID_BACK) {
            switch (cameraOrientationResult) {
                case 90:
                    cameraX = y;
                    cameraY = mPreviewSize.height - x;
                    break;
                case 180:
                    cameraX = mPreviewSize.width - x;
                    cameraY = mPreviewSize.height - y;
                    break;
                case 270:
                    cameraX = mPreviewSize.width - y;
                    cameraY = x;
                    break;
                case 0:
                    cameraX = x;
                    cameraY = y;
                    break;

            }
        } else {
            switch (cameraOrientationResult) {
                case 90:
                    cameraX = mPreviewSize.width - y;
                    cameraY = mPreviewSize.height - x;
                    break;
                case 180:
                    cameraX = x;
                    cameraY = mPreviewSize.height - y;
                    break;
                case 270:
                    cameraX = y;
                    cameraY = x;
                    break;
                case 0:
                    cameraX = mPreviewSize.width - x;
                    cameraY = y;
                    break;

            }
        }
        cameraX = cameraX * 2000f / mPreviewSize.width - 1000;
        cameraY = cameraY * 2000f / mPreviewSize.height - 1000;

        int left = clamp((int) (cameraX - (FOCUS_AREA_SIZE / mPreviewSize.width)));
        int top = clamp((int) (cameraY - (FOCUS_AREA_SIZE / mPreviewSize.height)));
        int right = clamp((int) (cameraX + (FOCUS_AREA_SIZE / mPreviewSize.width)));
        int bottom = clamp((int) (cameraY + (FOCUS_AREA_SIZE / mPreviewSize.height)));

        return new Rect(left, top, right, bottom);
    }

    private int clamp(int x) {
        if (x > 1000)
            return 1000;
        if (x < -1000)
            return -1000;
        return x;
    }

    private boolean CheckFocusing(boolean paramBoolean) {
        int iNewCamNum = (cameraIndex == CAMERA_ID_BACK) ? 0 : 1;
        if (paramBoolean) {
            if (bCannotFocus[iNewCamNum]) {
                bCannotFocus[iNewCamNum] = false;
                SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
                editor.remove(SETTINGS_CANNOT_FOCUS[iNewCamNum]);
                editor.apply();
            }
            return true;
        } else {
            if (bCannotFocus[iNewCamNum]) {
                return true;
            } else {
                bCannotFocus[iNewCamNum] = true;
                SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
                editor.putBoolean(SETTINGS_CANNOT_FOCUS[iNewCamNum], true);
                editor.apply();
                return false;
            }
        }
    }

    @Override
    public void onAutoFocus(boolean paramBoolean, Camera paracamera) {
        if (mCurrentStatus == CURRENT_STATUS_FOCUSING) {
            if (CheckFocusing(paramBoolean)) {
                // if you managed to focus, start recording
                Camera.Parameters parameters = camera.getParameters();
                if (parameters.isAutoExposureLockSupported()) {
                    parameters.setAutoExposureLock(true);
                }
                if (parameters.isAutoWhiteBalanceLockSupported()) {
                    parameters.setAutoWhiteBalanceLock(true);
                }
                if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
                }
                camera.cancelAutoFocus();
                camera.setParameters(parameters);

                recordButton.StartRecording();
                mThread = new Thread(new MediaRecorderWorker());
                mCurrentStatus = CURRENT_STATUS_RECORDING;
                m_lCurrentTimeInMS = System.currentTimeMillis();
                mThread.start();
            } else {
                Toast.makeText(this, R.string.toast_cannot_focus, Toast.LENGTH_SHORT).show();
                mCurrentStatus = CURRENT_STATUS_OBSERVE;
                CameraSetToContinuousPicture();
                recordButton.StopRecording();
            }
        }
    }

    @Override
    public void onPreviewFrame(byte[] paramArrayOfByte, Camera paracamera) {
        if (mCurrentStatus == CURRENT_STATUS_RECORDING) {
            long pts = System.currentTimeMillis() - m_lCurrentTimeInMS;
            if (pts >= 5000) {
                recordButton.StopRecording();
                synchronized (this) {
                    mCurrentStatus = CURRENT_STATUS_OBSERVE;
                    this.notify();
                }
            } else {
                if (pts >= 3100)
                    recordButton.CanFinish();
                synchronized (this) {
                    System.arraycopy(mBuffer[2].GetBuffer(), 0, mBuffer[mChainIdx].GetBuffer(), 0, mBuffer[2].GetBuffer().length);
                    mBuffer[mChainIdx].SetTime(pts);
                    mCameraFrameReady = true;
                    this.notify();
                }
            }
        }
        camera.addCallbackBuffer(mBuffer[2].GetBuffer());
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if ((event.getAction() == MotionEvent.ACTION_DOWN)
                && (camera != null)
                && (mCurrentStatus == CURRENT_STATUS_OBSERVE)) {

            camera.cancelAutoFocus();
            Rect focusRect = calculateTapArea(event.getX(), event.getY());

            Camera.Parameters parameters = camera.getParameters();
            if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }

            if (parameters.getMaxNumFocusAreas() > 0) {
                List<Camera.Area> mylist = new ArrayList<>();
                mylist.add(new Camera.Area(focusRect, 1000));
                parameters.setFocusAreas(mylist);
            }
            if (parameters.getMaxNumMeteringAreas() > 0) {
                List<Camera.Area> mylist = new ArrayList<>();
                mylist.add(new Camera.Area(focusRect, 1000));
                parameters.setMeteringAreas(mylist);
            }
            DrawFocusRect(event.getX(), event.getY());

            try {
                camera.cancelAutoFocus();
                camera.setParameters(parameters);
                camera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        mCameraResetFocusingHandler.removeCallbacks(mCameraResetFocusingRunnable);
                        mCameraResetFocusingHandler.postDelayed(mCameraResetFocusingRunnable, 5000);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private void ShowAlertDialog(int types_dialogs) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (types_dialogs) {
            case DIALOG_PERMISSION_RATIONALE_CAMERA:
                builder.setMessage(R.string.dialog_permission_rationale_camera_text)
                        .setPositiveButton(R.string.OK_button, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                ActivityCompat.requestPermissions(CameraActivity.this,
                                        new String[]{Manifest.permission.CAMERA},
                                        MY_PERMISSIONS_REQUEST_CAMERA);
                            }
                        });
                break;
            case DIALOG_PERMISSION_WRONG_CAMERA:
                builder.setMessage(R.string.dialog_permission_wrong_camera_text)
                        .setPositiveButton(R.string.exit_button, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                            }
                        })
                        .setNegativeButton(R.string.reask_button, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                ActivityCompat.requestPermissions(CameraActivity.this,
                                        new String[]{Manifest.permission.CAMERA},
                                        MY_PERMISSIONS_REQUEST_CAMERA);
                            }
                        });
                break;
        }
        // Create the AlertDialog object and return it
        builder.create().show();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        ClearFocusRect();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        ClearFocusRect();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    public void onClickChangeCameraSide(View view) {
        cameraIndex = ((cameraIndex == CAMERA_ID_BACK) ? CAMERA_ID_FRONT : CAMERA_ID_BACK);
        CloseCamera();
        OpenCamera();
        if (m_bPreviewReady)
            InitCameraPreview(surfaceHolder);
    }

    private class FrameData {
        private byte mBuffer[];
        private long mTime;

        FrameData() {
        }

        public void Init(int size) {
            if ((mBuffer == null) || (mBuffer.length != size)) {
                mBuffer = new byte[size];
            }
        }

        byte[] GetBuffer() {
            return mBuffer;
        }

        long GetTime() {
            return mTime;
        }

        void SetTime(long lTime) {
            mTime = lTime;
        }
    }

    public class MyExceptionHandler implements Thread.UncaughtExceptionHandler {

        private final CameraActivity context;
        private final Thread.UncaughtExceptionHandler rootHandler;

        MyExceptionHandler(CameraActivity context) {
            this.context = context;
            // we should store the current exception handler -- to invoke it for all not handled exceptions ...
            rootHandler = Thread.getDefaultUncaughtExceptionHandler();
            // we replace the exception handler now with us -- we will properly dispatch the exceptions ...
            Thread.setDefaultUncaughtExceptionHandler(this);
        }

        @Override
        public void uncaughtException(final Thread thread, final Throwable ex) {
            context.CloseCamera();
            if (rootHandler != null)
                rootHandler.uncaughtException(thread, ex);
        }
    }

    private class MediaRecorderWorker implements Runnable {
        @Override
        public void run() {
            do {
                boolean hasFrame = false;
                synchronized (CameraActivity.this) {
                    try {
                        while (!mCameraFrameReady && (mCurrentStatus == CURRENT_STATUS_RECORDING)) {
                            CameraActivity.this.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (mCameraFrameReady) {
                        mChainIdx = 1 - mChainIdx;
                        mCameraFrameReady = false;
                        hasFrame = true;
                    }
                }
                if ((mCurrentStatus == CURRENT_STATUS_RECORDING) && hasFrame) {
                    JNIMainLib.EncoderSendFrame(mBuffer[1 - mChainIdx].GetBuffer(), mBuffer[1 - mChainIdx].GetTime());
                }
            } while (mCurrentStatus == CURRENT_STATUS_RECORDING);
            if (mCurrentStatus == CURRENT_STATUS_CANCEL_RECORDING) {
                JNIMainLib.EncoderUninit(false);
                JNIMainLib.EncoderInit(mPreviewSize.width, mPreviewSize.height,
                        (cameraIndex == CAMERA_ID_FRONT) ? (360 - cameraOrientationResult) % 360 : cameraOrientationResult,
                        getExternalFilesDir(null).getAbsolutePath());
                mCurrentStatus = CURRENT_STATUS_OBSERVE;
            } else {
                JNIMainLib.EncoderUninit(true);
                Intent i = new Intent(getApplicationContext(), StabilizeActivity.class);
                i.putExtra("filename", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/cigr.mp4");
                startActivity(i);
            }
        }
    }
}
