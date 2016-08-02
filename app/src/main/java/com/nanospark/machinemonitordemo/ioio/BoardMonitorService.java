package com.nanospark.machinemonitordemo.ioio;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.nanospark.machinemonitordemo.MainActivity;
import com.nanospark.machinemonitordemo.R;
import com.nanospark.machinemonitordemo.events.InputUpdateEvent;
import com.nanospark.machinemonitordemo.events.SetDigitalOutputEvent;
import com.nanospark.machinemonitordemo.logging.EventLogging;
import com.nanospark.machinemonitordemo.util.Utils;

import de.greenrobot.event.EventBus;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;

/**
 * Foreground service that constantly monitors the IOIO boards digital connections.
 */
public class BoardMonitorService extends IOIOService {

    private static final int ONGOING_NOTIFICATION_ID = 1;

    private IOIOInput mInputDigital43;
    private IOIOInput mInputDigital44;
    private IOIOInput mInputDigital45;
    private IOIOInput mInputAnalog46;

    private DigitalOutput LED;

    private DigitalOutput mOutputDigital1;
    private DigitalOutput mOutputDigital2;
    private DigitalOutput mOutputDigital3;
    private DigitalOutput mOutputDigital4;

    private BoardStatus mBoardStatus;

    private IOIOInput mInputsArray[];
    private long mNextDesiredTime;

    @Override
    public void onCreate() {
        super.onCreate();
        EventLogging.LogEvent(this, "Board Monitor Service - OnCreate");
        mBoardStatus = BoardStatus.getInstance();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        EventLogging.LogEvent(this, "Board Monitor Service - OnDestroy");
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    /**
     * Event that is fired to set a particular output pin high
     * @param event the event that was posted
     */
    public void onEvent(SetDigitalOutputEvent event) {
        try {
            if (event.pinNumber >= 1 && event.pinNumber <= 3) {
                if (event.value) {
                    mOutputDigital1.write(event.pinNumber == 1 ? event.value : false);
                    mOutputDigital2.write(event.pinNumber == 2 ? event.value : false);
                    mOutputDigital3.write(event.pinNumber == 3 ? event.value : false);
                }
            } else if (event.pinNumber == 4) {
                mOutputDigital4.write(event.value);
            }
        } catch (ConnectionLostException ignored) {}
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        super.onStartCommand(intent, flags, startId);

        EventLogging.LogEvent(BoardMonitorService.this, "Starting Board Monitor Service");

        // We do not want Android to kill our service, so we create it as a foreground service which requires a persistent
        // notification so the user is aware of it.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.machine_monitor_active))
                .setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                .setOngoing(true);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setContentIntent(pendingIntent);

        startForeground(ONGOING_NOTIFICATION_ID, builder.build());

        return START_STICKY;
    }

    private void initialiseInputs(IOIO ioio) throws ConnectionLostException {

        EventLogging.LogEvent(this, "Initialising inputs");

        mInputDigital43 = new IOIOInput(ioio, 43, 0.0f);
        mInputDigital44 = new IOIOInput(ioio, 44, 0.0f);
        mInputDigital45 = new IOIOInput(ioio, 45, 0.0f);
        mInputAnalog46 = new IOIOInput(ioio, 46, 1.0f);

        mInputsArray = new IOIOInput[] {mInputDigital43, mInputDigital44, mInputDigital45, mInputAnalog46};
        long startTime = System.currentTimeMillis();

        // We use this timestamp to keep the overall sampling at precise boundaries (or as close to precise as is possible)
        mNextDesiredTime = startTime;
    }

    @Override
    protected IOIOLooper createIOIOLooper() {
        return new Looper();
    }

    class Looper extends BaseIOIOLooper {

        private boolean digitalInputStates[] ;
        private float anlalogInputVoltage[];

        @Override
        public void setup() throws ConnectionLostException {

            EventLogging.LogEvent(BoardMonitorService.this, "IOIO Looper setup");

            LED = ioio_.openDigitalOutput(0, true);

            mOutputDigital1 = ioio_.openDigitalOutput(1, false);
            mOutputDigital2 = ioio_.openDigitalOutput(2, false);
            mOutputDigital3 = ioio_.openDigitalOutput(3, false);
            mOutputDigital4 = ioio_.openDigitalOutput(4, false);

            initialiseInputs(ioio_);

            digitalInputStates = new boolean[mInputsArray.length];
            anlalogInputVoltage = new float[mInputsArray.length];

            // Read the digital inputs into our local array to initialise them.
            // On the first read we do not send any system wide notifications that the values have updated
            try {
                for (int i=0; i<mInputsArray.length; i++) {
                    updateInput(i+1, mInputsArray[i].readIsOn(), mInputsArray[i].getCurrentVoltage(), false);
                }

            } catch (InterruptedException e) {}

            mBoardStatus.setBoardConnected(true);

            EventLogging.LogEvent(BoardMonitorService.this, "Board Connected");
            Utils.showToastOnMainThread(BoardMonitorService.this, getString(R.string.board_connected), Toast.LENGTH_SHORT);
        }

        @Override
        public void loop() throws ConnectionLostException {

            long timeToSleep;

            try {

                for (int inputIndex = 0; inputIndex < mInputsArray.length; inputIndex++) {
                    digitalInputStates[inputIndex] = false;
                    anlalogInputVoltage[inputIndex] = 0.0f;
                }

                // On each iteration we sample 10 times
                for (int i=0; i < 10; i++) {

                    for (int inputIndex = 0; inputIndex < mInputsArray.length; inputIndex++) {
                        IOIOInput input = mInputsArray[inputIndex];
                        digitalInputStates[inputIndex] = digitalInputStates[inputIndex] | input.readIsOn();
                        anlalogInputVoltage[inputIndex] = Math.max(anlalogInputVoltage[inputIndex], input.getCurrentVoltage());
                    }

                    // For the first iteration we sleep with the light on, so this gives us a brief blinking effect
                    if (i == 0) {
                        LED.write(false);
                    } else if (i == 1) {
                        LED.write(true);
                    }

                    mNextDesiredTime += 100;
                    timeToSleep = mNextDesiredTime - System.currentTimeMillis();
                    if (timeToSleep > 0) {
                        Thread.sleep(timeToSleep);
                    }
                }

                // Read the inputs into our local array
                for (int i=0; i < mInputsArray.length; i++) {
                    updateInput(i + 43, digitalInputStates[i], anlalogInputVoltage[i], true);
                }

                EventLogging.LogEvent(BoardMonitorService.this, "READ INPUTS");

                // Broadcast an event every time around
                EventBus.getDefault().post(new InputUpdateEvent());

            } catch (InterruptedException e) {}
        }

        @Override
        public void disconnected() {
            super.disconnected();
            mBoardStatus.setBoardConnected(false);

            EventLogging.LogEvent(BoardMonitorService.this, "Board Disconnected");
            Utils.showToastOnMainThread(BoardMonitorService.this, getString(R.string.board_disconnected), Toast.LENGTH_SHORT);

            // Kill this service so we no longer keep running
            stopSelf();
        }
    }

    /**
     * Set the input value of a specific digital input
     * @param inputNumber the input number to set
     * @param isOn set to true if the input is on
     * @param voltage the current voltage of the input (only relevant for analog pins)
     */
    private void updateInput(int inputNumber, Boolean isOn, float voltage, boolean notifyChange) {
        mBoardStatus.setInput(inputNumber, isOn, voltage);
    }
}
