package com.openfocals.buddy;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.openfocals.services.notifications.NotificationService;
import com.openfocals.services.DeviceService;

public class StartupActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;
    private static final String TAG = "FOCALS_MAIN";

    boolean bt_enabled_ = false;
    boolean bt_seen_ = false;
    boolean got_perms_ = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.startup_activity);

        Context context = getApplicationContext();

        // make sure user has bluetooth enabled
        BluetoothAdapter bt_adapter = BluetoothAdapter.getDefaultAdapter();
        if (!bt_adapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            bt_enabled_ = true;
            bt_seen_ = true;
        }

        // start device service
        if (!isMyServiceRunning(DeviceService.class)) {
            Intent intent = new Intent(this, DeviceService.class);
            Log.i(TAG, "Device service not started - starting");
            startService(intent);
        } else {
            Log.i(TAG, "Device service was already started");
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.RECORD_AUDIO,
            }, PERMISSION_REQUEST_CODE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 10);
            }
        }

        tryGoMain();
    }


    private void tryGoMain() {
        // we can continue without notif perms - it just means no notifications
        // we're also allowing us to go to the main screen even if bt not enabled
        if (got_perms_ && bt_seen_) {
            goMain();
        }
    }

    private void goMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        if(requestCode == PERMISSION_REQUEST_CODE)
        {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Log.i(TAG, "coarse location permission granted");
                got_perms_ = true;
                tryGoMain();
            }
            else
            {
                Log.e(TAG, "coarse location permission denied");
                Toast.makeText(this, "Location permissions required for scanning bluetooth devices and finding focals.  Quitting",
                        Toast.LENGTH_LONG);
                finishAndRemoveTask();;
            }
        }
    }


    @Override
    public void onActivityResult(int request, int result, Intent intent) {
        if (request == REQUEST_ENABLE_BT) {
            Log.i(TAG, "Got discovery result: " + result + " : " + RESULT_OK + " / " + RESULT_CANCELED);
            bt_seen_ = true;
            if (result == RESULT_OK) {
                bt_enabled_ = true;
            }
            tryGoMain();
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


}
