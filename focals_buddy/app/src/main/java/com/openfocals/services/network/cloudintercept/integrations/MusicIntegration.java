package com.openfocals.services.network.cloudintercept.integrations;

import android.util.Log;

import com.openfocals.services.media.MediaPlaybackService;
import com.openfocals.services.network.cloudintercept.CloudIntegrationHandler;
import com.openfocals.services.network.cloudintercept.CloudMockService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

public class MusicIntegration extends CloudIntegrationHandler.CloudIntegration {

    private static final String INTEGRATION_ID = "e00432ff-abbc-4f6e-8271-68fb88b80537";


   // ,"854ceb25-7e1d-492d-82a2-a73d785611f7","Spotify",        "Control Spotify from your Focals!","https://developer.spotify.com/assets/branding-guidelines/icon3@2x.png",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=e00432ff-abbc-4f6e-8271-68fb88b80537","https://cloud.bynorth.com/v1/integration/disable?integrationId=e00432ff-abbc-4f6e-8271-68fb88b80537",null,null,false,null,null,null));

    private static final String TAG = "FOCALS_MUSIC";

    ////a.put(makeIntegration("e00432ff-abbc-4f6e-8271-68fb88b80537","854ceb25-7e1d-492d-82a2-a73d785611f7","Spotify",        "Control Spotify from your Focals!","https://developer.spotify.com/assets/branding-guidelines/icon3@2x.png",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=e00432ff-abbc-4f6e-8271-68fb88b80537","https://cloud.bynorth.com/v1/integration/disable?integrationId=e00432ff-abbc-4f6e-8271-68fb88b80537",null,null,false,null,null,null));

    boolean playing_ = true;

    public void register(CloudMockService h) {
        h.getIntegrations().registerIntegration(INTEGRATION_ID, this, "Spotify", "Control music", null);
    }

    public JSONObject createMediaPlayback() throws Exception {

        JSONObject o = new JSONObject();
        JSONArray actions = new JSONArray();
        JSONArray uactions = new JSONArray();

        uactions.put("add_to_favorites");
        o.put("user_actions", uactions);
        o.put("is_premium", true);

        if (MediaPlaybackService.getInstance().isMediaActive()) {
            actions.put("pause");
            actions.put("previous");
            actions.put("next");
            actions.put("volume_up");
            actions.put("volume_down");

            o.put("success", true);
            o.put("state", new JSONObject().put("volume", MediaPlaybackService.getInstance().getMediaVolume()));

            JSONObject track = new JSONObject();
            track.put("album", "");
            JSONArray artists = new JSONArray();
            //artists.put("test artist");
            artists.put("");
            track.put("artists", artists);

            track.put("duration", 230229);

            JSONArray images = new JSONArray();
            //images.put(new JSONObject().put("width", 640).put("height", 640).put("url", "https://i.scdn.co/image/ab67616d0000b2733f07eeb4c3639d6191c529c8"));
            //images.put(new JSONObject().put("width", 300).put("height", 300).put("url", "https://i.scdn.co/image/ab67616d00001e023f07eeb4c3639d6191c529c8"));
            //images.put(new JSONObject().put("width", 64).put("height", 64).put("url", "https://i.scdn.co/image/ab67616d000048513f07eeb4c3639d6191c529c8"));

            track.put("name", "Phone media");
            track.put("is_playing", MediaPlaybackService.getInstance().isMediaPlaying());
            track.put("progress_ms", 175615);


            o.put("track", track);

        } else {
            o.put("success", false);
        }

        o.put("actions", actions);

        return o;
    }


    public CloudIntegrationHandler.IntegrationResponse handleRequest(String method, String id, Map<String, String> params, Map<String, String> headers, JSONObject req) throws Exception {
        String action = req.getString("actionId");

        String response = null;
        Log.i(TAG, "Get task action (" + id + "): " + action + " : " + req.toString());
        if (!id.equals(INTEGRATION_ID)) {
            return new CloudIntegrationHandler.IntegrationResponse(404);
        }

        if (action.equals("get_current_info")) {
            Log.i(TAG, "Sending music info");

            JSONObject o = createMediaPlayback();

            Log.i(TAG, "Got request for music info, sending: " + o.toString());
            return new CloudIntegrationHandler.IntegrationResponse(o.toString());

        } else {
            Log.i(TAG, "Got music action: " + req.toString());

            if (action != null) {

                if (action.equals("play")) {
                    MediaPlaybackService.getInstance().mediaPlay();
                } else if (action.equals("pause")) {
                    MediaPlaybackService.getInstance().mediaPause();
                } else if (action.equals("previous")) {
                    MediaPlaybackService.getInstance().mediaPrevious();
                } else if (action.equals("next")) {
                    MediaPlaybackService.getInstance().mediaNext();
                } else if (action.equals("volume_down")) {
                    MediaPlaybackService.getInstance().mediaVolumeDown();
                } else if (action.equals("volume_up")) {
                    MediaPlaybackService.getInstance().mediaVolumeUp();
                } else {
                    Log.e(TAG, "Unknown media command: " + action);
                }
            }
            JSONObject o = createMediaPlayback();
            Log.i(TAG, "Sending current music: " + o.toString());
            return new CloudIntegrationHandler.IntegrationResponse(o.toString()); //"{}");
        }
    }
}
