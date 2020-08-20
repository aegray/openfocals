package com.openfocals.services.network.cloudintercept;

import android.util.Log;

import com.openfocals.commutils.ssl.SSLInterceptDataHandler;
import com.openfocals.commutils.ssl.SSLServerDataHandler;
import com.openfocals.focals.Device;
import com.openfocals.focals.events.FocalsBluetoothMessageEvent;
import com.openfocals.focals.events.FocalsConnectedEvent;
import com.openfocals.services.network.HTTPEndpointHandler;
import com.openfocals.services.network.HTTPHandler;
import com.openfocals.services.network.InterceptedNetworkServiceManager;
import com.openfocals.services.network.InterceptedSSLSessionLogger;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

import okio.Buffer;
import okio.ByteString;

public class CloudMockService
        implements InterceptedNetworkServiceManager.InterceptedNetworkSessionFactory {

    ByteString b = ByteString.of((byte)'\r', (byte)'\n', (byte)'\r', (byte)'\n');

    private static final String DUMMY_ACCOUNT_ID = "33aad05f-744a-46af-a554-143305192394";
    private static final String DUMMY_ID = "6be4a2ed-2d6e-47ab-a6e0-8fe62dbb663a";

    private static final String DUMMY_TOKEN_BASE_1 = "eyJhbGciOiJSUzUxMiIsInR5cCI6IkpXVCJ9.eyJ2ZXJzaW9uIjoxLCJhY2NvdW50Ijp7ImVtYWlsIjoic29tZWd1eUBvZm9jYWxzLmNvbSIsIm5hbWUiOiJTb21lIGd1eSIsImZpcnN0TmFtZSI6IlNvbWUiLCJsYXN0TmFtZSI6Imd1eSJ9LCJsb2dpblNjaGVtZSI6InBhc3N3b3JkIiwibG9naW5NZXRob2RzIjpbInBhc3N3b3JkIl0sImVtcGxveWVlUm9sZXMiOltdLCJ0eXBlIjoiYmVhcmVyIiwidGFncyI6W10sImNsYXNzIjoidXNlciIsImlhdCI6MTU5NTk0NzI3OSwiZXhwIjoxNTk1OTQ4NDc5LCJpc3MiOiJpZC5ieW5vcnRoLmNvbSIsInN1YiI6IjMzYWFkMDVmLTc0NGEtNDZhZi1hNTU0LTE0MzMwNTE5MjM5NCIsImp0aSI6ImEzNzBkYTY4LWJmMjItNDA1My1iOTM5LWJjNThiYzE4MWVlYyJ9";
    private static final String DUMMY_TOKEN_BASE_2 = "V8R9_aLEwhqiGvE4e3rKyGsFjjYqwbq0bbF8FAXWufIM1UAViVJxQtJk2e5YLUHsrEiQxEOXt_dn5f6yjrjvqgHv1NxhrZ8Tqw87mui6VWRoffJT5GcZa_2dSawd4OPBnCNDoTRulPtY3reA3xi3rEmuQn2Z_xeQDQ6zq8nT8dVuIg4ABat6ZxX8NR0DxjDZmA29BTmJa5vluoWx9ZTxybbSrQG0_AL2CNSHt_DVobj4O54U33CztLJ4WeN_JBusflrjtZNgAOy5TpxUnptIu227d1MJfN6M5ubnc8R3uej_dsBNzsGEwTIedQPINGXm6e4Dh7RGWmfYXB12jQZwgVQ_WN7f1A6RF5ESLNYV318gkCGtvZ7rzZzG_LgD_OFe9ifDcw_z3Bd3whQh1ut6-9UOabf1f9j6_OLUwl7uiurvEQhW-IrPmDqxA-1uPVHOHw3FAgM_oaBAG-0oIi7vM7GBBG9Tk3XkFlX69fX_OLDvQ0nuxQP4MoInXlmF4O6SzC8DPGN_DvBkLH6AmIMq4t8kp18IZyVWnzMrMN_It3NZV3DHUZBzaUHAurkyD23sRN1QBC85hDCQTPCV9R87pZTerJCisn034t95R85oTLv_m0rne_f2V4gNKOFSQ0voRJc56kToCtEGuvnRAk_nT9JrmmUg7Q36Krl";
    private static final String DUMMY_TOKEN_BASE = DUMMY_TOKEN_BASE_1 + "." + DUMMY_TOKEN_BASE_2;

    private static final String TAG = "FOCALS_ICLOUD";
    public static final String HOSTNAME = "ofocals.com";
    public static final String CLOUD_HOSTNAME = "cloud.ofocals.com";
    //public static final String HOSTNAME = "bysouth.com";
    //public static final String CLOUD_HOSTNAME = "cloud.bysouth.com";
    //public static final String HOSTNAME = "bynorth.com";
    //public static final String CLOUD_HOSTNAME = "cloud.bynorth.com";

    Device device_;
    HTTPEndpointHandler ephandler_ = new HTTPEndpointHandler();
    CloudIntegrationHandler integrations_ = new CloudIntegrationHandler();

    private int authtoken_onid_ = 0;
    private String authtoken_;


    private String nextAuthToken() {
        authtoken_onid_ += 1;

        String newtoken = DUMMY_TOKEN_BASE;
        newtoken += String.format("%08d", authtoken_onid_);
        authtoken_ = newtoken;
        return authtoken_;
    }

    private String getAuthToken() {
        if (authtoken_ == null) {
            return nextAuthToken();
        }
        return authtoken_; 
    }


    class InterceptedCloudSSLSession
            extends InterceptedNetworkServiceManager.InterceptedNetworkSession
            implements SSLServerDataHandler.IDataSender {
        SSLServerDataHandler data_;

        HTTPEndpointHandler.HTTPEndpointHandlerSession reqhandler_ = ephandler_.getSessionHandler();

        InterceptedCloudSSLSession() throws Exception {
            data_ = SSLInterceptDataHandler.createCloudInterceptSSLHandler();
            data_.setSender(this);
            reqhandler_.setSender(new HTTPHandler.HTTPHandlerSender() {
                @Override
                public void sendData(Buffer b) throws Exception {
                    Log.i(TAG, "CloudSSLSession sending http response data: " + b.clone().toString());
                    data_.write(b);
                }

                @Override
                public void close() throws Exception {
                    Log.i(TAG, "CloudSSLSession closing socket");
                    InterceptedCloudSSLSession.this.close();
                }
            });
        }

        public void onOpen() {
        }

        public void onData(Buffer b) {
            // this is a super hacky http parser - for now I'm just adding to a buffer until
            // I see \r\n\r\n at the end, then parsing the http path + headers
            Log.d(TAG, "CloudSSLSession got net data from focals: " + b.clone());
            try {
                Buffer b2 = data_.read(b.clone());

                if (b2 != null) {
                    reqhandler_.onData(b2.clone());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void onError() {

        }

        public void onClose() {

        }

        @Override
        public void sendData(Buffer b) {
            Buffer b2 = b.clone();
            Log.d(TAG, "CloudSSLSession sending net data to focals: " + b2.toString());
            super.sendData(b.clone());
        }
    }


    public CloudIntegrationHandler getIntegrations() { return integrations_; }
    public HTTPEndpointHandler getHttpEndpoints() { return ephandler_; }

    public CloudMockService(Device d) {
        device_ = d;
        device_.getEventBus().register(this);

        ephandler_.registerEndpoint(integrations_);

        ephandler_.registerEndpoint(new HTTPEndpointHandler.HTTPEndpoint() {
            @Override
            public boolean shouldHandle(String path) {
                return path.equals("/v1/device/companions");
            }

            @Override
            public void handle(HTTPHandler h, String method, String path, Map<String, String> params,
                               Map<String, String> headers, Buffer postdata) throws Exception {
                h.sendResponse(200);
                h.sendHeader("Content-Type", "application/json; charset=utf-8");
                h.finishHeaders();
                h.sendContent("{\"message\":\"Successfully added device with ID 1f46d08e to companion with ID 6be4a2ed-2d6e-47ab-a6e0-8fe62dbb663a\"}");

                h.finishResponse();
            }
        });
        ephandler_.registerEndpoint(new HTTPEndpointHandler.HTTPEndpoint() {
            @Override
            public boolean shouldHandle(String path) {
                return path.equals("/v1/device");
            }

            @Override
            public void handle(HTTPHandler h, String method, String path, Map<String, String> params,
                               Map<String, String> headers, Buffer postdata) throws Exception {
                h.sendResponse(200);
                h.sendHeader("Content-Type", "application/json; charset=utf-8");
                h.finishHeaders();
                h.sendContent("{\"id\":\"1f46d08e\",\"hardwareVersion\":\"1\",\"softwareVersion\":\"1.119.0-4672\",\"country\":\"Canada\",\"city\":\"Kitchener\",\"buildVariant\":null,\"hardwareAttributes\":{\"wo_id\":\"W7265\",\"kg_number\":\"KG03114\",\"lens_type\":\"Prescription\",\"sku_wo_id\":\"W7253\",\"build_type\":\"Customer\",\"sku_number\":\"SKU-00036-001\"},\"projectorFirmwareVersion\" :null,\"garnetFirmwareVersion\":null}");
                h.finishResponse();
            }
        });
        ephandler_.registerEndpoint(new HTTPEndpointHandler.HTTPEndpoint() {
            @Override
            public boolean shouldHandle(String path) {
                return path.equals("/v1/messenger/user");
            }

            @Override
            public void handle(HTTPHandler h, String method, String path, Map<String, String> params,
                               Map<String, String> headers, Buffer postdata) throws Exception {
                h.sendResponse(200);
                h.sendHeader("Content-Type", "application/json; charset=utf-8");
                h.finishHeaders();
                h.sendContent("{\"id\":\"eec6e7eb-517f-45ae-aeb5-a4d8e5b224b9\",\"mobileNumber\":null,\"name\":\"Some guy\",\"updatedAt\":\"2019-10-14T23:01:34.038Z\",\"externalServiceProvider\":\"unconfigured\",\"externalPhoneNumber\":null,\"externalServiceProviderError\":false}");
                h.finishResponse();
            }
        });
        ephandler_.registerEndpoint(new HTTPEndpointHandler.HTTPEndpoint() {
            @Override
            public boolean shouldHandle(String path) {
                return path.equals("/v1/feature-manager/user");
            }

            JSONObject makeFeature(String id, String name, String description, boolean featureEnabled, boolean featureVisible, boolean featureEditable) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("id", id);
                    o.put("name", name);
                    o.put("description", description);
                    o.put("featureEnabled", featureEnabled);
                    o.put("featureEditable", featureEditable);
                    o.put("featureVisible", featureVisible);
                    return o;
                } catch (JSONException ex) {
                    Log.e(TAG, "json error");
                }
                return null;
            }

            @Override
            public void handle(HTTPHandler h, String method, String path, Map<String, String> params,
                               Map<String, String> headers, Buffer postdata) throws Exception {

                Log.i(TAG, "Sending features/manager");

                JSONObject o = new JSONObject();
                JSONArray a = new JSONArray();
// ORIGINAL TABLE:
//                a.put(makeFeature("alexa_mp2","New Alexa media player","Force usage of new Alexa media player on iPhone.",false,false,false));
//                a.put(makeFeature("unicorn_game","Jumping Game","Enable access to the log jumping game.",false,false,false));
//                a.put(makeFeature("long_disp_time","Extend Display On Time","When not interacting with your glasses, the display will stay on a little longer before fading out.",false,false,false));
//                a.put(makeFeature("click_on_connect","Click on Loop Connect","Treat a loop connection as a center press to avoid missing events",false,false,false));
//                a.put(makeFeature("gif_reply","Smart GIF Replies","Allow replying to messages with smartly-chosen animated GIF images.",false,false,false));
//                a.put(makeFeature("time_to_work_v2","Time To Work","A new now moment that lets you know the time it will take to get to work.",false,false,false));
//                a.put(makeFeature("health","Fitness Tracking","Track your physical health from Google Fit.",true,true,true));
//                a.put(makeFeature("notif_quick_act","Notification Quick Actions","Manage your incoming notifications with custom quick actions.",true,false,false));
//                a.put(makeFeature("templated_settings","Additional Settings","Settings page in the Focals app including additional settings fetched dynamically from Focals.",true,false,false));
//                a.put(makeFeature("room7","Seven Rooms Feature","Customer feature for Seven Rooms.",false,false,false));
//                a.put(makeFeature("current_poi_tip","Current POI Tip","Show a tip for the current point of interest.",false,false,false));
//                a.put(makeFeature("alexa_bg_briefing","Alexa Background Flash Briefing","Flash briefings continue to play in the background after you exit Alexa.",true,true,true));
//                a.put(makeFeature("daily_briefing","Morning Briefing","Get a quick briefing on what your morning looks like, including the forecast, commute times, and your first calendar event.",true,true,true));
//                a.put(makeFeature("flash_cards","Language Flash Cards","Learn languages using with flash cards on Focals. We support French, Japanese, Chinese (Mandarin), Arabic, and Portuguese (Brazil)  ",false,true,true));
//                a.put(makeFeature("flywheel","Longpress Action Menu","Press and hold Loop to bring up a quick action menu.",false,false,false));
//                a.put(makeFeature("headway","Commute Status","See the ETA of your commute to home and work",true,true,true));
//                a.put(makeFeature("headway_start_trip","Commute Progress","See and share your commute status with your contacts during your commute.",false,false,false));
//                a.put(makeFeature("ketchup","Conversation Awareness","Focals will hold back incoming notifications while you are in a conversation and displays a summary of what you missed once you are done.",true,false,false));
//                a.put(makeFeature("multi_moments","Multiple Now Moments","View multiple Now moments on your Now view.",true,false,false));
//                a.put(makeFeature("noisedog","What's Playing?","Discover what music is playing around you and add it to your liked songs on Spotify.",true,true,true));
//                a.put(makeFeature("pied_piper","Spotify","See what's playing on Spotify. If you have Spotify Premium, control music with Loop.",true,false,false));
//                a.put(makeFeature("rewind","Rewind","Record 30 second audio snippets from your current meeting. You'll get a recording summary through email after the meeting ends.",true,true,true));
//                a.put(makeFeature("rewind_audio","Auto Capture Meeting Snippets - Audio","Attach audio files used to generate rewind notes to the email.",false,false,false));
//                a.put(makeFeature("screenshot","Screenshot Share","Share your experience with Focals by overlaying your display on a picture you take with your phone",true,false,false));
//                a.put(makeFeature("showcase","Showcase","Explore a quick walkthrough of the latest features on Focals",true,true,true));
//                a.put(makeFeature("sign_messages","Message Signature","Let others know you are sending them messages from Focals",false,false,false));
//                a.put(makeFeature("sky_captain","Flights","Track the status of upcoming flights synced from your calendar, including any delays or changes. Send a message to let others know where you are during your trip.",true,true,true));
//                a.put(makeFeature("talking_heads","Walkie-Talkie","Use Focals like a walkie talkie and send voice messages to other Focals users.",false,false,false));
//                a.put(makeFeature("task_list","Task List","Create and manage a list of tasks on Focals.",false,false,false));
//                a.put(makeFeature("tasks_app","Tasks","Add tasks and mark them as complete using Focals and any supported Task app.",true,true,true));
//                a.put(makeFeature("teleprompter","Focals Connect","Connect Focals with your Google Slides presentations. View speaker notes and control slides from Focals ",true,true,true));
//                a.put(makeFeature("tour_guide","Venue Tips","Discover popular tips about nearby places",true,true,true));
//                a.put(makeFeature("trebek","Trivia Game","Play trivia against other Focals users. New games arrive daily. ",true,true,true));
//                a.put(makeFeature("whiterabbit","Meeting responses","Let others know that you are running late to a meeting or that you won't make it",true,false,false));
//                a.put(makeFeature("wonderland","Wonderland Demo","A magical demo experience to help you show off your Focals to friends and family",true,false,false));
//                a.put(makeFeature("wysiwis","Lenscast","Share what you are seeing on Focals with others by mirroring your display on your phone",true,false,false));
//                a.put(makeFeature("battery_lens","Battery view","See the battery level for Focals and Loop",true,true,true));
//                a.put(makeFeature("notes","Notes","View and favorite notes with Focals and any supported  ",true,true,true));
//                a.put(makeFeature("old_tbt_walking","Legacy Turn-By-Turn Walking Directions","Experimental feature for legacy turn-by-turn walking directions.",true,false,false));
//                a.put(makeFeature("android_action","Android Notification Actions","Focals will support Android smartphone notification actions.",true,false,false));
//                a.put(makeFeature("health_digital","Screen Time","Track how much phone screen time you have saved by wearing Focals",true,true,true));
//                a.put(makeFeature("calendar","Calendar view","See all your calendar events for the day",true,true,true));
//                a.put(makeFeature("go","Places view","See your current location, a list of all your saved places, and start a trip from Focals.",true,true,true));
//                a.put(makeFeature("weather","Weather view","See the current and extended weather forecast",true,true,true));
//                a.put(makeFeature("flynns","Game Arcade","Play games on Focals",false,false,false));
//                a.put(makeFeature("colour_uniformity","Colour Uniformity","Adjust colour for best uniformity",false,false,false));
//                a.put(makeFeature("smart_reply_marian","New Text Smart Replies","A new an improved text smart replies engine",false,false,false));
//                a.put(makeFeature("sportscaster","Sports Updates","Keep up with your favorite NBA, NHL, NFL or MLB teams. Get updated scores and play-by-play details from your favorite teams.",true,true,true));
//                a.put(makeFeature("flywheel2","Quick Launch","When Alexa is opened you can now click down to see a list of quick actions.",true,false,false));
//
                a.put(makeFeature("alexa_mp2","New Alexa media player","Force usage of new Alexa media player on iPhone.",false,false,false));
                a.put(makeFeature("unicorn_game","Jumping Game","Enable access to the log jumping game.",true,true,true));
                a.put(makeFeature("long_disp_time","Extend Display On Time","When not interacting with your glasses, the display will stay on a little longer before fading out.",false,true,true));
                a.put(makeFeature("click_on_connect","Click on Loop Connect","Treat a loop connection as a center press to avoid missing events",false,false,false));
                a.put(makeFeature("gif_reply","Smart GIF Replies","Allow replying to messages with smartly-chosen animated GIF images.",false,true,true));
                a.put(makeFeature("time_to_work_v2","Time To Work","A new now moment that lets you know the time it will take to get to work.",false,true,true));
                a.put(makeFeature("health","Fitness Tracking","Track your physical health from Google Fit.",true,true,true));
                a.put(makeFeature("notif_quick_act","Notification Quick Actions","Manage your incoming notifications with custom quick actions.",true,true,true));
                a.put(makeFeature("templated_settings","Additional Settings","Settings page in the Focals app including additional settings fetched dynamically from Focals.",true,true,true));
                a.put(makeFeature("room7","Seven Rooms Feature","Customer feature for Seven Rooms.",false,true,true));
                a.put(makeFeature("current_poi_tip","Current POI Tip","Show a tip for the current point of interest.",false,false,false));
                a.put(makeFeature("alexa_bg_briefing","Alexa Background Flash Briefing","Flash briefings continue to play in the background after you exit Alexa.",true,true,true));
                a.put(makeFeature("daily_briefing","Morning Briefing","Get a quick briefing on what your morning looks like, including the forecast, commute times, and your first calendar event.",true,true,true));
                a.put(makeFeature("flash_cards","Language Flash Cards","Learn languages using with flash cards on Focals. We support French, Japanese, Chinese (Mandarin), Arabic, and Portuguese (Brazil)  ",false,true,true));
                a.put(makeFeature("flywheel","Longpress Action Menu","Press and hold Loop to bring up a quick action menu.",false,false,false));
                a.put(makeFeature("headway","Commute Status","See the ETA of your commute to home and work",true,true,true));
                a.put(makeFeature("headway_start_trip","Commute Progress","See and share your commute status with your contacts during your commute.",false,false,false));
                a.put(makeFeature("ketchup","Conversation Awareness","Focals will hold back incoming notifications while you are in a conversation and displays a summary of what you missed once you are done.",true,false,false));
                a.put(makeFeature("multi_moments","Multiple Now Moments","View multiple Now moments on your Now view.",true,false,false));
                a.put(makeFeature("noisedog","What's Playing?","Discover what music is playing around you and add it to your liked songs on Spotify.",true,true,true));
                a.put(makeFeature("pied_piper","Spotify","See what's playing on Spotify. If you have Spotify Premium, control music with Loop.",true,false,false));
                a.put(makeFeature("rewind","Rewind","Record 30 second audio snippets from your current meeting. You'll get a recording summary through email after the meeting ends.",true,true,true));
                a.put(makeFeature("rewind_audio","Auto Capture Meeting Snippets - Audio","Attach audio files used to generate rewind notes to the email.",true,true,true));
                a.put(makeFeature("screenshot","Screenshot Share","Share your experience with Focals by overlaying your display on a picture you take with your phone",true,false,false));
                a.put(makeFeature("showcase","Showcase","Explore a quick walkthrough of the latest features on Focals",true,true,true));
                a.put(makeFeature("sign_messages","Message Signature","Let others know you are sending them messages from Focals",false,false,false));
                a.put(makeFeature("sky_captain","Flights","Track the status of upcoming flights synced from your calendar, including any delays or changes. Send a message to let others know where you are during your trip.",true,true,true));
                a.put(makeFeature("talking_heads","Walkie-Talkie","Use Focals like a walkie talkie and send voice messages to other Focals users.",true,true,true));
                a.put(makeFeature("task_list","Task List","Create and manage a list of tasks on Focals.",true,false,false));
                a.put(makeFeature("tasks_app","Tasks","Add tasks and mark them as complete using Focals and any supported Task app.",true,true,true));
                a.put(makeFeature("teleprompter","Focals Connect","Connect Focals with your Google Slides presentations. View speaker notes and control slides from Focals ",true,true,true));
                a.put(makeFeature("tour_guide","Venue Tips","Discover popular tips about nearby places",true,true,true));
                a.put(makeFeature("trebek","Trivia Game","Play trivia against other Focals users. New games arrive daily. ",true,true,true));
                a.put(makeFeature("whiterabbit","Meeting responses","Let others know that you are running late to a meeting or that you won't make it",true,false,false));
                a.put(makeFeature("wonderland","Wonderland Demo","A magical demo experience to help you show off your Focals to friends and family",true,false,false));
                a.put(makeFeature("wysiwis","Lenscast","Share what you are seeing on Focals with others by mirroring your display on your phone",true,false,false));
                a.put(makeFeature("battery_lens","Battery view","See the battery level for Focals and Loop",true,true,true));
                a.put(makeFeature("notes","Notes","View and favorite notes with Focals and any supported  ",true,true,true));
                a.put(makeFeature("old_tbt_walking","Legacy Turn-By-Turn Walking Directions","Experimental feature for legacy turn-by-turn walking directions.",true,false,false));
                a.put(makeFeature("android_action","Android Notification Actions","Focals will support Android smartphone notification actions.",true,false,false));
                a.put(makeFeature("health_digital","Screen Time","Track how much phone screen time you have saved by wearing Focals",true,true,true));
                a.put(makeFeature("calendar","Calendar view","See all your calendar events for the day",true,true,true));
                a.put(makeFeature("go","Places view","See your current location, a list of all your saved places, and start a trip from Focals.",true,true,true));
                a.put(makeFeature("weather","Weather view","See the current and extended weather forecast",true,true,true));
                a.put(makeFeature("flynns","Game Arcade","Play games on Focals",false,false,false));
                a.put(makeFeature("colour_uniformity","Colour Uniformity","Adjust colour for best uniformity",false,false,false));
                a.put(makeFeature("smart_reply_marian","New Text Smart Replies","A new an improved text smart replies engine",false,false,false));
                a.put(makeFeature("sportscaster","Sports Updates","Keep up with your favorite NBA, NHL, NFL or MLB teams. Get updated scores and play-by-play details from your favorite teams.",true,true,true));
                a.put(makeFeature("flywheel2","Quick Launch","When Alexa is opened you can now click down to see a list of quick actions.",true,false,false));

                o.put("features", a);
                o.put("groupId", "48c6862f-6f84-4aee-b6e4-526d93a73d89");
                o.put("groupName", "Production Default Group");

                h.sendResponse(200);
                h.sendHeader("Content-Type", "application/json; charset=utf-8");
                h.finishHeaders();

                h.sendContent(o.toString());
                h.finishResponse();
            }
        });
 //       ephandler_.registerEndpoint(new HTTPEndpointHandler.HTTPEndpoint() {
 //           @Override
 //           public boolean shouldHandle(String path) {
 //               return path.equals("/v1/integration");
 //           }

 //           JSONObject makeIntegration(
 //                   String id,
 //                   String developerId,
 //                   String name,
 //                   String description,
 //                   String iconUrl,
 //                   boolean enabled,
 //                   String enableUrl,
 //                   String disableUrl,
 //                   String settingsUrl,
 //                   String longDescription,
 //                   boolean beta,
 //                   String tncUrl,
 //                   String privacyUrl,
 //                   String publicKey
 //           ) {
 //               try {
 //                   JSONObject o = new JSONObject();
 //                   o.put("id", id);
 //                   o.put("developerId", developerId);
 //                   o.put("name", name);
 //                   o.put("description", description);
 //                   JSONObject o2 = new JSONObject();
 //                   o2.put("type", "URL");
 //                   o2.put("value", iconUrl);
 //                   o.put("icon", o2);
 //                   o.put("enabled", enabled);
 //                   o.put("enableUrl", enableUrl);
 //                   o.put("disableUrl", disableUrl);
 //                   o.put("settingsUrl", settingsUrl);
 //                   o.put("longDescription", longDescription);
 //                   o.put("beta", beta);
 //                   o.put("tncUrl", tncUrl);
 //                   o.put("privacyUrl", privacyUrl);
 //                   o.put("publicKey", publicKey);


 //                   return o;
 //               } catch (JSONException ex) {
 //                   Log.e(TAG, "json error");
 //               }
 //               return null;
 //           }

 //           @Override
 //           public void handle(HTTPHandler h, String method, String path, Map<String, String> params,
 //                              Map<String, String> headers, Buffer postdata) throws Exception {
 //               h.sendResponse(200);
 //               h.sendHeader("Content-Type", "application/json; charset=utf-8");
 //               h.finishHeaders();

 //               JSONArray a = new JSONArray();

 //               a.put(makeIntegration("slack_v1","854ceb25-7e1d-492d-82a2-a73d785611f7","Slack",          "Receive and reply to DMs and groups DMs on your Focals!","https://cfr.slack-edge.com/45901/marketing/img/_rebrand/meta/slack_hash_256.png",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=42ba0f7c-beac-47b0-89d7-8e524af58afa","https://cloud.bynorth.com/v1/integration/disable?integrationId=42ba0f7c-beac-47b0-89d7-8e524af58afa","","",false,null,null,null));
 //               a.put(makeIntegration("spotify_v1","854ceb25-7e1d-492d-82a2-a73d785611f7","Spotify",        "Control Spotify from your Focals!","https://developer.spotify.com/assets/branding-guidelines/icon3@2x.png",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=e00432ff-abbc-4f6e-8271-68fb88b80537","https://cloud.bynorth.com/v1/integration/disable?integrationId=e00432ff-abbc-4f6e-8271-68fb88b80537",null,null,false,null,null,null));
 //               a.put(makeIntegration("pushbullet_v1","854ceb25-7e1d-492d-82a2-a73d785611f7","Pushbullet",     "Share anything with your Focals from any other device!","http://www.stickpng.com/assets/images/58481546cef1014c0b5e496a.png",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=dfdfb243-670a-4030-9c7d-8fabef045a67","https://cloud.bynorth.com/v1/integration/disable?integrationId=dfdfb243-670a-4030-9c7d-8fabef045a67","","",false,null,null,null));
 //               a.put(makeIntegration("evernote_v1","854ceb25-7e1d-492d-82a2-a73d785611f7","Evernote",       "View your notes on Focals.","https://assets.brandfolder.com/pdfgvi-g314ds-23ehtb/original/App_icon-circle.png",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=a689e3c9-59f6-4320-8c88-4ce98162fdc0","https://cloud.bynorth.com/v1/integration/disable?integrationId=a689e3c9-59f6-4320-8c88-4ce98162fdc0","","",false,null,null,null));
 //               a.put(makeIntegration("todoist_v1","854ceb25-7e1d-492d-82a2-a73d785611f7","Focals Todoist", "Stay on top of your to-dos from Todoist on Focals","https://bloghubstaffcom.lightningbasecdn.com/wp-content/uploads/2017/10/Todoist-main_logo_positive-300x300.png",false,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=6b771546-e8d4-4102-9b1d-8ca7f946b49c","https://cloud.bynorth.com/v1/integration/disable?integrationId=6b771546-e8d4-4102-9b1d-8ca7f946b49c","","Stay on top of your to-dos from Todoist on Focals.\n\nNOTE: This app was not created by, affiliated with, or supported by Doist.",false,null,null,null));
 //               a.put(makeIntegration("onenote_v1","854ceb25-7e1d-492d-82a2-a73d785611f7","OneNote",        "List and see your OneNote on Focals.","https://images-na.ssl-images-amazon.com/images/I/21PvrNoucFL._SY355_.png",false,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=9f8e88a7-097e-4c85-b1be-4808bddf3176","https://cloud.bynorth.com/v1/integration/disable?integrationId=9f8e88a7-097e-4c85-b1be-4808bddf3176","","",false,null,null,null));
 //               a.put(makeIntegration("googletasks_v1","854ceb25-7e1d-492d-82a2-a73d785611f7","Google Tasks",   "Manage your Google Tasks from Focals.","https://www.androidpolice.com/wp-content/cache/wp-appbox/a32f0c784daf6ebdee8d1d2aa88cf1b5/ai-ad558b8d3a18effc498879622379f140",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=b7eed5df-45c2-448b-9ad2-6dfd341e40cf","https://cloud.bynorth.com/v1/integration/disable?integrationId=b7eed5df-45c2-448b-9ad2-6dfd341e40cf","","",false,null,null,null));
 //               a.put(makeIntegration("googlefit_v1","854ceb25-7e1d-492d-82a2-a73d785611f7","Google Fit",     "See your physical activity on your Focals.","https://gstatic.com/images/branding/product/1x/gfit_512dp.png",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=873d95ce-8614-4d19-84d2-6168c2fdb154","https://cloud.bynorth.com/v1/integration/disable?integrationId=873d95ce-8614-4d19-84d2-6168c2fdb154","","",false,null,null,null));
 //               a.put(makeIntegration("drinkwater_v1","854ceb25-7e1d-492d-82a2-a73d785611f7","Drink Water",    "Drink water reminders","https://cdn.iconscout.com/icon/premium/png-256-thumb/drinking-water-568666.png",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=7ddd21d7-f824-4e2e-8b79-81ad8d022ad0","https://cloud.bynorth.com/v1/integration/disable?integrationId=7ddd21d7-f824-4e2e-8b79-81ad8d022ad0","","",false,null,null,null));
 //               a.put(makeIntegration("twitter_v1","854ceb25-7e1d-492d-82a2-a73d785611f7","Twitter",        "Twitter threads for your Focals!","https://icon-library.net/images/twitter-icon-images/twitter-icon-images-12.jpg",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=56ee8339-7eb0-431c-bbc0-de17b49f19a5","https://cloud.bynorth.com/v1/integration/disable?integrationId=56ee8339-7eb0-431c-bbc0-de17b49f19a5","",null,false,null,null,null));

 //               //a.put(makeIntegration("42ba0f7c-beac-47b0-89d7-8e524af58afa","854ceb25-7e1d-492d-82a2-a73d785611f7","Slack",          "Receive and reply to DMs and groups DMs on your Focals!","https://cfr.slack-edge.com/45901/marketing/img/_rebrand/meta/slack_hash_256.png",false,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=42ba0f7c-beac-47b0-89d7-8e524af58afa","https://cloud.bynorth.com/v1/integration/disable?integrationId=42ba0f7c-beac-47b0-89d7-8e524af58afa","","",false,null,null,null));
 //               //a.put(makeIntegration("e00432ff-abbc-4f6e-8271-68fb88b80537","854ceb25-7e1d-492d-82a2-a73d785611f7","Spotify",        "Control Spotify from your Focals!","https://developer.spotify.com/assets/branding-guidelines/icon3@2x.png",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=e00432ff-abbc-4f6e-8271-68fb88b80537","https://cloud.bynorth.com/v1/integration/disable?integrationId=e00432ff-abbc-4f6e-8271-68fb88b80537",null,null,false,null,null,null));
 //               //a.put(makeIntegration("dfdfb243-670a-4030-9c7d-8fabef045a67","854ceb25-7e1d-492d-82a2-a73d785611f7","Pushbullet",     "Share anything with your Focals from any other device!","http://www.stickpng.com/assets/images/58481546cef1014c0b5e496a.png",false,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=dfdfb243-670a-4030-9c7d-8fabef045a67","https://cloud.bynorth.com/v1/integration/disable?integrationId=dfdfb243-670a-4030-9c7d-8fabef045a67","","",false,null,null,null));
 //               //a.put(makeIntegration("a689e3c9-59f6-4320-8c88-4ce98162fdc0","854ceb25-7e1d-492d-82a2-a73d785611f7","Evernote",       "View your notes on Focals.","https://assets.brandfolder.com/pdfgvi-g314ds-23ehtb/original/App_icon-circle.png",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=a689e3c9-59f6-4320-8c88-4ce98162fdc0","https://cloud.bynorth.com/v1/integration/disable?integrationId=a689e3c9-59f6-4320-8c88-4ce98162fdc0","","",false,null,null,null));
 //               //a.put(makeIntegration("6b771546-e8d4-4102-9b1d-8ca7f946b49c","854ceb25-7e1d-492d-82a2-a73d785611f7","Focals Todoist", "Stay on top of your to-dos from Todoist on Focals","https://bloghubstaffcom.lightningbasecdn.com/wp-content/uploads/2017/10/Todoist-main_logo_positive-300x300.png",false,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=6b771546-e8d4-4102-9b1d-8ca7f946b49c","https://cloud.bynorth.com/v1/integration/disable?integrationId=6b771546-e8d4-4102-9b1d-8ca7f946b49c","","Stay on top of your to-dos from Todoist on Focals.\n\nNOTE: This app was not created by, affiliated with, or supported by Doist.",false,null,null,null));
 //               //a.put(makeIntegration("9f8e88a7-097e-4c85-b1be-4808bddf3176","854ceb25-7e1d-492d-82a2-a73d785611f7","OneNote",        "List and see your OneNote on Focals.","https://images-na.ssl-images-amazon.com/images/I/21PvrNoucFL._SY355_.png",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=9f8e88a7-097e-4c85-b1be-4808bddf3176","https://cloud.bynorth.com/v1/integration/disable?integrationId=9f8e88a7-097e-4c85-b1be-4808bddf3176","","",false,null,null,null));
 //               //a.put(makeIntegration("b7eed5df-45c2-448b-9ad2-6dfd341e40cf","854ceb25-7e1d-492d-82a2-a73d785611f7","Google Tasks",   "Manage your Google Tasks from Focals.","https://www.androidpolice.com/wp-content/cache/wp-appbox/a32f0c784daf6ebdee8d1d2aa88cf1b5/ai-ad558b8d3a18effc498879622379f140",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=b7eed5df-45c2-448b-9ad2-6dfd341e40cf","https://cloud.bynorth.com/v1/integration/disable?integrationId=b7eed5df-45c2-448b-9ad2-6dfd341e40cf","","",false,null,null,null));
 //               //a.put(makeIntegration("873d95ce-8614-4d19-84d2-6168c2fdb154","854ceb25-7e1d-492d-82a2-a73d785611f7","Google Fit",     "See your physical activity on your Focals.","https://gstatic.com/images/branding/product/1x/gfit_512dp.png",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=873d95ce-8614-4d19-84d2-6168c2fdb154","https://cloud.bynorth.com/v1/integration/disable?integrationId=873d95ce-8614-4d19-84d2-6168c2fdb154","","",false,null,null,null));
 //               //a.put(makeIntegration("7ddd21d7-f824-4e2e-8b79-81ad8d022ad0","854ceb25-7e1d-492d-82a2-a73d785611f7","Drink Water",    "Drink water reminders","https://cdn.iconscout.com/icon/premium/png-256-thumb/drinking-water-568666.png",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=7ddd21d7-f824-4e2e-8b79-81ad8d022ad0","https://cloud.bynorth.com/v1/integration/disable?integrationId=7ddd21d7-f824-4e2e-8b79-81ad8d022ad0","","",false,null,null,null));
 //               //a.put(makeIntegration("56ee8339-7eb0-431c-bbc0-de17b49f19a5","854ceb25-7e1d-492d-82a2-a73d785611f7","Twitter",        "Twitter threads for your Focals!","https://icon-library.net/images/twitter-icon-images/twitter-icon-images-12.jpg",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=56ee8339-7eb0-431c-bbc0-de17b49f19a5","https://cloud.bynorth.com/v1/integration/disable?integrationId=56ee8339-7eb0-431c-bbc0-de17b49f19a5","",null,false,null,null,null));

 //               h.sendContent(a.toString());
 //               h.finishResponse();
 //           }
 //       });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsConnected(FocalsConnectedEvent e) {
        Log.i(TAG, "Initializing cloud: token=" + getAuthToken());
        device_.setupCloud(HOSTNAME, DUMMY_ACCOUNT_ID, getAuthToken(), DUMMY_ID);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsMessage(FocalsBluetoothMessageEvent e) {
        if (e.message.hasRefreshCloudToken()) {
            device_.setCloudToken(nextAuthToken());
            Log.i(TAG, "Refreshed cloud token with dummy: " + getAuthToken());
        }
    }

    @Override
    public InterceptedNetworkServiceManager.InterceptedNetworkSession createSession() throws Exception {
        return new InterceptedCloudSSLSession();
    }
}
