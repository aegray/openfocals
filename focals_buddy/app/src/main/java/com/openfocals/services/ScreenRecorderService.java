package com.openfocals.services;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;


import com.openfocals.buddy.MainActivity;
import com.openfocals.buddy.R;
import com.openfocals.services.screenmirror.ScreenFrameGrabber;
import com.openfocals.services.screenmirror.VideoSelectRectOverlayView;

public class ScreenRecorderService extends Service implements View.OnTouchListener {

    private static final String TAG = "FOCALS_SCREEN_RECORD";
    private static final String TITLE = "openfocals screen recording";
    private static final String APP_DIR_NAME = "openfocals";

    private static final String BASE = "com.openfocals.service.ScreenRecorderService.";
    public static final String ACTION_START = BASE + "ACTION_START";
    public static final String ACTION_STOP = BASE + "ACTION_STOP";
    public static final String ACTION_QUERY_STATUS = BASE + "ACTION_QUERY_STATUS";
    public static final String ACTION_QUERY_STATUS_RESULT = BASE + "ACTION_QUERY_STATUS_RESULT";
    public static final String EXTRA_RESULT_CODE = BASE + "EXTRA_RESULT_CODE";
    public static final String EXTRA_QUERY_RESULT_RECORDING = BASE + "EXTRA_QUERY_RESULT_RECORDING";
    private static final int NOTIFICATION = R.string.app_name;


    private static final String NOTIFICATION_CHANNEL_ID = "openfocals_screengrab";
    private static ScreenFrameGrabber srec;


    private static final Object sSync = new Object();

    private MediaProjectionManager mMediaProjectionManager;
    private NotificationManager mNotificationManager;

    private VideoSelectRectOverlayView overlay_view_;

