package com.nanospark.machinemonitordemo.util;

import android.content.Context;
import android.support.v4.content.ContextCompat;

import com.nanospark.machinemonitordemo.R;

/**
 * Helper class related to displaying the lubricant level
 */
public class LubricantHelper {

    public static final float HIGH_THRESHOLD = 1.5f;
    public static final float MEDIUM_THRESHOLD = 0.5f;

    private static LubricantHelper sInstance;

    public static LubricantHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new LubricantHelper(context);
        }
        return sInstance;
    }

    private Context context;
    private int colorHigh;
    private int colorMedium;
    private int colorLow;

    private LubricantHelper(Context context) {
        this.context = context;
        colorHigh = ContextCompat.getColor(context, R.color.lubricant_high);
        colorMedium = ContextCompat.getColor(context, R.color.lubricant_medium);
        colorLow = ContextCompat.getColor(context, R.color.lubricant_low);
    }

    public int getColor(float currentValue) {
        if (currentValue >= HIGH_THRESHOLD) {
            return colorHigh;
        } else if (currentValue >= MEDIUM_THRESHOLD) {
            return colorMedium;
        }
        return colorLow;
    }
}
