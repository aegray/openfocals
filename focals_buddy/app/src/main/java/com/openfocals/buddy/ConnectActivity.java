package com.openfocals.buddy;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.openfocals.focals.Device;
import com.openfocals.focals.events.FocalsConnectedEvent;
import com.openfocals.focals.events.FocalsConnectionFailedEvent;
import com.openfocals.focals.events.FocalsDisconnectedEvent;
import com.openfocals.focals.messages.LoopConnectionState;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

public class ConnectActivity extends AppCompatActivity {

    public static final String TAG = "FOCALS_DISCOVER";
    private static final int PERMISSION_REQUEST_CODE = 3;
    private static final int INTERVAL_SIZE_MS = 1000;
    private static final int MIN_INTERVALS_POTENTIAL = 3;
    private static final int LIMIT_INTERVALS_NO_DEVICES = 10;
    private static final int LIMIT_CONNECT_FAILURES = 5;

    // this should really be in a utility class but I'm whipping together here to do it fast
    // @TODO: cleanup
    BluetoothAdapter adapter_;

    Device device_;

    TreeMap<Integer, BluetoothDevice> potentials_ = new TreeMap<>();
    Set<String> potential_names_ = new HashSet<>();

    Handler handler_ = new Handler();
    Handler handler_label_ = new Handler();

    TextView text_;
    TextView text_help_;


    boolean discovering_ = false;
    boolean got_permissions_ = false;
    boolean connecting_ = false;

    int connect_failure_count_ = 0;


    String label_base_string_ = "Searching";
    int n_found_last_interval_ = 0;
    int n_intervals_with_nothing_ = 0;
    int n_intervals_total_ = 0;
    int n_sub_intervals_total_ = 0;


