package com.smadec.maxx.cigr;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class ResultVideoActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {
    public static final int DIALOG_PERMISSION_RATIONALE_FILE = 101;
    public static final int DIALOG_PERMISSION_WRONG_FILE = 104;
    public static final int DIALOG_ASK_GOOGLE_PLAY_RATE = 105;
    public static final int MY_PERMISSIONS_REQUEST_FILE = 101;

    private static final String INSTAGRAM_PACKAGE_NAME = "com.instagram.android";
    private static final String FACEBOOK_PACKAGE_NAME = "com.facebook.katana";
    static private final String SETTINGS_FILE_TIME = "file_time";

    private static final String COUNT_SAVED_FILES = "files_count";
    private static final String PLAY_RATE_SHOWED = "play_rate_showed";
    boolean bSaveToNetwork = false;

    boolean bSurfaceCreated = false;
    boolean bFileCreated = false;
    private TextureView preview;
    private ProgressBar progressBar;
    private Handler mHandler;
    private Timer timer;
    private long mFileTime;

    private FirebaseAnalytics mFirebaseAnalytics;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_video);

        mAdView = findViewById(R.id.adView4);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        mHandler = new IncomingHandler(this);

        progressBar = findViewById(R.id.progressBarMakeResultVideo);
        if (JNIMainLib.CheckResultVideo() == 0) {
            bFileCreated = true;
            progressBar.setVisibility(ProgressBar.INVISIBLE);
        } else {
            progressBar.setVisibility(ProgressBar.VISIBLE);
            timer = new Timer();
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    if (JNIMainLib.CheckResultVideo() == 0) {
                        timer.cancel();
                        bFileCreated = true;
                        mHandler.obtainMessage(1).sendToTarget();
                    }
                }
            };
            timer.schedule(timerTask, 500, 500);
        }

        preview = findViewById(R.id.textureView);
        preview.setSurfaceTextureListener(this);
    }

    void copyFile(File src, File dst) throws IOException {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null)
                inChannel.close();
            outChannel.close();
        }
    }

    private void CopyFileWithoutCheckPermission() {
        File fromFile = new File(getExternalFilesDir(null).getAbsolutePath() + "/cigr_result.mp4");
        if (mFileTime == fromFile.lastModified()) {
            Toast.makeText(this, R.string.toast_result_video_seem_file, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "store_to_file");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

            new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/Camera/").mkdirs();

            copyFile(fromFile,
                    new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/Camera/cigr_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".mp4"));

            MediaScannerConnection.scanFile(this,
                    new String[]{Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/Camera/cigr_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".mp4"},
                    null,
                    null);

            Toast.makeText(this, R.string.toast_result_video_succesfull, Toast.LENGTH_SHORT).show();
            mFileTime = fromFile.lastModified();
            getPreferences(Context.MODE_PRIVATE).edit().putLong(SETTINGS_FILE_TIME, mFileTime).apply();
            CheckAskRateOnPlayMarket();
        } catch (IOException e) {
            Toast.makeText(this, R.string.toast_result_video_error, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void onSaveToGalery(View view) {
        if (!bFileCreated)
            return;
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                ShowAlertDialog(DIALOG_PERMISSION_RATIONALE_FILE);
            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_FILE);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            CopyFileWithoutCheckPermission();
        }
    }

    private void ShowAlertDialog(int types_dialogs) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (types_dialogs) {
            case DIALOG_PERMISSION_RATIONALE_FILE:
                builder.setMessage(R.string.dialog_permission_rationale_file_text)
                        .setPositiveButton(R.string.OK_button, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                ActivityCompat.requestPermissions(ResultVideoActivity.this,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        MY_PERMISSIONS_REQUEST_FILE);
                            }
                        });
                break;
            case DIALOG_PERMISSION_WRONG_FILE:
                builder.setMessage(R.string.dialog_permission_wrong_file_text)
                        .setPositiveButton(R.string.exit_button, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        })
                        .setNegativeButton(R.string.reask_button, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                ActivityCompat.requestPermissions(ResultVideoActivity.this,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        MY_PERMISSIONS_REQUEST_FILE);
                            }
                        });

                break;
            case DIALOG_ASK_GOOGLE_PLAY_RATE:
                builder.setMessage(R.string.dialog_ask_rewiew_play_market)
                        .setNegativeButton(R.string.not_now_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setPositiveButton(R.string.rate_button, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent goToMarket = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details?id=com.smadec.maxx.cigr"));
                                startActivity(goToMarket);
                            }
                        });
                break;
        }
        // Create the AlertDialog object and return it
        builder.create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_FILE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    CopyFileWithoutCheckPermission();
                } else {
                    ShowAlertDialog(DIALOG_PERMISSION_WRONG_FILE);
                }
            }
        }
    }

    void PlayResultVideo(SurfaceTexture surface) {
        if (!bSurfaceCreated || !bFileCreated)
            return;
        MediaPlayer mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(getExternalFilesDir(null).getAbsolutePath() + "/cigr_result.mp4");
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaPlayer.setSurface(new Surface(surface));
        try {
            mMediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaPlayer.start();
        mMediaPlayer.setLooping(true);

        ViewGroup.LayoutParams lp = preview.getLayoutParams();

        // here we adjust the size of the displayed preview so that there are no distortions

        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // landscape
            lp.width = mMediaPlayer.getVideoWidth() * preview.getHeight() / mMediaPlayer.getVideoHeight();
            lp.height = preview.getHeight();
        } else {
            // portrait
            lp.height = mMediaPlayer.getVideoHeight() * preview.getWidth() / mMediaPlayer.getVideoWidth();
            lp.width = preview.getWidth();
        }

        preview.setLayoutParams(lp);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        bSurfaceCreated = true;
        PlayResultVideo(surface);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        bSurfaceCreated = false;
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private boolean isPackageInstalled(String szPackageName) {
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo(szPackageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void createIntentForSocial(String filename, String szPackageName) {
        // Create the new Intent using the 'Send' action.
        Intent share = new Intent(Intent.ACTION_SEND);

        // Set the MIME type
        share.setType("video/*");
        share.setPackage(szPackageName);

        // Create the URI from the media
        File media = new File(filename);
        Uri uri = Uri.fromFile(media);

        // Add the URI to the Intent.
        share.putExtra(Intent.EXTRA_STREAM, uri);

        // Broadcast the Intent.
        startActivity(Intent.createChooser(share, "Share to"));

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "store_to_social");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    public void onSaveToSocial(String szNameSocialPackage) {
        if (!bFileCreated)
            return;
        if (isPackageInstalled(szNameSocialPackage)) {
            bSaveToNetwork = true;
            createIntentForSocial(getExternalFilesDir(null).getAbsolutePath() + "/cigr_result.mp4", szNameSocialPackage);
        } else {
            Intent goToMarket = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details?id=" + szNameSocialPackage));
            startActivity(goToMarket);
        }
    }

    public void onSaveToFacebook(View view) {
        onSaveToSocial(FACEBOOK_PACKAGE_NAME);
    }

    public void onSaveToInsta(View view) {
        onSaveToSocial(INSTAGRAM_PACKAGE_NAME);
    }

    private void CheckAskRateOnPlayMarket() {
        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
        if (pref.getBoolean(PLAY_RATE_SHOWED, false))
            return;
        int iCount = pref.getInt(COUNT_SAVED_FILES, 0);
        if (iCount >= 2) {
            ShowAlertDialog(DIALOG_ASK_GOOGLE_PLAY_RATE);
            pref.edit().putBoolean(PLAY_RATE_SHOWED, true).apply();
        }
        pref.edit().putInt(COUNT_SAVED_FILES, iCount + 1).apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bSaveToNetwork)
            CheckAskRateOnPlayMarket();
        bSaveToNetwork = false;
    }

    public void onShare(View view) {
        // Create the new Intent using the 'Send' action.
        Intent share = new Intent(Intent.ACTION_SEND);

        // Set the MIME type
        share.setType("video/*");

        // Create the URI from the media
        Uri uri = Uri.fromFile(new File(getExternalFilesDir(null).getAbsolutePath() + "/cigr_result.mp4"));

        // Add the URI to the Intent.
        share.putExtra(Intent.EXTRA_STREAM, uri);

        // Broadcast the Intent.
        startActivity(Intent.createChooser(share, "Share to"));

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "store_to_somewhere");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    static class IncomingHandler extends Handler {
        private final WeakReference<ResultVideoActivity> mService;

        IncomingHandler(ResultVideoActivity service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            ResultVideoActivity service = mService.get();
            if (service != null) {
                switch (msg.what) {
                    case 1:
                        service.progressBar.setVisibility(ProgressBar.INVISIBLE);
                        service.PlayResultVideo(service.preview.getSurfaceTexture());
                }
            }
        }
    }
}
