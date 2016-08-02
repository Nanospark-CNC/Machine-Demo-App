package com.nanospark.machinemonitordemo.ioio;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class BoardConnectionHiddenActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent keepAliveService = new Intent(this, BoardMonitorService.class);
        startService(keepAliveService);
        finish();
    }
}
