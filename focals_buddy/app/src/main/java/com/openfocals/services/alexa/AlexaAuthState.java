package com.openfocals.services.alexa;

import android.util.Log;

import com.openfocals.focals.Device;
import com.openfocals.focals.events.FocalsBluetoothMessageEvent;
import com.openfocals.focals.events.FocalsConnectedEvent;
import com.openfocals.focals.events.FocalsDisconnectedEvent;
import com.openfocals.focals.messages.AlexaAuthActionToBuddy;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class AlexaAuthState {
    private static final String ALEXA_NAME = "alexa";
    private static final String TAG = "FOCALS_ALEXA_STATE";

    Device device_;

    boolean device_authed_ = false;

    static AlexaAuthState instance_;

    public static AlexaAuthState getInstance() { return instance_; };

    public AlexaAuthState(Device d) {
        instance_ = this;
        device_ = d;
        device_.getEventBus().register(this);
    }

    public boolean isAuthenticated() { return device_authed_; }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsBluetoothMessage(FocalsBluetoothMessageEvent e) {
        if (e.message.hasAlexaAuth()) {
            AlexaAuthActionToBuddy r = e.message.getAlexaAuth();
            if (r.hasState()) {
                if (r.getState().getName().equals(ALEXA_NAME)) {
                    device_authed_ = r.getState().getAuthorized();
                    Log.i(TAG, "Alexa authorized: " + device_authed_);
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsDisconnected(FocalsDisconnectedEvent e) {
        device_authed_ = false;
    }
}
