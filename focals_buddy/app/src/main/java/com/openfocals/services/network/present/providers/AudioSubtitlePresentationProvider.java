package com.openfocals.services.network.present.providers;

import android.content.Context;
import android.util.Log;

import com.openfocals.services.network.present.PresentationProvider;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class AudioSubtitlePresentationProvider extends PresentationProvider {
    private static final String TAG = "FOCALS_SUBTITLES";
    private static final int RESET_AFTER_MILLIS = 1000;
    private Context context_;
    private SpeechToTextProvider speech_;

    private static final int MAX_CHARS_ON_SCREEN = 60;

    private String prev_;
    private String last_full_;

    boolean open_ = false;


    private long last_speech_ = 0;

    String str_ = "";


    public AudioSubtitlePresentationProvider(Context c) {
        speech_ = new SpeechToTextProvider(c);
        EventBus.getDefault().register(this);
    }

    Runnable periodic_clearer = new Runnable() {
        @Override
        public void run() {

            if (open_) {
                long cur = System.currentTimeMillis();
                if (cur > last_speech_ + RESET_AFTER_MILLIS) {
                    prev_ = last_full_;
                    sendCard("");
                }

                new android.os.Handler().postDelayed(this, 500);
            }
        }
    };


    @Override
    public void resetPresentation() {
        str_ = "";
        open_ = true;
        sendCard("");
        speech_.start();

        last_speech_ = System.currentTimeMillis();
        new android.os.Handler().postDelayed(periodic_clearer, 500);

        Log.i(TAG, "Starting to listen for speech audio");
    }

    @Override
    public void onNext() {
        speech_.stop();
    }

    @Override
    public void onPrevious() {
    }

    @Override
    public void onClose() {
        open_ = false;
        speech_.stop();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSpeech(SpeechToTextProvider.SpeechRecognitionResult in_result) {
        if (open_ && (in_result.result != null)) {
            last_speech_ = System.currentTimeMillis();

            if ((last_full_ != null) && (in_result.result.equals(last_full_))) {
                return;
            }
            last_full_ = in_result.result;

            String[] parts = in_result.result.split("\n");
            String iresult = parts[parts.length-1];

            if (prev_ != null) {
                if ((iresult.length() < prev_.length()) && (iresult.length() < MAX_CHARS_ON_SCREEN / 2)) {
                    prev_ = null;
                }
            }

            String result = iresult;
            if ((prev_ != null)  && (result.length() > prev_.length())) {
                result = result.substring(prev_.length());
            }

            if (result.length() > MAX_CHARS_ON_SCREEN) {
                int index = iresult.lastIndexOf(' ');

                if (index >= 0)
                    prev_ = iresult.substring(0, index+1);
                else
                    prev_ = iresult;

                sendCard(result);
                Log.i(TAG, "Sending text: " + result.length() + " / " + result + " / " + prev_);
            } else {
                sendCard(result);
            }
        }
    }
}
