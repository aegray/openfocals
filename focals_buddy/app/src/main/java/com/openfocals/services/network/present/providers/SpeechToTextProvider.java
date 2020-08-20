package com.openfocals.services.network.present.providers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.speech.SpeechRecognizer;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import org.greenrobot.eventbus.EventBus;
import org.mozilla.deepspeech.libdeepspeech.DeepSpeechModel;
import org.mozilla.deepspeech.libdeepspeech.DeepSpeechStreamingState;

class SpeechToTextProvider {
    private static final String TAG = "FOCALS_SPEECH";
    private static final long BEAM_WIDTH = 500;
    private static final float LM_ALPHA = 0.931289039105002f;
    private static final float LM_BETA = 1.1834137581510284f;



    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    AudioRecord recorder_ = null;
    Thread recording_thread_ = null;
    boolean is_recording_ = false;


    private int NUM_BUFFER_ELEMENTS = 1024;
    private int BYTES_PER_ELEMENT = 2; // 2 bytes (short) because of 16 bit format

    private static final String TFLITE_MODEL_FILENAME = "deepspeech-0.8.0-models.tflite";
    private static final String SCORER_FILENAME = "deepspeech-0.8.0-models.scorer";
    
    private DeepSpeechModel model_ = null;
    private DeepSpeechStreamingState stream_context_ = null;

    private Context context_ = null;

//    private void checkAudioPermission() {
//        // permission is automatically granted on sdk < 23 upon installation
//        if (Build.VERSION.SDK_INT >= 23)
//        {
//            String permission = Manifest.permission.RECORD_AUDIO;
//
//            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.RECORD_AUDIO }, REQUEST_AUDIO_PERMS);
//            }
//        }
//    }


    public class SpeechRecognitionResult {
        public boolean intermediate = false;
        public String result;

        public SpeechRecognitionResult(String result) {
            this.result = result;
        }

        public SpeechRecognitionResult(String result, boolean intermed) {
            this.result = result;
            this.intermediate = intermed;
        }
    }

    private void transcribe() {
        short[] audioData = new short[NUM_BUFFER_ELEMENTS];

        while (is_recording_) {
            recorder_.read(audioData, 0, NUM_BUFFER_ELEMENTS);
            model_.feedAudioContent(stream_context_, audioData, audioData.length);
            String decoded = model_.intermediateDecode(stream_context_);
            Log.i(TAG, "SpeechToText got result: " + decoded);

            EventBus.getDefault().post(new SpeechRecognitionResult(decoded, true));
        }
    }

    private boolean createModel() {
        String modelsPath = context_.getExternalFilesDir(null).toString();
        String tfliteModelPath = modelsPath + "/" + TFLITE_MODEL_FILENAME;
        String scorerPath = modelsPath + "/" + SCORER_FILENAME;

        ////for (
        //for (path in listOf(tfliteModelPath, scorerPath)) {
        //    if (!(File(path).exists())) {
        //        status.text = "Model creation failed: $path does not exist."
        //        return false
        //    }
        //}
        Log.i(TAG, "Loading tflite: " + tfliteModelPath);
        model_ = new DeepSpeechModel(tfliteModelPath);
        model_.setBeamWidth(BEAM_WIDTH);
        Log.i(TAG, "Loading scorer: " + scorerPath);
        model_.enableExternalScorer(scorerPath);
        model_.setScorerAlphaBeta(LM_ALPHA, LM_BETA);
        Log.i(TAG, "Finished creating model");

        return true;
    }

    private void startListening() {
        Log.i(TAG, "Starting SpeechToText listening");

        //status.text = "Creating model...\n"

        if (model_ == null) {
            Log.i(TAG, "SpeechToText creating model");
            if (!createModel()) {
                Log.e(TAG, "SpeechToText failed to create model");
                return;
            }
            //status.append("Created model.\n")
        } else {
            //status.append("Model already created.\n")
        }

        if (model_ != null) {
            Log.e(TAG, "Created model, starting recording");
            stream_context_ = model_.createStream();


            if (recorder_ == null) {
                recorder_ = new AudioRecord(
                        MediaRecorder.AudioSource.VOICE_RECOGNITION,
                        model_.sampleRate(),
                        RECORDER_CHANNELS,
                        RECORDER_AUDIO_ENCODING,
                        NUM_BUFFER_ELEMENTS * BYTES_PER_ELEMENT);
            }
            recorder_.startRecording();
            is_recording_ = true;

            if (recording_thread_ == null) {
                recording_thread_ = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        transcribe();
                    }
                }, "AudioRecorder Thread");
                recording_thread_.start();
            }
            Log.i(TAG, "SpeechToText started recording");
        }
    }

    //@Override
    //void onCreate(Bundle savedInstanceState) {
    //    //super.onCreate(savedInstanceState)
    //    //setContentView(R.layout.activity_main)
    //    //checkAudioPermission()

    //    // create application data directory on the device
    //    getExternalFilesDir(null)

    //    status.text = "Ready, waiting ..."
    //}

    private void stopListening() {
        Log.i(TAG, "Stopping SpeechToText listening");
        is_recording_ = false;
        Log.i(TAG, "Stoppping recorder");
        recorder_.stop();
        recorder_ = null;
        //isRecording = false
        //btnStartInference.text = "Start Recording"

        Log.i(TAG, "Calling finish stream");
        String decoded = model_.finishStream(stream_context_);
        stream_context_ = null;
        Log.i(TAG, "Finished stream");



        //val decoded = model?.finishStream(stream_context_)
        //transcription.text = decoded

        if (recording_thread_ != null) {
            recording_thread_.interrupt();
            recording_thread_ = null;
        }

        if (model_ != null) {
            model_.freeModel();
            model_ = null;
        }
    }

    void start() {
        Log.i(TAG, "Starting SpeechToText");
        if (!is_recording_) 
            startListening();
    }

    void stop() {
        Log.i(TAG, "Stopping SpeechToText");
        if (is_recording_)
            stopListening();
    }

    public SpeechToTextProvider(Context c) {
        context_ = c;
    }
//
//    @Override
//    protected void finalize() {
//        if (model_ != null) {
//            model_.freeModel();
//        }
//    }
//

    //@Override
    //void onDestroy() {
    //    super.onDestroy()
    //    if (model_ != null) {
    //        model_.freeModel();
    //    }
    //}


}
