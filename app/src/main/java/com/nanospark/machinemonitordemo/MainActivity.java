package com.nanospark.machinemonitordemo;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.nanospark.machinemonitordemo.events.InputUpdateEvent;
import com.nanospark.machinemonitordemo.events.SetDigitalOutputEvent;
import com.nanospark.machinemonitordemo.ioio.BoardStatus;
import com.nanospark.machinemonitordemo.logging.EventLogging;
import com.nanospark.machinemonitordemo.ui.LubricantIndicator;
import com.nanospark.machinemonitordemo.util.LubricantHelper;

import java.io.File;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

public class MainActivity extends AppCompatActivity {

    private boolean wasOn43;
    private boolean wasOn44;
    private boolean wasOn45;

    @Bind(R.id.video_view)
    VideoView videoView;

    @Bind(R.id.lubricantIndicator)
    LubricantIndicator lubricantIndicator;

    @Bind(R.id.lubricant_label)
    TextView lubricantLabel;

    @Bind(R.id.background)
    ViewGroup background;

    private BoardStatus boardStatus;
    private boolean videoStarted;

    private Runnable lowLubricantRunnable;
    private Handler delayedHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        boardStatus = BoardStatus.getInstance();
        background.setBackgroundResource(R.color.amber);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    protected void onResume() {
        super.onResume();

        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        String fileName = "video1.mp4";

        File file = new File(path, fileName);

        if (file.exists()) {
            videoView.setVideoPath(file.getPath());

            //Video Loop
            videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    videoView.start(); //need to make transition seamless.
                }
            });

            videoView.start();
        } else {
            Toast.makeText(this, getString(R.string.cannot_find_video) + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }

        // Pause the video after a small delay so we give chance for the video to appear
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                pauseVideo();
            }
        }, 500);
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

    private void playVideo() {
        videoView.start();
    }

    private void pauseVideo() {
        videoView.pause();
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

        int newBackgroundColor = 0;

        // We only care about 43 - 45 when the value changes (We only allow one change to be detected on each cycle)
        if (isOn45 != wasOn45) {
            if (isOn45) {
                playVideo();
                setDigitalOutput(1, true);
                newBackgroundColor = ContextCompat.getColor(this, R.color.green);
            }
            wasOn45 = isOn45;
        } else if (isOn43 != wasOn43) {
            if (isOn43) {
                pauseVideo();
                setDigitalOutput(3, true);
                newBackgroundColor = ContextCompat.getColor(this, R.color.red);
            }
            wasOn43 = isOn43;
        } else if (isOn44 != wasOn44) {
            if (isOn44) {
                pauseVideo();
                setDigitalOutput(2, true);
                newBackgroundColor = ContextCompat.getColor(this, R.color.amber);
            }
            wasOn44 = isOn44;
        }

        final boolean setLubricantColor;
        if (voltage46 < LubricantHelper.MEDIUM_THRESHOLD) {
            setLubricantColor = false;
            if (lowLubricantRunnable == null) {

                // Make sure the lubricant is not green when we are below the threshold and waiting for the delay
                runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                lubricantIndicator.setColor(ContextCompat.getColor(MainActivity.this, R.color.amber));
                                lubricantLabel.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.amber));
                            }
                        });
                lowLubricantRunnable = new Runnable() {
                    @Override
                    public void run() {
                        setDigitalOutput(4, true);
                        lubricantIndicator.setColor(ContextCompat.getColor(MainActivity.this, R.color.red));
                        lubricantLabel.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.red));
                    }
                };
                delayedHandler.postDelayed(lowLubricantRunnable, 30000);
            }
        } else {
            setLubricantColor = true;
            setDigitalOutput(4, false);
            if (lowLubricantRunnable != null) {
                delayedHandler.removeCallbacks(lowLubricantRunnable);
                lowLubricantRunnable = null;
            }
        }

        final int bgColor = newBackgroundColor;

        // Just update the data that we are displaying
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        lubricantIndicator.setCurrentValue(voltage46);
                        if (setLubricantColor) {
                            lubricantIndicator.setColor(LubricantHelper.getInstance(MainActivity.this).getColor(voltage46));
                            lubricantLabel.setTextColor(LubricantHelper.getInstance(MainActivity.this).getColor(voltage46));
                        }
                        if (bgColor != 0) {
                            background.setBackgroundColor(bgColor);
                        }
                    }
                });
    }

    private void setDigitalOutput(int pinNumber, boolean isOn) {
        EventBus.getDefault().post(new SetDigitalOutputEvent(pinNumber, isOn));
    }
}
