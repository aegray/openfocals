package com.openfocals.buddy.ui.calibrate;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.openfocals.buddy.FocalsBuddyApplication;
import com.openfocals.buddy.R;
import com.openfocals.focals.Device;
import com.openfocals.focals.events.FocalsBluetoothMessageEvent;
import com.openfocals.focals.events.FocalsConnectedEvent;
import com.openfocals.focals.events.FocalsDisconnectedEvent;
import com.openfocals.focals.messages.CalibrationResponse;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;


public class CalibrateFragment extends Fragment {
    private static final String TAG = "FOCALS_CALIBRATE";
    private static final int INTERVAL_SIZE_MS = 1000;

    Handler handler_ = new Handler();


    Device device_;

    TextView text_connected_;
    TextView text_status_;


    List<Button> conditional_buttons_ = new ArrayList<>();

    boolean starting_ = true;
    int counter_ = 0;


    private final Runnable label_handler = new Runnable() {
        @Override
        public void run() {
            if (!starting_) return;
            counter_ += 1;

            String label = "Starting calibration";

            for (int i = 0; i < (counter_ % 3) + 1; ++i) {
                label += ".";
            }

            text_status_.setText(label);
            handler_.postDelayed(label_handler, INTERVAL_SIZE_MS) ;
        }
    };

    public CalibrateFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_calibrate, container, false);
    }


    private void addButton(int id, View.OnClickListener listener) {
        Button b = getView().findViewById(id);
        b.setOnClickListener(listener);
        conditional_buttons_.add(b);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        device_ = ((FocalsBuddyApplication) getActivity().getApplication()).device;
        text_connected_ = getView().findViewById(R.id.textCalibrationConnected);
        text_status_ = getView().findViewById(R.id.textCalibrationStatus);


        addButton(R.id.buttonCalibrateStop, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                device_.calibrationStop();
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });


        updateEnabled();
    }


    @Override
    public void onStart() {
        super.onStart();
        device_.getEventBus().register(this);

        if (!device_.isConnected()) {
            Toast.makeText(
                    getContext(),
                    "Cannot run calibration - not connected",
                    Toast.LENGTH_SHORT).show();

            getActivity().getSupportFragmentManager().popBackStack();
        } else {
            device_.calibrationStart();
            handler_.postDelayed(label_handler, INTERVAL_SIZE_MS) ;
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        device_.getEventBus().unregister(this);
    }

    private void setEnabled(boolean enabled) {
        for (Button b : conditional_buttons_) {
            b.setEnabled(enabled);
        }
    }

    private void updateEnabled() {
        if (device_.isConnected()) {
            text_connected_.setVisibility(View.INVISIBLE);
            setEnabled(true);
        } else {
            text_connected_.setVisibility(View.VISIBLE);
            setEnabled(false);
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsConnected(FocalsConnectedEvent e) {
        updateEnabled();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsBluetoothMessage(FocalsBluetoothMessageEvent e) {
        if (e.message.hasCalibration()) {
            CalibrationResponse r = e.message.getCalibration();
            if (r.hasStarted()) {
                // we
                starting_ = false;
                device_.calibrationSetMainMode();
                text_status_.setText("Calibrating.  \n" +
                        "Try to line up the circle and crosses so they are overlapping as much as possible\n" +
                        "Press up, down, left, and right on the loop to move and when they are overlapping," +
                        " click the loop to accept.");
            } else if (r.hasStopped()) {

                Log.i(TAG, "Calibration finished: " + r.getStopped().getResult());
                getActivity().getSupportFragmentManager().popBackStack();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsDisconnected(FocalsDisconnectedEvent e) {
        Toast.makeText(
                getContext(),
                "Cannot run calibration - lost connection",
                Toast.LENGTH_SHORT).show();
        getActivity().getSupportFragmentManager().popBackStack();
    }

}
