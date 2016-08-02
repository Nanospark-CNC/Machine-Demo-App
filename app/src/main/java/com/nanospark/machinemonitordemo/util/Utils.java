package com.nanospark.machinemonitordemo.util;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

/**
 * General purpose utils class for misc. helper functions
 */
public class Utils {

    /**
     * Helper method to display a Toast on the main UI thread
     *
     * @param context     the Context
     * @param message     the message to display
     * @param toastLength the length of time to display the toast for
     */
    public static void showToastOnMainThread(final Context context, final String message, final int toastLength) {
        Handler mainHandler = new Handler(context.getApplicationContext().getMainLooper());
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, toastLength).show();
            }
        };
        mainHandler.post(myRunnable);
    }

}
