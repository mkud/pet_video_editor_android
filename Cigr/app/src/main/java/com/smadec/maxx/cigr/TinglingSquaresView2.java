package com.smadec.maxx.cigr;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class TinglingSquaresView2 extends FrameLayout {

    private static final long ANIMATION_RESTART_DELAY = 1500;
    private static int ANIMATION_TIME_BASE = 500;
    private static float LAG_FACTOR = 0.5f;
    private final SquareView2[][] squareViews = new SquareView2[3][4];
    private final ObjectAnimator[] animatorsLeftToRight = new ObjectAnimator[12];
    private final AnimatorSet scene2 = new AnimatorSet();
    private Context ctx;
    private int side = 40;
    private int padding = 8;
    private AnimatorSet column1AnimationLTR, column2AnimationLTR, column3AnimationLTR, column4AnimationLTR;
    private Runnable animationRunnableLeftToRight = new Runnable() {
        @Override
        public void run() {
            scene2.start();
        }
    };

    public TinglingSquaresView2(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.ctx = context;
        init();
    }

    public TinglingSquaresView2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.ctx = context;
        init();
    }

    public static void setAnimationTimeBase(int animationTimeBase) {
        ANIMATION_TIME_BASE = animationTimeBase;
    }

    public static void setLagFactor(float lagFactor) {
        LAG_FACTOR = lagFactor;
    }

    public static long getStartDelayForColumn(int columnNumber) {
        columnNumber = 4 - columnNumber;
        return (long) (ANIMATION_TIME_BASE * 0.3f * columnNumber);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(widthMeasureSpec, (side * 3) + (padding * 2) + 17);
    }

    public void init() {
        for (int m = 0; m < 3; m++) {
            for (int n = 0; n < 4; n++) {
                squareViews[m][n] = new SquareView2(ctx, m, n);
            }
        }

        initAnimations();
    }

    public void initAnimations() {

        setupLeftToRightAnimators();

        scene2.playTogether(column4AnimationLTR, column2AnimationLTR, column3AnimationLTR, column1AnimationLTR);
    }

    private void setupLeftToRightAnimators() {
        PropertyValuesHolder pvhLeftToRightAnimation = PropertyValuesHolder.ofFloat(View
                .ROTATION, 0, 90);

        int m = 0, n = 0;

        for (int i = 0; i < 12; i++) {
            animatorsLeftToRight[i] = ObjectAnimator.ofPropertyValuesHolder(squareViews[m][n],
                    pvhLeftToRightAnimation);
            switch (m) {
                case 0:
                    animatorsLeftToRight[i].setDuration(ANIMATION_TIME_BASE);
                    break;
                case 1:
                    animatorsLeftToRight[i].setDuration((int) (ANIMATION_TIME_BASE * LAG_FACTOR));
                    break;
                case 2:
                    animatorsLeftToRight[i].setDuration(ANIMATION_TIME_BASE);
                    break;
            }
            n++;
            if (n > 3) {
                m++;
                n = 0;
            }
        }

        column4AnimationLTR = new AnimatorSet();
        column4AnimationLTR.playTogether(animatorsLeftToRight[3], animatorsLeftToRight[7], animatorsLeftToRight[11]);

        column3AnimationLTR = new AnimatorSet();
        column3AnimationLTR.playTogether(animatorsLeftToRight[2], animatorsLeftToRight[6], animatorsLeftToRight[10]);
        column3AnimationLTR.setStartDelay(getStartDelayForColumn(3));

        column2AnimationLTR = new AnimatorSet();
        column2AnimationLTR.playTogether(animatorsLeftToRight[1], animatorsLeftToRight[5], animatorsLeftToRight[9]);
        column2AnimationLTR.setStartDelay(getStartDelayForColumn(2));

        column1AnimationLTR = new AnimatorSet();
        column1AnimationLTR.playTogether(animatorsLeftToRight[0], animatorsLeftToRight[4], animatorsLeftToRight[8]);
        column1AnimationLTR.setStartDelay(getStartDelayForColumn(1));
        column1AnimationLTR.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                runAnimation(ANIMATION_RESTART_DELAY);
            }
        });

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        for (int m = 0; m < 3; m++) {
            for (int n = 0; n < 4; n++) {
                addView(squareViews[m][n]);
            }
        }
    }

    public void StartAnimation() {
        scene2.start();
    }

    public void runAnimation(long delay) {
        for (int m = 0, i = 0; m < 3; m++) {
            for (int n = 0; n < 4; n++, i++) {
                squareViews[m][n].Move();
            }
        }
        postDelayed(animationRunnableLeftToRight, delay);
    }
}