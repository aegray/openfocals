package com.openfocals.focals.events;

public class FocalsBatteryStateEvent {
    public Integer focals_battery;
    public Integer ring_battery;
    public boolean focals_charging;


    public FocalsBatteryStateEvent(
            Integer fb,
            Integer rb,
            boolean fc) {
        focals_battery = fb;
        ring_battery = rb;
        focals_charging = fc;
    }

    public FocalsBatteryStateEvent() {
        this(null, null, false);
    }
}
