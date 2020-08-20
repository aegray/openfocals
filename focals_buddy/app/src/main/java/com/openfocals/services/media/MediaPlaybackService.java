package com.openfocals.services.media;

import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;

import java.util.List;

public class MediaPlaybackService {
    private static final String TAG = "FOCALS_MEDIA";

    static MediaPlaybackService instance_;

    MediaSessionManager mediaSessionManager;
    ComponentName componentName;
    MediaController controller;

    AudioManager audio_mgr_;

    boolean is_active_ = false;
    boolean is_playing_ = true;

    public static MediaPlaybackService getInstance() { return instance_; }

    private class MyAudioPlaybackCallback extends AudioManager.AudioPlaybackCallback {
        @Override
        public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
            is_active_ = audio_mgr_.isMusicActive();
            is_playing_ = configs.size() != 0;
            Log.i(TAG, "Music state change: is_active=" + is_active_ + " is_playing=" + is_playing_);

            for (AudioPlaybackConfiguration c : configs) {
                Log.i(TAG, "  config: " + c.getAudioAttributes().toString());
            }
        }
    }

//    MediaSessionManager.OnActiveSessionsChangedListener sessionsChangedListener = new MediaSessionManager.OnActiveSessionsChangedListener() {
//        @Override
//        public void onActiveSessionsChanged(List<MediaController> controllers) {
//            Log.d(TAG, "onActiveSessionsChanged: session is changed");
//            for (MediaController controller : controllers) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                    Log.i(TAG, "onActiveSessionsChanged: controller = " + controller.getPackageName());
//                    MediaMetadata meta = controller.getMetadata();
//                    Log.i(TAG, "onCreate: artist = " + meta.getString(MediaMetadata.METADATA_KEY_ARTIST));
//                    Log.i(TAG, "onCreate: song = " + meta.getString(MediaMetadata.METADATA_KEY_TITLE));
//                }
//            }
//        }
//    };

    public boolean isMediaActive() { return is_active_; }
    public boolean isMediaPlaying() { return is_playing_; }

    public void mediaNext() {
        audio_mgr_.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT));
    }

    public void mediaPrevious() {
        audio_mgr_.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
    }

    public void mediaPause() {
        audio_mgr_.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE));
    }

    public void mediaPlay() {
        audio_mgr_.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY));
    }

    public void mediaVolumeDown() {
        int cur = audio_mgr_.getStreamVolume(AudioManager.STREAM_MUSIC);
        int min = audio_mgr_.getStreamMinVolume(AudioManager.STREAM_MUSIC);
        if (cur > min) {
            audio_mgr_.setStreamVolume(AudioManager.STREAM_MUSIC, cur - 2, 0);
        }
    }

    public void mediaVolumeUp() {

        int cur = audio_mgr_.getStreamVolume(AudioManager.STREAM_MUSIC);
        int max = audio_mgr_.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (cur < max) {
            audio_mgr_.setStreamVolume(AudioManager.STREAM_MUSIC, cur + 2, 0);
        }
    }

    public int getMediaVolume() {
        int cur = audio_mgr_.getStreamVolume(AudioManager.STREAM_MUSIC);
        int max = audio_mgr_.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int min = audio_mgr_.getStreamMinVolume(AudioManager.STREAM_MUSIC);
        Log.i(TAG, "Media volume: cur=" + cur + " minmax=" + min + "/" + max);
        return (int)(100*((cur - min) / ((float)((max == min) ? 1 : (max - min)))));
    }


    public MediaPlaybackService(Context ctx) {
        instance_ = this;
        audio_mgr_ = (AudioManager)ctx.getSystemService(Context.AUDIO_SERVICE);
        audio_mgr_.registerAudioPlaybackCallback(new MyAudioPlaybackCallback(), new Handler());

        //componentName = new ComponentName(ctx, MediaPlaybackService.class);
//
//
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
//            mediaSessionManager = (MediaSessionManager) ctx.getSystemService(Context.MEDIA_SESSION_SERVICE);
//            //mediaSessionManager.addOnActiveSessionsChangedListener(sessionsChangedListener, componentName);
//
//            List<MediaController> controllers = mediaSessionManager.getActiveSessions(componentName);
//            Log.d(TAG, "onCreate listener: controllers size = " + controllers.size());
//            for (MediaController mediaController : controllers) {
//                controller = mediaController;
//                Log.d(TAG, "onCreate: controller = " + controller.getPackageName());
//                MediaMetadata meta = controller.getMetadata();
//                Log.d(TAG, "onCreate: artist = " + meta.getString(MediaMetadata.METADATA_KEY_ARTIST));
//                Log.d(TAG, "onCreate: song = " + meta.getString(MediaMetadata.METADATA_KEY_TITLE));
//            }
//        }
    }
}
