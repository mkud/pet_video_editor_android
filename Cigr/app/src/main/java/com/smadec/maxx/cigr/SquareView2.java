package com.smadec.maxx.cigr;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/**
 * @author Ishan Khanna
 */

public class SquareView2 extends View {

    private final int row;
    private final int col;
    public Paint paint;
    /**
     * Side of the square.
     */
    private int s;
    /**
     * Padding between two adjacent squares.
     */
    private int p;
    private float left, top, right, bottom;

    public SquareView2(Context context, int row, int col) {
        super(context);
        this.row = row;
        this.col = col;
        init();
    }

    private void init() {
        s = 40;
        p = 8;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);

        top = 17 + (row * s) + (row * p);
        left = (col * s) + (col * p);
        bottom = top + s;
        right = left + s;
        setPivotX(right);
        setPivotY(bottom);
    }

    public void Move() {
        setRotation(0);
        left += 40;
        right += 40;
        setPivotX(right);

        invalidate();

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(left, top, right, bottom, paint);
    }

}