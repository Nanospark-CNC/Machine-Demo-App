package com.nanospark.machinemonitordemo;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.VideoView;

import com.nanospark.machinemonitordemo.events.InputUpdateEvent;
import com.nanospark.machinemonitordemo.events.SetDigitalOutputEvent;
import com.nanospark.machinemonitordemo.ioio.BoardStatus;
import com.nanospark.machinemonitordemo.logging.EventLogging;
import com.nanospark.machinemonitordemo.ui.LubricantIndicator;
import com.nanospark.machinemonitordemo.util.LubricantHelper;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

public class MainActivity extends AppCompatActivity {

    private static final int VIDEO_MACHINE_RUNNING = 1;
    private static final int VIDEO_MACHINE_SHUDDER_STOP = 2;
    private static final int VIDEO_MACHINE_DOWN = 3;
    private static final int VIDEO_MACHINE_ON_HOLD = 4;

    private boolean wasOn43;
    private boolean wasOn44;
    private boolean wasOn45;

    @Bind(R.id.video_view)
    VideoView videoView;

    @Bind(R.id.lubricantIndicator)
    LubricantIndicator lubricantIndicator;

    @Bind(R.id.lubricant_label)
    TextView lubricantLabel;

    private BoardStatus boardStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        boardStatus = BoardStatus.getInstance();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    protected void onResume() {
        super.onResume();
        // At startup we display the on hold video by default
        playVideo(VIDEO_MACHINE_ON_HOLD);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
    }

    private void playVideo(int videoNumber) {
        String uriPath = "android.resource://com.nanospark.machinemonitordemo/raw/video" + videoNumber;
        Uri uri = Uri.parse(uriPath);

        // The shudder/stop video should only play once and then go to the on hold video and start looping
        if (videoNumber == VIDEO_MACHINE_SHUDDER_STOP) {
            videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    playVideo(VIDEO_MACHINE_DOWN);
                }
            });
            videoView.setOnPreparedListener(null);
        } else {

            // All other videos will just loop
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setLooping(true);
                }
            });
        }

        videoView.setVideoURI(uri);
        videoView.start();
    }

    /**
     * Callback that is invoked when something new is added to the log
     * @param event the event object that was posted
     */
    public void onEvent(InputUpdateEvent event) {

        EventLogging.LogEvent(this, "onEvent");

        boolean isOn43 = boardStatus.getInputState(43);
        boolean isOn44 = boardStatus.getInputState(44);
        boolean isOn45 = boardStatus.getInputState(45);
        final float voltage46 = boardStatus.getInputVoltage(46);

        EventLogging.LogEvent(this, "*** UPDATING INPUTS");
        EventLogging.LogEvent(this, "*** 43 = " + isOn43);
        EventLogging.LogEvent(this, "*** 44 = " + isOn44);
        EventLogging.LogEvent(this, "*** 45 = " + isOn45);
        EventLogging.LogEvent(this, "*** 46 = " + voltage46);


        // We only care about 43 - 45 when the value changes (We only allow one change to be detected on each cycle)
        if (isOn45 != wasOn45) {
            if (isOn45) {
                playVideo(VIDEO_MACHINE_RUNNING);
                setDigitalOutput(1, true);
            } else {
                setDigitalOutput(1, false);
            }
            wasOn45 = isOn45;
        } else if (isOn43 != wasOn43) {
            if (isOn43) {
                playVideo(VIDEO_MACHINE_SHUDDER_STOP); // Play once then loop 3
                setDigitalOutput(3, true);
            } else {
                setDigitalOutput(3, false);
            }
            wasOn43 = isOn43;
        } else if (isOn44 != wasOn44) {
            if (isOn44) {
                playVideo(VIDEO_MACHINE_ON_HOLD);
                setDigitalOutput(2, true);
            } else {
                setDigitalOutput(2, false);
            }
            wasOn44 = isOn44;
        }

        if (voltage46 < LubricantHelper.MEDIUM_THRESHOLD) {
            setDigitalOutput(4, true);
        } else {
            setDigitalOutput(4, false);
        }

        // Just update the data that we are displaying
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        lubricantIndicator.setCurrentValue(voltage46);
                        lubricantLabel.setTextColor(LubricantHelper.getInstance(MainActivity.this).getColor(voltage46));
                    }
                });
    }

    private void setDigitalOutput(int pinNumber, boolean isOn) {
        EventBus.getDefault().post(new SetDigitalOutputEvent(pinNumber, isOn));
    }
}
