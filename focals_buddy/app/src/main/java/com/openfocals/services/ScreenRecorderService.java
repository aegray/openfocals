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
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;


/*
 * ScreenRecordingSample
 * Sample project to cature and save audio from internal and video from screen as MPEG4 file.
 *
 * Copyright (c) 2015 saki t_saki@serenegiant.com
 *
 * File name: ScreenRecorderService.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
 */

//import java.io.IOException;
//
//import android.annotation.TargetApi;
//import android.app.Notification;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.app.Service;
//import android.content.Context;
//import android.content.Intent;
//import android.media.projection.MediaProjection;
//import android.media.projection.MediaProjectionManager;
//import android.os.Build;
//import android.os.IBinder;
//import android.support.annotation.Nullable;
//import android.text.TextUtils;
//import android.util.DisplayMetrics;
//import android.util.Log;

import com.openfocals.buddy.MainActivity;
import com.openfocals.buddy.R;
import com.openfocals.services.screenmirror.ScreenFrameGrabber;
//import com.serenegiant.media.MediaAudioEncoder;
//import com.serenegiant.media.MediaEncoder;
//import com.serenegiant.media.MediaMuxerWrapper;
//import com.serenegiant.media.MediaScreenEncoder;
//import com.serenegiant.media.ScreenFrameGrabber;
//import com.serenegiant.screenrecordingsample.MainActivity;
//import com.serenegiant.screenrecordingsample.R;
//import com.serenegiant.utils.BuildCheck;
//import com.serenegiant.utils.FileUtils;

public class ScreenRecorderService extends Service {
    private static final boolean DEBUG = false;
    private static final String TAG = "openfocals screen recording";
    private static final String APP_DIR_NAME = "openfocals";
    //static {
    //    FileUtils.DIR_NAME = APP_DIR_NAME;
    //}
    //MediaProjection projection_ = null;

    private static final String BASE = "com.openfocals.service.ScreenRecorderService.";
    public static final String ACTION_START = BASE + "ACTION_START";
    public static final String ACTION_STOP = BASE + "ACTION_STOP";
    public static final String ACTION_PAUSE = BASE + "ACTION_PAUSE";
    public static final String ACTION_RESUME = BASE + "ACTION_RESUME";
    public static final String ACTION_QUERY_STATUS = BASE + "ACTION_QUERY_STATUS";
    public static final String ACTION_QUERY_STATUS_RESULT = BASE + "ACTION_QUERY_STATUS_RESULT";
    public static final String EXTRA_RESULT_CODE = BASE + "EXTRA_RESULT_CODE";
    public static final String EXTRA_QUERY_RESULT_RECORDING = BASE + "EXTRA_QUERY_RESULT_RECORDING";
    public static final String EXTRA_QUERY_RESULT_PAUSING = BASE + "EXTRA_QUERY_RESULT_PAUSING";
    private static final int NOTIFICATION = R.string.app_name;


    private static final String NOTIFICATION_CHANNEL_ID = "openfocals_screengrab";
    private static ScreenFrameGrabber srec;


    private static final Object sSync = new Object();
    //private static MediaMuxerWrapper sMuxer;

    private MediaProjectionManager mMediaProjectionManager;
    private NotificationManager mNotificationManager;

