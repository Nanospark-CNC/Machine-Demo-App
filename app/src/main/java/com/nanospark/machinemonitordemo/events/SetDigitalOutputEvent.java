package com.nanospark.machinemonitordemo.events;

/**
 * Event that is fired when we want to update a digital output pin value.
 */
public class SetDigitalOutputEvent {
    public int pinNumber;
    public boolean value;

    public SetDigitalOutputEvent(int pinNumber, boolean value) {
        this.pinNumber = pinNumber;
        this.value = value;
    }
}