    private final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice dev, int rssi, byte[] data) {
            Log.i(TAG, "Discovery found device: " + dev.getName() + " : " + dev.getAddress());
            String devname = dev.getName();
            n_found_last_interval_ += 1;
            if ((devname != null) && (devname.contains("Focals"))) {
                potentials_.put(Integer.valueOf(rssi), dev);
                potential_names_.add(dev.getName());
                Log.i(TAG, "Discovery got potential: " + dev.getName() + " : " + dev.getAddress());
            }
        }
    };


    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String devname = dev.getName();
                String address = dev.getAddress();
                Log.i(TAG, "Discovery found device: " + dev.getName() + " : " + dev.getAddress());
                if (devname.contains("FOCALS")) {
                    int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);
                    potentials_.put(Integer.valueOf(rssi), dev);
                    Log.i(TAG, "Discovery got potential: " + dev.getName() + " : " + dev.getAddress());
                }
            }
        }
    };


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        if(requestCode == PERMISSION_REQUEST_CODE)
        {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Log.i(TAG, "coarse location permission granted");
                got_permissions_ = true;
                startDiscovery();
            }
            else
            {
                // @TODO: handle lack of permissions
                Log.e(TAG, "coarse location permission denied");
                got_permissions_ = false;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        device_ = ((FocalsBuddyApplication)getApplication()).device;
        device_.getEventBus().register(this);


        if (device_.isConnected()) {
            setResult(RESULT_CANCELED);
            finish();
        }

        adapter_ = BluetoothAdapter.getDefaultAdapter();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
            }, PERMISSION_REQUEST_CODE);
        }

        Button cxlbutton = findViewById(R.id.buttonCancelDiscovery);
        cxlbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopDiscovery();
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        text_ = findViewById(R.id.textDiscoveryStatus);
        text_help_ = findViewById(R.id.textInfoConnectTrouble);
        text_help_.setVisibility(View.INVISIBLE);
    }

    private final Runnable timeout_handler = new Runnable() {
        @Override
        public void run() {
            if (!discovering_) return;
            n_intervals_total_ += 1;

            if (potentials_.isEmpty()) {

                if (n_found_last_interval_ == 0) {
                    n_intervals_with_nothing_ += 1;
                }
                n_found_last_interval_ = 0;

                if (n_intervals_with_nothing_ > LIMIT_INTERVALS_NO_DEVICES) {
                    // attempted hack - android does some stuff to prevent us from scanning too often
                    // if we end up in a state where we're not seeing anything, rekick off
                    adapter_.stopLeScan(leScanCallback);
                    adapter_.startLeScan(leScanCallback);
                    n_intervals_with_nothing_ = 0;
                }

                Log.i(TAG, "No results");
                handler_.postDelayed(this, INTERVAL_SIZE_MS);
            } else {
                if (n_intervals_total_ >= MIN_INTERVALS_POTENTIAL) {
                    stopDiscovery();

                    BluetoothDevice d = potentials_.lastEntry().getValue();
                    Integer s =  potentials_.lastEntry().getKey();

                    Log.i(TAG, "Got final device: " + s + " : " + d.getName() + " : " + d.getAddress());

                    device_.setTarget(d.getName(), d.getAddress());
                    startConnecting();
                } else {
                    handler_.postDelayed(this, INTERVAL_SIZE_MS);
                }
            }
        }
    };


    private final Runnable label_handler = new Runnable() {
        @Override
        public void run() {
            if (!discovering_ && !connecting_) return;
            n_sub_intervals_total_ += 1;

            String label = "";

            if (discovering_) {
                int n_found = potential_names_.size();
                if (n_found > 0) {
                    label = "Found " + n_found + ". ";
                }
            }

            label += label_base_string_;
            //label += "Searching";
            for (int i = 0; i < (n_sub_intervals_total_ % 3) + 1; ++i) {
                label += ".";
            }

            text_.setText(label);
            handler_label_.postDelayed(label_handler, INTERVAL_SIZE_MS) ;
        }
    };

    private void stopDiscovery() {
        discovering_ = false;
        adapter_.stopLeScan(leScanCallback);
    }

    private void startDiscovery() {
        // stop the last scan if one is going on
        if (got_permissions_) {
            stopDiscovery();
            if (!adapter_.startLeScan(leScanCallback)) {
                Log.e(TAG, "Failed to start LE scan");

            } else {
                Log.i(TAG, "Started LE scan");
                discovering_ = true;
                n_found_last_interval_ = 0;
                n_intervals_with_nothing_ = 0;
                n_intervals_total_ = 0;
                n_sub_intervals_total_ = 0;
                label_base_string_ = "Searching";
                text_.setText(label_base_string_);
                handler_.postDelayed(timeout_handler, INTERVAL_SIZE_MS) ;
                handler_label_.postDelayed(label_handler, INTERVAL_SIZE_MS) ;
            }
        }
    }


    private void startProcess() {
        if (device_.shouldDiscover()) {
            startDiscovery();
        } else {
            startConnecting();
        }
    }

    private void stopProcess() {
        if (discovering_)
            stopDiscovery();

        if (connecting_)
            stopConnecting();
    }

    private void startConnecting() {
        connect_failure_count_ = 0;
        discovering_ = false;
        connecting_ = true;
        device_.start();
        label_base_string_ = "Connecting";
        text_.setText(label_base_string_);
        //"Connecting...");
    }

    private void stopConnecting() {
        discovering_ = false;
        connecting_ = false;
        device_.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startProcess();
    }


    @Override
    protected void onPause() {
        super.onPause();
        stopDiscovery();
    }

    @Override
    protected void onStop() {
        super.onStop();
        device_.getEventBus().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsConnected(FocalsConnectedEvent e) {
        Log.i(TAG, "ConnectActivity::onFocalsConnected");

        if (device_.getLoopState() != LoopConnectionState.State.CONNECTED) {
            device_.startLoopPairing();
        }

        setResult(RESULT_CANCELED);
        finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsConnectionFailed(FocalsConnectionFailedEvent e) {
        Log.i(TAG, "ConnectActivity::onFocalsConnectionFailed");
        connect_failure_count_ += 1;
        if (connect_failure_count_ >= 2) {
            Log.i(TAG, "Showing connect help: failure_count=" + connect_failure_count_);
            text_help_.setVisibility(View.VISIBLE);
        }
        if (connect_failure_count_ > LIMIT_CONNECT_FAILURES) {
            Log.i(TAG, "Restarting discovery - too many failures: failure_count=" + connect_failure_count_);
            stopConnecting();
            startDiscovery();
        } else {
            Log.i(TAG, "Connect failed - retrying: failure_count=" + connect_failure_count_);

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsDisconnected(FocalsDisconnectedEvent e) {
        Log.i(TAG, "ConnectActivity::onFocalsDisconnected");
        startProcess();
    }

}
