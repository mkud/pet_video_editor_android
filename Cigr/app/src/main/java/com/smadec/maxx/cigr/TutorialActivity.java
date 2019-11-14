package com.smadec.maxx.cigr;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ViewFlipper;

import com.google.firebase.analytics.FirebaseAnalytics;

public class TutorialActivity extends AppCompatActivity {

    Animation animationFlipIn_ltr;
    Animation animationFlipOut_ltr;
    Animation animationFlipIn_rtl;
    Animation animationFlipOut_rtl;
    AnimationDrawable frameAnimation;
    private ViewFlipper vflipper;
    private Button buttonPrev;
    private Button buttonNext;
    private ImageView mAnimationStep4;

    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);
        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        vflipper = findViewById(R.id.tutorialFlipper);
        buttonNext = findViewById(R.id.tutorial_button_next);
        buttonPrev = findViewById(R.id.tutorial_button_prev);
        animationFlipIn_ltr = AnimationUtils.loadAnimation(this,
                R.anim.flipin_ltr);
        animationFlipOut_ltr = AnimationUtils.loadAnimation(this,
                R.anim.flipout_ltr);
        animationFlipIn_rtl = AnimationUtils.loadAnimation(this,
                R.anim.flipin_rtl);
        animationFlipOut_rtl = AnimationUtils.loadAnimation(this,
                R.anim.flipout_rtl);

        mAnimationStep4 = findViewById(R.id.imageViewTutorial4);
        mAnimationStep4.setImageResource(R.drawable.tutorial4anim);

        // Get the background, which has been compiled to an AnimationDrawable object.
        frameAnimation = (AnimationDrawable) mAnimationStep4.getDrawable();

    }

    public void prevClick(View view) {
        vflipper.setInAnimation(animationFlipIn_ltr);
        vflipper.setOutAnimation(animationFlipOut_ltr);

        vflipper.showPrevious();
        buttonNext.setText(R.string.button_next);
        frameAnimation.stop();
        if (vflipper.getDisplayedChild() == 0)
            buttonPrev.setVisibility(View.INVISIBLE);
    }

    public void nextClick(View view) {
        if (vflipper.getDisplayedChild() == vflipper.getChildCount() - 1) {
            Intent i = new Intent(getApplicationContext(), CameraActivity.class);
            startActivity(i);
            finish();
            return;
        }
        vflipper.setInAnimation(animationFlipIn_rtl);
        vflipper.setOutAnimation(animationFlipOut_rtl);
        vflipper.showNext();
        buttonPrev.setVisibility(View.VISIBLE);
        if (vflipper.getDisplayedChild() == vflipper.getChildCount() - 1) {
            buttonNext.setText(R.string.begin_button);
            frameAnimation.start();
        }
    }
}
