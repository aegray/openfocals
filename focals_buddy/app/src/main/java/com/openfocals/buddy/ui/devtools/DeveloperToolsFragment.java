package com.openfocals.buddy.ui.devtools;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;

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
import com.openfocals.focals.events.FocalsConnectedEvent;
import com.openfocals.focals.events.FocalsDisconnectedEvent;
import com.openfocals.services.files.FileTransferService;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.NOTIFICATION_SERVICE;

public class DeveloperToolsFragment extends Fragment {

    private static final String TAG = "FOCALS_DEVTOOLS";

    Device device_;

    List<Button> conditional_buttons_ = new ArrayList<>();

    Button button_generate_notification_;
    Button button_screencast_start_;
    Button button_screencast_stop_;
    Button button_unpair_loop_;
    Button button_set_display_offsets_;
    Button button_download_update_;
    Button button_send_update_;

    TextView text_connected_;


    int notif_onind_ = 0;

    public DeveloperToolsFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_developer_tools, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        device_ = ((FocalsBuddyApplication) getActivity().getApplication()).device;
        button_generate_notification_ = getView().findViewById(R.id.buttonGenNotification);

        text_connected_ = getView().findViewById(R.id.textDevtoolsConnected);

        button_unpair_loop_ = getView().findViewById(R.id.buttonUnpairLoop);
        button_unpair_loop_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                device_.unpairLoop();
            }
        });
        conditional_buttons_.add(button_unpair_loop_);

        button_screencast_start_ = getView().findViewById(R.id.buttonScreencastStart);
        button_screencast_start_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                device_.startScreencast("192.168.1.6", 8002);
            }
        });
        conditional_buttons_.add(button_screencast_start_);

        button_screencast_stop_ = getView().findViewById(R.id.buttonScreencastStop);
        button_screencast_stop_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                device_.stopScreencast();
            }
        });
        conditional_buttons_.add(button_screencast_stop_);


        button_download_update_ = getView().findViewById(R.id.buttonDownloadUpdate);
        button_download_update_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileTransferService.getInstance().downloadUpdateFile("http://192.168.1.6:8000/update.zip");
            }
        });


        button_send_update_ = getView().findViewById(R.id.buttonSendUpdateToFocals);
        button_send_update_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileTransferService.getInstance().sendUpdateToFocals();
            }
        });
        conditional_buttons_.add(button_send_update_);


        button_set_display_offsets_ = getView().findViewById(R.id.buttonAdjustDisplayOffsets);
        button_set_display_offsets_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int x = Integer.valueOf(((TextView)getView().findViewById(R.id.editTextDisplayOffsetX)).getText().toString());
                    int y = Integer.valueOf(((TextView)getView().findViewById(R.id.editTextDisplayOffsetY)).getText().toString());

                    device_.setDisplayOffsets(x, y);
                } catch (Exception e) {
                    // pass - its a devtool
                    Log.e(TAG, "Failed to set display offsets: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to set display offsets: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        getView().findViewById(R.id.buttonNetworkDisable).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                device_.setNetworkEnabled(false);
            }
        });
        getView().findViewById(R.id.buttonNetworkEnable).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                device_.setNetworkEnabled(true);
            }
        });
        getView().findViewById(R.id.buttonForgetCurrentDevice).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                device_.stop();
                device_.setTarget(null, null);
            }
        });

        getView().findViewById(R.id.buttonGenNotification).setOnClickListener((new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        Context context = getContext();
                        NotificationManager mNotificationManager =
                                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            NotificationChannel channel = new NotificationChannel("FOCALS",
                                    "FOCALS CHANNEL",
                                    NotificationManager.IMPORTANCE_DEFAULT);
                            channel.setDescription("OPENFOCALS NOTIFICATIONS");
                            mNotificationManager.createNotificationChannel(channel);
                        }
                        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, "FOCALS")
                                .setSmallIcon(R.mipmap.ic_launcher) // notification icon
                                .setContentTitle("test title") // title for notification
                                .setContentText("test message")// message for notification
                                .setAutoCancel(true); // clear notification after click

                        mNotificationManager.notify(notif_onind_, mBuilder.build());
                        notif_onind_ += 1;
                    }
                }));


        updateEnabled();
    }


    @Override
    public void onStart() {
        super.onStart();
        device_.getEventBus().register(this);
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
    public void onFocalsDisconnected(FocalsDisconnectedEvent e) {
        updateEnabled();
    }


}
