package com.smadec.maxx.cigr;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class StabilizeActivity extends AppCompatActivity {
    public boolean bBackPressed = false;
    TinglingSquaresView2 sw;
    TextView progressText;
    String szFileName;
    Timer timer;
    TimerTask timerTask;
    double dProgress = 0;
    private Handler mHandler;
    private ProgressBar progressBar;

    private FirebaseAnalytics mFirebaseAnalytics;
    private AdView mAdView;

    @Override
    public void onBackPressed() {
        if (bBackPressed)
            return;
        bBackPressed = true;
        timer.cancel();
        progressBar.setVisibility(ProgressBar.VISIBLE);
        progressText.setText(R.string.stabilize_exiting);
        new Thread(new Runnable() {
            @Override
            public void run() {
                JNIMainLib.StabilizerSyncStopStabilize();
                mHandler.obtainMessage(4).sendToTarget();
            }
        }).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stabilize);

        mAdView = findViewById(R.id.adView2);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        sw = findViewById(R.id.tinglingSquaresView);
        sw.StartAnimation();

        progressBar = findViewById(R.id.progressBarExit);

        progressText = findViewById(R.id.textProgress);
        Intent intent = getIntent();
        szFileName = intent.getStringExtra("filename");

        mHandler = new IncomingHandler(this);

        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                dProgress = JNIMainLib.StabilizerCheckSyncFile();
                if (dProgress < 0) {
                    timer.cancel();
                    if (dProgress == -1)
                        mHandler.obtainMessage(2).sendToTarget();
                    else
                        mHandler.obtainMessage(3).sendToTarget();
                } else
                    mHandler.obtainMessage(1).sendToTarget();
            }
        };
        timer.schedule(timerTask, 1000, 1000);
    }

    private void ShowDialogExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_cannot_stabilize_header)
                .setMessage(R.string.dialog_cannot_stabilize_body)
                .setCancelable(false)
                .setNegativeButton(R.string.OK_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    static class IncomingHandler extends Handler {
        private final WeakReference<StabilizeActivity> mService;

        IncomingHandler(StabilizeActivity service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            StabilizeActivity service = mService.get();
            if (service != null) {
                switch (msg.what) {
                    case 1:
                        service.progressText.setText(String.format(Locale.getDefault(), "%.0f%%", (service.dProgress * 100)));
                        break;
                    case 2:
                        Intent i = new Intent(service.getApplicationContext(), VideoEditActivity.class);
                        service.startActivity(i);
                        service.finish();
                        return;
                    case 3:
                        service.ShowDialogExit();
                        return;
                    case 4:
                        service.bBackPressed = false;
                        service.progressBar.setVisibility(ProgressBar.INVISIBLE);
                        service.finish();
                }
            }
        }
    }
}
