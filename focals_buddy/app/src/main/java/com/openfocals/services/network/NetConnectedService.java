package com.openfocals.services.network;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;

import com.openfocals.focals.Device;
import com.openfocals.focals.events.FocalsConnectedEvent;
import com.openfocals.focals.events.FocalsConnectionFailedEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class NetConnectedService {

    Device device_;
    ConnectivityManager mgr_;
    boolean net_connected_ = false;


    class ConnectedUpdater extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent i) {
            NetConnectedService.this.updateConnectionStatus();
        }
    }


    public NetConnectedService(Context c, Device d) {
        device_ = d;
        mgr_ = (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);
        c.registerReceiver(new ConnectedUpdater(), new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));

        device_.getEventBus().register(this);
    }

    public void updateConnectionStatus() {
        NetworkInfo info = mgr_.getActiveNetworkInfo();
        if ((info != null) && (info.isConnected())) {
            net_connected_ = true;
            sendConnectionStatus();
        } else {
            net_connected_ = false;
            sendConnectionStatus();
        }
    }

    private void sendConnectionStatus() {
        if (device_.isConnected()) {
            device_.setNetworkEnabled(net_connected_);
        }
    }

    @Subscribe(threadMode= ThreadMode.MAIN)
    public void onFocalsConnected(FocalsConnectedEvent c) {
        sendConnectionStatus();
    }
}