    public ScreenRecorderService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //if (BuildCheck.isLollipop())
            mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationManager.createNotificationChannel(
                    new NotificationChannel(NOTIFICATION_CHANNEL_ID, "********", NotificationManager.IMPORTANCE_DEFAULT));
        }

        showNotification(TAG);
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.v(TAG, "onDestroy:");
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
        } else if (ACTION_PAUSE.equals(action)) {
            pauseScreenRecord();
        } else if (ACTION_RESUME.equals(action)) {
            resumeScreenRecord();
        }
        return result;
    }

    private boolean updateStatus() {
        final boolean isRecording, isPausing;
        synchronized (sSync) {
            isRecording = (srec != null);
            isPausing = false; //isRecording && srec..isPaused();
        }
        final Intent result = new Intent();
        result.setAction(ACTION_QUERY_STATUS_RESULT);
        result.putExtra(EXTRA_QUERY_RESULT_RECORDING, isRecording);
        result.putExtra(EXTRA_QUERY_RESULT_PAUSING, isPausing);
        sendBroadcast(result);
        return isRecording;
    }

    /**
     * start screen recording as .mp4 file
     * @param intent
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startScreenRecord(final Intent intent) {


        synchronized (sSync) {
            if (srec == null) {
                final int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
                // get MediaProjection
                final MediaProjection projection = mMediaProjectionManager.getMediaProjection(resultCode, intent);
                if (projection != null) {
                    final DisplayMetrics metrics = getResources().getDisplayMetrics();
                    int width = metrics.widthPixels;
                    int height = metrics.heightPixels;
                    if (width > height) {
                        // 横長
                        final float scale_x = width / 1920f;
                        final float scale_y = height / 1080f;
                        final float scale = Math.max(scale_x,  scale_y);
                        width = (int)(width / scale);
                        height = (int)(height / scale);
                    } else {
                        // 縦長
                        final float scale_x = width / 1080f;
                        final float scale_y = height / 1920f;
                        final float scale = Math.max(scale_x,  scale_y);
                        width = (int)(width / scale);
                        height = (int)(height / scale);
                    }
                    //srec = new ScreenFrameGrabber(projection, 120, 120, metrics.densityDpi); //width, height, metrics.densityDpi);

                    int expect_width = 220 + 180;
                    int expect_height = height * expect_width / width;
                    Log.i("SCREEN", "REQUESTING IMAGE: " + expect_height);


                    srec = new ScreenFrameGrabber(projection, expect_width, expect_height, metrics.densityDpi); //width, height, metrics.densityDpi);

                    srec.setListener(DeviceService.getInstance().screenListener);
                    srec.start();


                    //if (DEBUG) Log.v(TAG, String.format("startRecording:(%d,%d)(%d,%d)", metrics.widthPixels, metrics.heightPixels, width, height));
                    /////try {
                    /////	sMuxer = new MediaMuxerWrapper(this, ".mp4");	// if you record audio only, ".m4a" is also OK.
                    /////	if (true) {
                    /////		// for screen capturing
                    /////		new MediaScreenEncoder(sMuxer, mMediaEncoderListener,
                    /////			projection, width, height, metrics.densityDpi, 800 * 1024, 15);
                    /////	}
                    /////	if (false) {
                    /////		// for audio capturing
                    /////		new MediaAudioEncoder(sMuxer, mMediaEncoderListener);
                    /////	}
                    /////	sMuxer.prepare();
                    /////	sMuxer.startRecording();
                    /////} catch (final IOException e) {
                    /////	Log.e(TAG, "startScreenRecord:", e);
                    /////}
                }
            }
        }
    }

    /**
     * stop screen recording
     */
    private void stopScreenRecord() {
        //if (DEBUG) Log.v(TAG, "stopScreenRecord:sMuxer=" + sMuxer);
        ////synchronized (sSync) {
        ////	if (sMuxer != null) {
        ////		sMuxer.stopRecording();
        ////		sMuxer = null;
        ////		// you should not wait here
        ////	}
        ////}
        srec = null;
        //projection_ = null;
        stopForeground(true/*removeNotification*/);
        if (mNotificationManager != null) {
            mNotificationManager.cancel(NOTIFICATION);
            mNotificationManager = null;
        }
        stopSelf();
    }

    private void pauseScreenRecord() {
        ////synchronized (sSync) {
        ////	if (sMuxer != null) {
        ////		sMuxer.pauseRecording();
        ////	}
        ////}
    }

    private void resumeScreenRecord() {
        ////synchronized (sSync) {
        ////	if (sMuxer != null) {
        ////		sMuxer.resumeRecording();
        ////	}
        ////}
    }

    /**
     * callback methods from encoder
     */
    //private static final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
    //    @Override
    //    public void onPrepared(final MediaEncoder encoder) {
    //        if (DEBUG) Log.v(TAG, "onPrepared:encoder=" + encoder);
    //    }

    //    @Override
    //    public void onStopped(final MediaEncoder encoder) {
    //        if (DEBUG) Log.v(TAG, "onStopped:encoder=" + encoder);
    //    }
    //};

//================================================================================
    /**
     * helper method to show/change message on notification area
     * and set this service as foreground service to keep alive as possible as this can.
     * @param text
     */
    private void showNotification(final CharSequence text) {
        // Set the info for the views that show in the notification panel.
        Notification.Builder b = new Notification.Builder(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            b.setChannelId(NOTIFICATION_CHANNEL_ID);
        }
        Notification notification = b
                .setSmallIcon(R.drawable.ic_launcher_foreground) //.ic_launcher)  // the status icon
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
        //FileUtils.DIR_NAME = APP_DIR_NAME;
        return PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
    }

}
