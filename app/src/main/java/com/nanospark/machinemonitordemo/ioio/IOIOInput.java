package com.nanospark.machinemonitordemo.ioio;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;

/**
 * A wrapper class that enables an input to be treated as either an analog or digital input;
 */
public class IOIOInput {

    protected IOIO mIOIO;
    private boolean mIsDigital;
    private int mInputType;
    private DigitalInput mDigitalInput;
    private AnalogInput mAnalogInput;
    private int mNPinNumber;
    private double mVoltageThreshold;
    private float mCurrentVoltage;

    /**
     * Construct an IOIOInput with no voltage value (a digital input).
     * @param ioio the ioio object
     * @param pinNumber the pin number that this input relates to
     **/
    public IOIOInput(IOIO ioio, int pinNumber) throws ConnectionLostException {
        this(ioio, pinNumber, 0.0);
    }

    /**
     * Construct an IOIOInput with a voltage threshold value (typically an anlog input).
     * @param ioio the ioio object
     * @param pinNumber the pin number that this input relates to
     * @param voltageThreshold the voltage threshold for the changeover between on/off (0.0 indicates that this should be a digital input
     **/
    public IOIOInput(IOIO ioio, int pinNumber, double voltageThreshold) throws ConnectionLostException {
        mIOIO = ioio;
        mNPinNumber = pinNumber;

        if (voltageThreshold > 0) {
            mAnalogInput = mIOIO.openAnalogInput(pinNumber);
            mVoltageThreshold = voltageThreshold;
            mIsDigital = false;
        } else {
            mDigitalInput = mIOIO.openDigitalInput(pinNumber, DigitalInput.Spec.Mode.PULL_DOWN);
            mIsDigital = true;
        }
    }

    /**
     * Get the pin number associated with this input.
     * @return the pin number of this input
     */
    public int getPinNumber() {
        return mNPinNumber;
    }

    /**
     * Check if the current pin is in the on state
     * @return true if the pin is on or false if off
     */
    public boolean readIsOn() throws InterruptedException, ConnectionLostException {
        if (mIsDigital) {
            return mDigitalInput.read();
        } else {
            mCurrentVoltage = mAnalogInput.getVoltage();
            return (mCurrentVoltage > mVoltageThreshold);
        }
    }

    /**
     * Get the current voltage of this pin (only relevant for analog values)
     * @return the voltage value as a float
     */
    public float getCurrentVoltage() {
        return mCurrentVoltage;
    }

    /**
     * Close the input so it may be re-used again.
     */
    public void close() {
        if (mIsDigital) {
            mDigitalInput.close();
        } else {
            mAnalogInput.close();
        }
    }
}
