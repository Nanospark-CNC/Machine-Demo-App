package com.nanospark.machinemonitordemo.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.nanospark.machinemonitordemo.util.LubricantHelper;

/**
 * Created by jamie on 02/08/2016.
 */
public class LubricantIndicator extends View {

    private static final float MAX_VALUE = 3.3f;
    private static final float MIN_VALUE = 0.0f;

    private Paint paint;

    private float currentValue = 2.0f;
    private LubricantHelper lubricantHelper;

    public LubricantIndicator(Context context) {
        super(context);
        init(context);
    }

    public LubricantIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LubricantIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        paint = new Paint();
        lubricantHelper = LubricantHelper.getInstance(context);
    }

    public void setCurrentValue(float currentValue) {
        this.currentValue = currentValue;
        invalidate();
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(lubricantHelper.getColor(currentValue));
        float height = canvas.getHeight() - (canvas.getHeight() * (currentValue / (MAX_VALUE - MIN_VALUE)));
        canvas.drawRect(0, height, canvas.getWidth(), canvas.getHeight(), paint);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);
    }
}
