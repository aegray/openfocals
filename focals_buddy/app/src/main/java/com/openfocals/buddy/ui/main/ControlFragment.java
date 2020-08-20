package com.openfocals.buddy.ui.main;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Network;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.provider.Settings;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.openfocals.buddy.ConnectActivity;
import com.openfocals.buddy.FocalsBuddyApplication;
import com.openfocals.buddy.R;
import com.openfocals.buddy.ui.calibrate.CalibrateFragment;
import com.openfocals.focals.Device;
import com.openfocals.focals.events.FocalsBatteryStateEvent;
import com.openfocals.focals.events.FocalsBluetoothMessageEvent;
import com.openfocals.focals.events.FocalsConnectedEvent;
import com.openfocals.focals.events.FocalsConnectionFailedEvent;
import com.openfocals.focals.events.FocalsDisconnectedEvent;
import com.openfocals.focals.messages.LoopConnectionState;
import com.openfocals.services.network.NetworkService;
import com.openfocals.services.notifications.NotificationService;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class ControlFragment extends Fragment {
    private static final String TAG = "FOCALS_UI_MAIN";
    static final int REQUEST_DISCOVERY = 2;
    static final int LIMIT_CONNECT_FAILS_BEFORE_DISCOVERY = 5;
    static final int REQUEST_SETUP_NOTIF_PERMS = 3;


    TextView text_connected_;
    TextView text_device_;
    TextView text_battery_;
    TextView text_network_;
    TextView text_loop_;

    TextView text_permissions_;


    List<Button> conditional_buttons_ = new ArrayList<>();

    Button button_connect_;

    Device device_;

    int connect_failure_count_ = 0;

    Handler stats_handler_ = new Handler();


    private final BroadcastReceiver bt_state_receiver = new BroadcastReceiver() {
        public void onReceive (Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                handleConnectedChange();
            }
        }
    };

    public static ControlFragment newInstance() {
        return new ControlFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.control_fragment, container, false);
    }

    private void refreshPermsState() {
        final ComponentName cn = new ComponentName(getActivity(), NotificationService.class);
        String flat = Settings.Secure.getString(getActivity().getContentResolver(), "enabled_notification_listeners");
        final boolean enabled = flat != null && flat.contains(cn.flattenToString());
        text_permissions_.setVisibility(enabled ? View.INVISIBLE : View.VISIBLE);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        device_ = ((FocalsBuddyApplication)getActivity().getApplication()).device;

        //want_connection_ = device_.isConnected();

        text_connected_ = getView().findViewById(R.id.textConnected);
        text_device_ = getView().findViewById(R.id.textDevice);
        text_battery_ = getView().findViewById(R.id.textBattery);
        text_loop_ = getView().findViewById(R.id.textLoop);
        text_network_ = getView().findViewById(R.id.textNetworkStats);
        text_permissions_ = getView().findViewById(R.id.textPermissionNotification);


        text_loop_.setClickable(true);
        text_loop_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (device_.getLoopState() != LoopConnectionState.State.CONNECTED) {
                    device_.startLoopPairing();
                }
            }
        });

        String permstring = new String("openfocals does not have permissions to listen to notifications.  Enable them here");
        SpannableString content = new SpannableString(permstring);
        content.setSpan(new UnderlineSpan(), permstring.length()-16, permstring.length(), 0);
        text_permissions_.setText(content);

        final ComponentName cn = new ComponentName(getActivity(), NotificationService.class);
        String flat = Settings.Secure.getString(getActivity().getContentResolver(), "enabled_notification_listeners");
        final boolean enabled = flat != null && flat.contains(cn.flattenToString());

        text_permissions_.setClickable(true);
        text_permissions_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                startActivityForResult(intent, REQUEST_SETUP_NOTIF_PERMS);
                refreshPermsState();
            }
        });

        refreshPermsState();



        if (device_.getTargetMac() == null) {
            text_device_.setText("Device: none");
        } else {
            text_device_.setText("Device: " + device_.getTargetName() + " : " + device_.getTargetMac());
        }


        button_connect_ = getView().findViewById(R.id.buttonConnect);
        button_connect_.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (!isBluetoothEnabled()) {
                    BluetoothAdapter b = BluetoothAdapter.getDefaultAdapter();
                    if (b != null) {
                        b.enable();
                    }
                } else if (!device_.wantConnection()) {
                    launchConnectActivity();
                } else {
                    device_.stop();
                    handleConnectedChange();
                }
            }
        }));


        handleConnectedChange();
    }

    @Override
    public void onActivityResult(int request, int result, Intent intent) {
        if (request == REQUEST_DISCOVERY) {
            Log.i(TAG, "Got discovery result: " + result + " : " + RESULT_OK + " / " + RESULT_CANCELED);
            handleConnectedChange();
        } else if (request == REQUEST_SETUP_NOTIF_PERMS) {
            refreshPermsState();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        device_.getEventBus().register(this);
        connect_failure_count_ = 0;
        getActivity().registerReceiver(bt_state_receiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        Log.i(TAG, "StartupActivity::onStart");

        stats_handler_.postDelayed(stats_updater, 500);
    }


    Runnable stats_updater = new Runnable() {

        private String make_size(int bytes) {
            String postfix = "";
            if (bytes >= 1000) {
                bytes /= 1000;
                postfix = "k";
            }
            if (bytes >= 1000) {
                bytes /= 1000;
                postfix = "m";
            }
            return "" + bytes + postfix;
        }
        @Override
        public void run() {
            if (device_.isConnected()) {
                NetworkService n = NetworkService.getInstance();

                text_network_.setText("Network sent/recv=" + make_size(n.getBytesSent())
                        + "/" + make_size(n.getBytesRecv())
                        + " open=" + n.getOpenConnections());
            }
            stats_handler_.postDelayed(stats_updater, 500);
        }
    };


    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "StartupActivity::onPause");
    }


    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "StartupActivity::onStop");
        getActivity().unregisterReceiver(bt_state_receiver);
        device_.getEventBus().unregister(this);
    }


    private void updateBatteryState() {
        FocalsBatteryStateEvent bat = device_.getLastBatteryState();
        text_battery_.setText("Battery: " +
                (bat.focals_battery != null ? bat.focals_battery : "") +
                (((bat.focals_battery != null) && bat.focals_charging) ? " (charging) " : ""));

        if (device_.getLoopState() == LoopConnectionState.State.NOT_CONNECTED) {

            if (!device_.isConnected()) {
                String loop = new String("Loop: (not connected)");
                text_loop_.setText(loop);
            } else {
                String loop = new String("Loop: (not connected)<br><u><b>Click here to connect loop</b></u>");
                text_loop_.setText(Html.fromHtml(loop));
            }
        } else if (device_.getLoopState() == LoopConnectionState.State.CONNECTING) {
            text_loop_.setText("Loop: (connecting)");
        } else {
            text_loop_.setText("Loop: " +
                    (bat.ring_battery != null ? bat.ring_battery : ""));
        }
    }

    private void updateDevice() {
        if (device_.getTargetMac() == null) {
            text_device_.setText("Device: none");
        } else {
            text_device_.setText("Device: " + device_.getTargetName() + " : " + device_.getTargetMac());
        }
    }

    private boolean isBluetoothEnabled() {
        BluetoothAdapter b = BluetoothAdapter.getDefaultAdapter();
        return (b != null) && (b.isEnabled());
    }

    private void handleConnectedChange() {
        updateBatteryState();
        updateDevice();

        if (device_.isConnected()) {

            button_connect_.setVisibility(View.INVISIBLE);
            button_connect_.setText("Disconnect");
            text_connected_.setText("Connected! :)");
            button_connect_.setEnabled(true);

            for (Button b : conditional_buttons_) {
                b.setEnabled(true);
            }

        } else {
            button_connect_.setVisibility(View.VISIBLE);
            for (Button b : conditional_buttons_) {
                b.setEnabled(false);
            }

            if (!isBluetoothEnabled()) {
                text_connected_.setText("Bluetooth is disabled.  Reenable to allow connecting");
                button_connect_.setEnabled(true);
                button_connect_.setText("Enable");
            } else {
                button_connect_.setEnabled(true);
                if (device_.wantConnection()) {
                    button_connect_.setText("Stop reconnect");
                    text_connected_.setText("Dropped connection...Reconnecting...");
                } else {
                    button_connect_.setText("Connect");
                    text_connected_.setText("Not connected");
                }
            }
        }
    }

    private void launchConnectActivity() {
        Intent i = new Intent(getContext(), ConnectActivity.class);
        startActivityForResult(i, REQUEST_DISCOVERY);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsConnected(FocalsConnectedEvent e) {
        Log.i(TAG, "StartupActivity::onFocalsConnected");
        handleConnectedChange();

        // start periodic stats updater
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsBatteryState(FocalsBatteryStateEvent e) {
        Log.i(TAG, "Got battery state");
        updateBatteryState();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsMessage(FocalsBluetoothMessageEvent e) {
        handleConnectedChange();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsConnectionFailed(FocalsConnectionFailedEvent e) {
        Log.i(TAG, "Got connection failed");
        handleConnectedChange();
        connect_failure_count_ += 1;

        if (!device_.isConnected() && device_.isConnecting() &&
                (connect_failure_count_ > LIMIT_CONNECT_FAILS_BEFORE_DISCOVERY)) {
            connect_failure_count_ = 0;
            launchConnectActivity();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsDisconnected(FocalsDisconnectedEvent e) {
        Log.i(TAG, "Got disconnected");
        handleConnectedChange();
        text_network_.setText("");
    }
}