    public ScreenRecorderService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationManager.createNotificationChannel(
                    new NotificationChannel(NOTIFICATION_CHANNEL_ID, "openfocals_record", NotificationManager.IMPORTANCE_DEFAULT));
        }


        showNotification(TITLE);
    }

    public void removeOverlay() {
        if (overlay_view_ != null) {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(overlay_view_);
            overlay_view_ = null;
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {

        int result = START_STICKY;
        final String action = intent != null ? intent.getAction() : null;
        if (ACTION_START.equals(action)) {
            startScreenRecord(intent);
            updateStatus();
        } else if (ACTION_STOP.equals(action) || TextUtils.isEmpty(action)) {
            stopScreenRecord();
            updateStatus();
            result = START_NOT_STICKY;
        } else if (ACTION_QUERY_STATUS.equals(action)) {
            if (!updateStatus()) {
                stopSelf();
                result = START_NOT_STICKY;
            }
        }
        return result;
    }

    private boolean updateStatus() {
        final boolean isRecording;
        synchronized (sSync) {
            isRecording = (srec != null);
        }
        final Intent result = new Intent();
        result.setAction(ACTION_QUERY_STATUS_RESULT);
        result.putExtra(EXTRA_QUERY_RESULT_RECORDING, isRecording);
        sendBroadcast(result);
        return isRecording;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startScreenRecord(final Intent intent) {
        synchronized (sSync) {
            if (srec == null) {
                final int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
                // draw video overlay
                addOverlayView();
                final MediaProjection projection = mMediaProjectionManager.getMediaProjection(resultCode, intent);
                if (projection != null) {
                    final DisplayMetrics metrics = getResources().getDisplayMetrics();
                    srec = new ScreenFrameGrabber(projection, metrics.widthPixels, metrics.heightPixels, metrics.densityDpi);
                    srec.setListener(DeviceService.getInstance().screenListener);
                    srec.start();

                }
            }
        }
    }

    private void stopScreenRecord() {
        srec.stop();
        srec = null;

        removeOverlay();
        stopForeground(true);
        if (mNotificationManager != null) {
            mNotificationManager.cancel(NOTIFICATION);
            mNotificationManager = null;
        }
        stopSelf();
    }


    private void showNotification(final CharSequence text) {
        // Set the info for the views that show in the notification panel.
        Notification.Builder b = new Notification.Builder(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            b.setChannelId(NOTIFICATION_CHANNEL_ID);
        }
        Notification notification = b
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.app_name))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(createPendingIntent())  // The intent to send when the entry is clicked
                .build();
        //startForeground(NOTIFICATION, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        //    startForeground(0, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        //} else {
        //    startForeground(0, notification);
        //}
        startForeground(NOTIFICATION, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        // Send the notification.
        mNotificationManager.notify(NOTIFICATION, notification);
    }

    protected PendingIntent createPendingIntent() {
        return PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
    }


    private void addOverlayView() {

        if (overlay_view_ == null) {

            final WindowManager.LayoutParams params;
            int layoutParamsType;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                layoutParamsType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutParamsType = WindowManager.LayoutParams.TYPE_PHONE;
            }

            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    layoutParamsType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    //WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT);

            params.gravity = Gravity.TOP | Gravity.LEFT;

            Rect f = DeviceService.getInstance().screen_cap_rect;
            if (f == null) {
                f = new Rect(0, 0, 400, 400);
                DeviceService.getInstance().screen_cap_rect = f;
            }


            params.x = f.left;
            params.y = f.top;
            params.width = f.width();
            params.height = f.height();


            //FrameLayout interceptorLayout = new FrameLayout(this);
            //interceptorLayout.addView(new VideoSelectRectOverlayView(this));
            //{
            //
            //            @Override
            //            public boolean dispatchKeyEvent(KeyEvent event) {
            //
            ////                // Only fire on the ACTION_DOWN event, or you'll get two events (one for _DOWN, one for _UP)
            ////                if (event.getAction() == KeyEvent.ACTION_DOWN) {
            ////
            ////                    // Check if the HOME button is pressed
            ////                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            ////
            ////                        Log.v(TAG, "BACK Button Pressed");
            ////
            ////                        // As we've taken action, we'll return true to prevent other apps from consuming the event as well
            ////                        return true;
            ////                    }
            ////                }
            ////
            ////                return false;
            ////
            //                // Otherwise don't intercept the event
            //                return super.dispatchKeyEvent(event);
            //            }
            //            @Override
            //            public boolean dispatchTouchEvent(MotionEvent event) {
            //
            //                Log.i(TAG, "Got touch event");
            //                return false; //super.onInterceptTouchEvent();
            ////                // Only fire on the ACTION_DOWN event, or you'll get two events (one for _DOWN, one for _UP)
            ////                if (event.getAction() == KeyEvent.ACTION_DOWN) {
            ////
            ////                    // Check if the HOME button is pressed
            ////                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            ////
            ////                        Log.v(TAG, "BACK Button Pressed");
            ////
            ////                        // As we've taken action, we'll return true to prevent other apps from consuming the event as well
            ////                        return true;
            ////                    }
            ////                }
            ////
            ////                return false;
            ////
            //                // Otherwise don't intercept the event
            //                //return super.dispatchKeyEvent(event);
            //            }
            //        };
            //        LayoutInflater inflater = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE));
            //
            //        if (inflater != null) {
            //            overlay_view_ = inflater.inflate(R.layout.floating_view, interceptorLayout);
            //            //overlay_view_.setOnTouchListener(this);
            //            //overlay_view_.setOnIn
            //            ((WindowManager) getSystemService(WINDOW_SERVICE)).addView(overlay_view_, params);
            //            //windowManager.addView(overlay_view_, params);
            //        }
            //        else {
            //            Log.e("SAW-example", "Layout Inflater Service is null; can't inflate and display R.layout.floating_view");
            //        }
            overlay_view_ = new VideoSelectRectOverlayView(this);
            overlay_view_.params = params;
            ((WindowManager) getSystemService(WINDOW_SERVICE)).addView(overlay_view_, params);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.i(TAG, "GOT TOUCH");
        return false;
    }
}
