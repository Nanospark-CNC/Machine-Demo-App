package com.nanospark.machinemonitordemo.ioio;

import android.util.SparseArray;

/**
 * A singleton that maintains the current board status.
 */
public class BoardStatus {

    private static BoardStatus sInstance;

    private boolean mIsBoardConnected;

    // Use a sparse array, just so we don't have to worry about 0 indexing with lists/arrays
    private SparseArray<Boolean> mBooleanInputArray = new SparseArray<>();
    private SparseArray<Float> mVoltageInputArray = new SparseArray<>();

    /**
     * Get the singleton instance of the BoardStatus class
     * @return the single instance of the BoardStatus class
     */
    public static BoardStatus getInstance() {
        if (sInstance == null)
        {
            sInstance = new BoardStatus();
        }
        return sInstance;
    }

    // Private constructor to prevent direct instantiation
    protected BoardStatus() {
    }

    /**
     * Set the IOIO board's current connection status
     * @param isConnected set to true if the board is currently connected
     */
    public void setBoardConnected(boolean isConnected) {
        mIsBoardConnected = isConnected;
    }

    /**
     * Checks the connection status of the IOIO board.
     * @return true if the board is currently connected
     */
    public boolean isBoardConnected() {
        return mIsBoardConnected;
    }

    /**
     * Set the digital input value of a specific input
     * @param inputNumber the input number to set
     * @param isOn set to true if the input is on
     * @param voltage the voltage of the pin (only relevant for analog inputs)
     */
    public void setInput(int inputNumber, Boolean isOn, float voltage) {
        mBooleanInputArray.put(inputNumber, isOn);
        mVoltageInputArray.put(inputNumber, voltage);
    }

    /**
     * Get the current state of all digital inputs
     * @return the state of all inputs in a sparse array (index with by the id of each input)
     */
    public SparseArray<Boolean> getAllInputStates() {
        return mBooleanInputArray;
    }

    /**
     * Get the current voltages of all inputs
     * @return the voltage of all inputs
     */
    public SparseArray<Float> getAllVoltages() {
        return mVoltageInputArray;
    }


    /**
     * Get the current state of a specific input
     * @return the state of the input
     */
    public boolean getInputState(int inputNumber) {
        Boolean b = mBooleanInputArray.get(inputNumber);

        if (b != null) {
            return mBooleanInputArray.get(inputNumber);
        }
        return false;
    }


    /**
     * Get the current voltage of a specific input
     * @return the voltage of the input
     */
    public float getInputVoltage(int inputNumber) {
        Float f = mVoltageInputArray.get(inputNumber);

        if (f != null) {
            return f;
        } else {
            return 0.0f;
        }
    }
}
