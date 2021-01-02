package com.openfocals.buddy.ui.screenmirror;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.openfocals.buddy.FocalsBuddyApplication;
import com.openfocals.buddy.MainActivity;
import com.openfocals.buddy.R;
import com.openfocals.focals.Device;
import com.openfocals.focals.events.FocalsBluetoothMessageEvent;
import com.openfocals.focals.events.FocalsConnectedEvent;
import com.openfocals.focals.events.FocalsDisconnectedEvent;
import com.openfocals.focals.messages.CalibrationResponse;
import com.openfocals.services.ScreenRecorderService;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


public class ScreenMirrorFragment extends Fragment {
    private static final String TAG = "FOCALS_SCREEN_MIRROR";
    private static final int INTERVAL_SIZE_MS = 1000;
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1;

    //Device device_;

    //TextView text_connected_;
    //TextView text_status_;
    //List<Button> conditional_buttons_ = new ArrayList<>();

    //boolean starting_ = true;
    //int counter_ = 0;

    private ToggleButton mRecordButton;
    private ToggleButton mPauseButton;
    private MyBroadcastReceiver mReceiver;



    public ScreenMirrorFragment() {
        // Required empty public constructor
        mReceiver = new MyBroadcastReceiver(this); //getActivity()); //(MainActivity)getActivity().this); //this);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_screen_mirror, container, false);
    }




    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRecordButton = (ToggleButton)view.findViewById(R.id.record_button);
        mPauseButton = (ToggleButton)view.findViewById(R.id.pause_button);
        updateRecording(false, false);
        //if (mReceiver == null) {
        //    mReceiver = new MyBroadcastReceiver(this); //getActivity()); //(MainActivity)getActivity().this); //this);
        //}

//
//        addButton(R.id.buttonCalibrateStop, new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                device_.calibrationStop();
//                getActivity().getSupportFragmentManager().popBackStack();
//            }
//        });
//
//
//        updateEnabled();
    }

    private static final class MyBroadcastReceiver extends BroadcastReceiver {
        private final WeakReference<ScreenMirrorFragment> mWeakParent;
        public MyBroadcastReceiver(final ScreenMirrorFragment parent) {
            mWeakParent = new WeakReference<ScreenMirrorFragment>(parent);
        }

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (ScreenRecorderService.ACTION_QUERY_STATUS_RESULT.equals(action)) {
                final boolean isRecording = intent.getBooleanExtra(ScreenRecorderService.EXTRA_QUERY_RESULT_RECORDING, false);
                final boolean isPausing = intent.getBooleanExtra(ScreenRecorderService.EXTRA_QUERY_RESULT_PAUSING, false);
                final ScreenMirrorFragment parent = mWeakParent.get();
                if (parent != null) {
                    parent.updateRecording(isRecording, isPausing);
                }
            }
        }
    }


    @Override
    public void onStart() {
        //super.onResume();
        super.onStart();
        ((MainActivity)getActivity()).setActivityResultReceiverFragment(this);
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ScreenRecorderService.ACTION_QUERY_STATUS_RESULT);
        getContext().registerReceiver(mReceiver, intentFilter);
        queryRecordingStatus();
        //getActivity().getSupportFragmentManager().popBackStack();

    }


    @Override
    public void onPause() {
        super.onPause();
        ((MainActivity)getActivity()).setActivityResultReceiverFragment(null);

        //getContext().unregisterReceiver(mReceiver);
    }

    private void queryRecordingStatus() {
        final Intent intent = new Intent(((MainActivity)getActivity()), ScreenRecorderService.class);
        intent.setAction(ScreenRecorderService.ACTION_QUERY_STATUS);
        getContext().startService(intent);
    }

    private void startScreenRecorder(final int resultCode, final Intent data) {
        final Intent intent = new Intent(((MainActivity)getActivity()), ScreenRecorderService.class);
        intent.setAction(ScreenRecorderService.ACTION_START);
        intent.putExtra(ScreenRecorderService.EXTRA_RESULT_CODE, resultCode);
        intent.putExtras(data);
        getContext().startService(intent);
    }

    private void updateRecording(final boolean isRecording, final boolean isPausing) {
        mRecordButton.setOnCheckedChangeListener(null);
        mPauseButton.setOnCheckedChangeListener(null);
        try {
            mRecordButton.setChecked(isRecording);
            mPauseButton.setEnabled(isRecording);
            mPauseButton.setChecked(isPausing);
        } finally {
            mRecordButton.setOnCheckedChangeListener(mOnCheckedChangeListener);
            mPauseButton.setOnCheckedChangeListener(mOnCheckedChangeListener);
        }
    }

    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_CODE_SCREEN_CAPTURE == requestCode) {
            if (resultCode != Activity.RESULT_OK) {
                // when no permission
                Toast.makeText(getActivity(), "permission denied", Toast.LENGTH_LONG).show();
                return;
            }
            startScreenRecorder(resultCode, data);
        }
    }

    private final CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
            switch (buttonView.getId()) {
                case R.id.record_button:
                    if (isChecked) {
                        final MediaProjectionManager manager
                                = (MediaProjectionManager)getContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                        final Intent permissionIntent = manager.createScreenCaptureIntent();
                        startActivityForResult(permissionIntent, REQUEST_CODE_SCREEN_CAPTURE);
                    } else {
                        final Intent intent = new Intent(((MainActivity)getActivity()), ScreenRecorderService.class);
                        intent.setAction(ScreenRecorderService.ACTION_STOP);
                        getContext().startService(intent);
                    }
                    break;
                case R.id.pause_button:
                    if (isChecked) {
                        final Intent intent = new Intent(((MainActivity)getActivity()), ScreenRecorderService.class);
                        intent.setAction(ScreenRecorderService.ACTION_PAUSE);
                        getContext().startService(intent);
                    } else {
                        final Intent intent = new Intent(((MainActivity)getActivity()), ScreenRecorderService.class);
                        intent.setAction(ScreenRecorderService.ACTION_RESUME);
                        getContext().startService(intent);
                    }
                    break;
            }
        }
    };


}
