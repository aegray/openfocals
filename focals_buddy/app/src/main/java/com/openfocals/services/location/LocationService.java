package com.openfocals.services.location;

import android.util.Log;

import com.openfocals.focals.Device;
import com.openfocals.focals.events.FocalsConnectedEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class LocationService {

    private static final String TAG = "FOCALS_LOCATION";

    Device device_;

    public LocationService(Device d) {
        device_ = d;
        device_.getEventBus().register(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsConnected(FocalsConnectedEvent e) {
        device_.sendLocation(41.884, -87.6528);
    }

}
