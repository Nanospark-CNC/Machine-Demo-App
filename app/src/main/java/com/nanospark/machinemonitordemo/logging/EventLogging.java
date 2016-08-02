package com.nanospark.machinemonitordemo.logging;

import android.content.Context;
import android.util.Log;

/**
 * Class which logs key application events that will be visible to the user. Uses two log files such that the oldest log
 * file will be overwritten when we hit the maximum log file size.
 */
public class EventLogging {

	private static final String TAG = "MachineMonitorDemo";

	public static void LogEvent(Context context, String details) {
        Log.d(TAG, details);
	}
	
	
	public static void LogError(Context context, String details) {
        Log.e(TAG, details);
	}
}
