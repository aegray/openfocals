package com.openfocals.services.network.cloudintercept;


import android.util.Log;

import com.openfocals.services.network.HTTPEndpointHandler;
import com.openfocals.services.network.HTTPHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import okio.Buffer;

public class CloudIntegrationHandler extends HTTPEndpointHandler.HTTPEndpoint {
    private static final String TAG = "FOCALS_ICLOUD";
    private static final String FIXED_DEV_ID = "854ceb25-7e1d-492d-82a2-a73d785611f7";
    public static final String RESP_PATH = "/v1/integration/respond/";
    public static final String ROOT_PATH = "/v1/integration";

    public static class IntegrationResponse {
        public int response_code = 200;
        public String response;

        public IntegrationResponse(int _response_code) {
            response_code = _response_code;
        }

        public IntegrationResponse(int r, String o) {
            response_code = r;
            response = o;
        }

        public IntegrationResponse(String o) {
            response_code = 200;
            response = o;
        }
    }

    public static class CloudIntegration {
        public IntegrationResponse handleRequest(String method, String id, Map<String, String> params, Map<String, String> headers, JSONObject request) throws Exception {
            return new IntegrationResponse(new JSONObject()
                    .put("ok", false)
                    .put("name", "Error")
                    .put("message", "Unimplmented").toString());
        }
    };


    HashMap<String, IntegrationInfo> integrations_ = new HashMap<>();

    CloudIntegration def_handler_ = new CloudIntegration();


    class IntegrationInfo {
        String id;
        CloudIntegration handler;
        String name;
        String description;
        String iconUrl;

    }

    public void registerIntegration(String id, CloudIntegration handler, String name, String description, String iconUrl) {
        IntegrationInfo inf = new IntegrationInfo();
        inf.id = id;
        inf.handler = handler;
        inf.name = name;
        inf.description = description;
        inf.iconUrl = iconUrl;
        integrations_.put(id, inf);
    }


    public boolean shouldHandle(String path) {
        return path.startsWith(ROOT_PATH);
    }

    private IntegrationResponse makeError() throws JSONException {
        return new IntegrationResponse(404, new JSONObject()
                .put("ok", false)
                .put("name", "Error")
                .put("message", "Unimplemented").toString());
    }


