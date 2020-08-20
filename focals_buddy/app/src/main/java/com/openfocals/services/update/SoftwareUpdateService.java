package com.openfocals.services.update;

import android.util.Log;

import com.openfocals.focals.Device;
import com.openfocals.focals.events.FocalsBluetoothMessageEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class SoftwareUpdateService {

    private static final String TAG = "FOCALS_UPDATE";

    Device device_;

    public SoftwareUpdateService(Device d) {
        device_ = d;
        device_.getEventBus().register(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsMessage(FocalsBluetoothMessageEvent e) {
        if (e.message.hasSoftwareUpdate()) {
            Log.i(TAG, "Got software update message: " + e.message);
        }
    }
}
