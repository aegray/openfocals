package com.openfocals.services.network.present.providers;

import com.openfocals.services.network.present.PresentationProvider;

public class StreamingTextPresentationProvider extends PresentationProvider {
    private static final String TAG = "FOCALS_PRESENT_STEXT";
    int state_ = -1;

    int rate_ = 1000;

    String texts_[] = new String[]{
            "Sometimes you have to go",
            "To the store many times",
            "in order to find what you want",
            "and sometimes you never find it",
            "no matter how many times you go"
    };


    public void setRate(int msPerCard) {
        rate_ = msPerCard;
    }


    public void setTexts(String[] texts) {
        texts_ = texts;
    }

    void sendNextCard() {
        if (state_ >= 0) {
            state_ = state_ + 1;
            if (state_ > texts_.length) {
                state_ = 1;
            }
            sendCard(texts_[state_ - 1]);

            new android.os.Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            sendNextCard();
                        }
                    }, rate_);
        }
    }

    @Override
    public void resetPresentation() {
        state_ = 0;
        sendNextCard();
    }

    @Override
    public void onNext() {
        rate_ = java.lang.Math.max(50, (rate_ / 2));
    }

    @Override
    public void onPrevious() {
        rate_ = rate_ * 2;
    }

    @Override
    public void onClose() {
        state_ = -1;
    }

}