    JSONObject makeIntegration(
            String id,
            String developerId,
            String name,
            String description,
            String iconUrl,
            boolean enabled,
            String enableUrl,
            String disableUrl,
            String settingsUrl,
            String longDescription,
            boolean beta,
            String tncUrl,
            String privacyUrl,
            String publicKey
    ) {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("developerId", developerId);
            o.put("name", name);
            o.put("description", description);
            JSONObject o2 = new JSONObject();
            o2.put("type", "URL");
            o2.put("value", iconUrl);
            o.put("icon", o2);
            o.put("enabled", enabled);
            o.put("enableUrl", enableUrl);
            o.put("disableUrl", disableUrl);
            o.put("settingsUrl", settingsUrl);
            o.put("longDescription", longDescription);
            o.put("beta", beta);
            o.put("tncUrl", tncUrl);
            o.put("privacyUrl", privacyUrl);
            o.put("publicKey", publicKey);


            return o;
        } catch (JSONException ex) {
            Log.e(TAG, "json error");
        }
        return null;
    }


    @Override
    public void handle(HTTPHandler h, String method, String path, Map<String, String> params, Map<String, String> headers, Buffer postdata) throws Exception {
        Log.i(TAG, "Got integration request: " + path);
        if (path.equals(ROOT_PATH)) {
            Log.i(TAG, "GLASSES REQUESTED INTEGRATIONS LIST");
            //Got integration request: " + path);
            throw new RuntimeException("GLASSES REQUESTED INTEGRATIONS LIST");
            //h.sendResponse(200);
            //h.sendHeader("Content-Type", "application/json; charset=utf-8");
            //h.finishHeaders();

            //JSONArray a = new JSONArray();

            //for (HashMap.Entry<String, IntegrationInfo> v : integrations_.entrySet()) {
            //    a.put(makeIntegration(v.getKey(), FIXED_DEV_ID, v.getValue().name, v.getValue().description,
            //            v.getValue().iconUrl, true, null, null, null,
            //            "", false, null, null, null));
            //}

            //// adapted table
//          //  a.put(makeIntegration("slack_v1","854ceb25-7e1d-492d-82a2-a73d785611f7","Slack",          "Receive and reply to DMs and groups DMs on your Focals!","https://cfr.slack-edge.com/45901/marketing/img/_rebrand/meta/slack_hash_256.png",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=42ba0f7c-beac-47b0-89d7-8e524af58afa","https://cloud.bynorth.com/v1/integration/disable?integrationId=42ba0f7c-beac-47b0-89d7-8e524af58afa","","",false,null,null,null));
//          //  a.put(makeIntegration("spotify_v1","854ceb25-7e1d-492d-82a2-a73d785611f7","Spotify",        "Control Spotify from your Focals!","https://developer.spotify.com/assets/branding-guidelines/icon3@2x.png",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=e00432ff-abbc-4f6e-8271-68fb88b80537","https://cloud.bynorth.com/v1/integration/disable?integrationId=e00432ff-abbc-4f6e-8271-68fb88b80537",null,null,false,null,null,null));
//          //  a.put(makeIntegration("pushbullet_v1","854ceb25-7e1d-492d-82a2-a73d785611f7","Pushbullet",     "Share anything with your Focals from any other device!","http://www.stickpng.com/assets/images/58481546cef1014c0b5e496a.png",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=dfdfb243-670a-4030-9c7d-8fabef045a67","https://cloud.bynorth.com/v1/integration/disable?integrationId=dfdfb243-670a-4030-9c7d-8fabef045a67","","",false,null,null,null));
//          //  a.put(makeIntegration("evernote_v1","854ceb25-7e1d-492d-82a2-a73d785611f7","Evernote",       "View your notes on Focals.","https://assets.brandfolder.com/pdfgvi-g314ds-23ehtb/original/App_icon-circle.png",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=a689e3c9-59f6-4320-8c88-4ce98162fdc0","https://cloud.bynorth.com/v1/integration/disable?integrationId=a689e3c9-59f6-4320-8c88-4ce98162fdc0","","",false,null,null,null));
//          //  a.put(makeIntegration("todoist_v1","854ceb25-7e1d-492d-82a2-a73d785611f7","Focals Todoist", "Stay on top of your to-dos from Todoist on Focals","https://bloghubstaffcom.lightningbasecdn.com/wp-content/uploads/2017/10/Todoist-main_logo_positive-300x300.png",false,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=6b771546-e8d4-4102-9b1d-8ca7f946b49c","https://cloud.bynorth.com/v1/integration/disable?integrationId=6b771546-e8d4-4102-9b1d-8ca7f946b49c","","Stay on top of your to-dos from Todoist on Focals.\n\nNOTE: This app was not created by, affiliated with, or supported by Doist.",false,null,null,null));
//          //  a.put(makeIntegration("onenote_v1","854ceb25-7e1d-492d-82a2-a73d785611f7","OneNote",        "List and see your OneNote on Focals.","https://images-na.ssl-images-amazon.com/images/I/21PvrNoucFL._SY355_.png",false,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=9f8e88a7-097e-4c85-b1be-4808bddf3176","https://cloud.bynorth.com/v1/integration/disable?integrationId=9f8e88a7-097e-4c85-b1be-4808bddf3176","","",false,null,null,null));
//          //  a.put(makeIntegration("googletasks_v1","854ceb25-7e1d-492d-82a2-a73d785611f7","Google Tasks",   "Manage your Google Tasks from Focals.","https://www.androidpolice.com/wp-content/cache/wp-appbox/a32f0c784daf6ebdee8d1d2aa88cf1b5/ai-ad558b8d3a18effc498879622379f140",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=b7eed5df-45c2-448b-9ad2-6dfd341e40cf","https://cloud.bynorth.com/v1/integration/disable?integrationId=b7eed5df-45c2-448b-9ad2-6dfd341e40cf","","",false,null,null,null));
//          //  a.put(makeIntegration("googlefit_v1","854ceb25-7e1d-492d-82a2-a73d785611f7","Google Fit",     "See your physical activity on your Focals.","https://gstatic.com/images/branding/product/1x/gfit_512dp.png",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=873d95ce-8614-4d19-84d2-6168c2fdb154","https://cloud.bynorth.com/v1/integration/disable?integrationId=873d95ce-8614-4d19-84d2-6168c2fdb154","","",false,null,null,null));
//          //  a.put(makeIntegration("drinkwater_v1","854ceb25-7e1d-492d-82a2-a73d785611f7","Drink Water",    "Drink water reminders","https://cdn.iconscout.com/icon/premium/png-256-thumb/drinking-water-568666.png",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=7ddd21d7-f824-4e2e-8b79-81ad8d022ad0","https://cloud.bynorth.com/v1/integration/disable?integrationId=7ddd21d7-f824-4e2e-8b79-81ad8d022ad0","","",false,null,null,null));
//          //  a.put(makeIntegration("twitter_v1","854ceb25-7e1d-492d-82a2-a73d785611f7","Twitter",        "Twitter threads for your Focals!","https://icon-library.net/images/twitter-icon-images/twitter-icon-images-12.jpg",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=56ee8339-7eb0-431c-bbc0-de17b49f19a5","https://cloud.bynorth.com/v1/integration/disable?integrationId=56ee8339-7eb0-431c-bbc0-de17b49f19a5","",null,false,null,null,null));
//
            //// orig table
            ////a.put(makeIntegration("42ba0f7c-beac-47b0-89d7-8e524af58afa","854ceb25-7e1d-492d-82a2-a73d785611f7","Slack",          "Receive and reply to DMs and groups DMs on your Focals!","https://cfr.slack-edge.com/45901/marketing/img/_rebrand/meta/slack_hash_256.png",false,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=42ba0f7c-beac-47b0-89d7-8e524af58afa","https://cloud.bynorth.com/v1/integration/disable?integrationId=42ba0f7c-beac-47b0-89d7-8e524af58afa","","",false,null,null,null));
            ////a.put(makeIntegration("e00432ff-abbc-4f6e-8271-68fb88b80537","854ceb25-7e1d-492d-82a2-a73d785611f7","Spotify",        "Control Spotify from your Focals!","https://developer.spotify.com/assets/branding-guidelines/icon3@2x.png",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=e00432ff-abbc-4f6e-8271-68fb88b80537","https://cloud.bynorth.com/v1/integration/disable?integrationId=e00432ff-abbc-4f6e-8271-68fb88b80537",null,null,false,null,null,null));
            ////a.put(makeIntegration("dfdfb243-670a-4030-9c7d-8fabef045a67","854ceb25-7e1d-492d-82a2-a73d785611f7","Pushbullet",     "Share anything with your Focals from any other device!","http://www.stickpng.com/assets/images/58481546cef1014c0b5e496a.png",false,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=dfdfb243-670a-4030-9c7d-8fabef045a67","https://cloud.bynorth.com/v1/integration/disable?integrationId=dfdfb243-670a-4030-9c7d-8fabef045a67","","",false,null,null,null));
            ////a.put(makeIntegration("a689e3c9-59f6-4320-8c88-4ce98162fdc0","854ceb25-7e1d-492d-82a2-a73d785611f7","Evernote",       "View your notes on Focals.","https://assets.brandfolder.com/pdfgvi-g314ds-23ehtb/original/App_icon-circle.png",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=a689e3c9-59f6-4320-8c88-4ce98162fdc0","https://cloud.bynorth.com/v1/integration/disable?integrationId=a689e3c9-59f6-4320-8c88-4ce98162fdc0","","",false,null,null,null));
            ////a.put(makeIntegration("6b771546-e8d4-4102-9b1d-8ca7f946b49c","854ceb25-7e1d-492d-82a2-a73d785611f7","Focals Todoist", "Stay on top of your to-dos from Todoist on Focals","https://bloghubstaffcom.lightningbasecdn.com/wp-content/uploads/2017/10/Todoist-main_logo_positive-300x300.png",false,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=6b771546-e8d4-4102-9b1d-8ca7f946b49c","https://cloud.bynorth.com/v1/integration/disable?integrationId=6b771546-e8d4-4102-9b1d-8ca7f946b49c","","Stay on top of your to-dos from Todoist on Focals.\n\nNOTE: This app was not created by, affiliated with, or supported by Doist.",false,null,null,null));
            ////a.put(makeIntegration("9f8e88a7-097e-4c85-b1be-4808bddf3176","854ceb25-7e1d-492d-82a2-a73d785611f7","OneNote",        "List and see your OneNote on Focals.","https://images-na.ssl-images-amazon.com/images/I/21PvrNoucFL._SY355_.png",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=9f8e88a7-097e-4c85-b1be-4808bddf3176","https://cloud.bynorth.com/v1/integration/disable?integrationId=9f8e88a7-097e-4c85-b1be-4808bddf3176","","",false,null,null,null));
            ////a.put(makeIntegration("b7eed5df-45c2-448b-9ad2-6dfd341e40cf","854ceb25-7e1d-492d-82a2-a73d785611f7","Google Tasks",   "Manage your Google Tasks from Focals.","https://www.androidpolice.com/wp-content/cache/wp-appbox/a32f0c784daf6ebdee8d1d2aa88cf1b5/ai-ad558b8d3a18effc498879622379f140",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=b7eed5df-45c2-448b-9ad2-6dfd341e40cf","https://cloud.bynorth.com/v1/integration/disable?integrationId=b7eed5df-45c2-448b-9ad2-6dfd341e40cf","","",false,null,null,null));
            ////a.put(makeIntegration("873d95ce-8614-4d19-84d2-6168c2fdb154","854ceb25-7e1d-492d-82a2-a73d785611f7","Google Fit",     "See your physical activity on your Focals.","https://gstatic.com/images/branding/product/1x/gfit_512dp.png",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=873d95ce-8614-4d19-84d2-6168c2fdb154","https://cloud.bynorth.com/v1/integration/disable?integrationId=873d95ce-8614-4d19-84d2-6168c2fdb154","","",false,null,null,null));
            ////a.put(makeIntegration("7ddd21d7-f824-4e2e-8b79-81ad8d022ad0","854ceb25-7e1d-492d-82a2-a73d785611f7","Drink Water",    "Drink water reminders","https://cdn.iconscout.com/icon/premium/png-256-thumb/drinking-water-568666.png",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=7ddd21d7-f824-4e2e-8b79-81ad8d022ad0","https://cloud.bynorth.com/v1/integration/disable?integrationId=7ddd21d7-f824-4e2e-8b79-81ad8d022ad0","","",false,null,null,null));
            ////a.put(makeIntegration("56ee8339-7eb0-431c-bbc0-de17b49f19a5","854ceb25-7e1d-492d-82a2-a73d785611f7","Twitter",        "Twitter threads for your Focals!","https://icon-library.net/images/twitter-icon-images/twitter-icon-images-12.jpg",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=56ee8339-7eb0-431c-bbc0-de17b49f19a5","https://cloud.bynorth.com/v1/integration/disable?integrationId=56ee8339-7eb0-431c-bbc0-de17b49f19a5","",null,false,null,null,null));


            //h.sendContent(a.toString());
            //Log.i(TAG, "SENDING INTEGRATION TABLE: " + a.toString());
            //h.finishResponse();
        } else {
            String uid = path.substring(RESP_PATH.length());

            JSONObject req = null;
            if (postdata != null) {
                req = new JSONObject(postdata.readUtf8());
            }

            IntegrationResponse resp = null;
            IntegrationInfo handler = integrations_.get(uid);
            if (handler != null) {
                resp = handler.handler.handleRequest(method, uid, params, headers, req);
            }


            //if ((postdata != null) && (uid != null) && !uid.isEmpty()) {
            //    JSONObject req = new JSONObject(postdata.readUtf8());
            //    String actionId = req.getString("actionId");
            //    if (actionId != null) {
            //        String[] parts = actionId.split(":");
            //        if (parts.length > 1) {
            //            IntegrationInfo handler = integrations_.get(parts[0]);
            //            if (handler != null) {
            //                resp = handler.handler.handleRequest(method, uid, params, headers, req);
            //            }
            //        }
            //    }
            //}

            if (resp == null) {
                resp = makeError();
            }


            h.sendResponse(resp.response_code);
            h.sendHeader("Content-Type", "application/json; charset=utf-8");
            h.finishHeaders();
            if (resp.response != null) {
                h.sendContent(resp.response.toString());
            }
            h.finishResponse();
        }
    }
}
