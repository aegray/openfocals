package com.openfocals.services.network.cloudintercept.integrations;

import android.util.Log;

import com.openfocals.services.network.cloudintercept.CloudIntegrationHandler;
import com.openfocals.services.network.cloudintercept.CloudMockService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NotesIntegration extends CloudIntegrationHandler.CloudIntegration {

     
    private static final String INTEGRATION_ID = "a689e3c9-59f6-4320-8c88-4ce98162fdc0";
    
    private static final String INTEGRATION_SPEC = "{\"functions\":{\"get_projects\":{\"url\":\"https://" +
            CloudMockService.CLOUD_HOSTNAME + "/v1/integration/respond/b7eed5df-45c2-448b-9ad2-6dfd341e40cf\",\"method\":\"POST\"," +
            "\"body\":{\"actionId\":\"tasks:get_projects\"}},\"get_tasks\":{\"url\":\"https://" + CloudMockService.CLOUD_HOSTNAME +
            "/v1/integration/respond/b7eed5df-45c2-448b-9ad2-6dfd341e40cf\",\"method\":\"POST\",\"body\":{\"actionId\":\"tasks:get_tasks\"}}," +
            "\"create\":{\"url\":\"https://" + CloudMockService.CLOUD_HOSTNAME + "/v1/integration/respond/b7eed5df-45c2-448b-9ad2-6dfd341e40cf\"," +
            "\"method\":\"POST\",\"body\":{\"actionId\":\"tasks:create\"}},\"check\":{\"url\":\"https://" + CloudMockService.CLOUD_HOSTNAME +
            "/v1/integration/respond/b7eed5df-45c2-448b-9ad2-6dfd341e40cf\",\"method\":\"POST\",\"body\":{\"actionId\":\"tasks:check\"}}," +
            "\"uncheck\":{\"url\":\"https://" + CloudMockService.CLOUD_HOSTNAME + "/v1/integration/respond/b7eed5df-45c2-448b-9ad2-6dfd341e40cf\"," +
            "\"method\":\"POST\",\"body\":{\"actionId\":\"tasks:uncheck\"}}},\"actions\":[]}";
    private static final String TAG = "FOCALS_NOTES";

    ////a.put(makeIntegration("dfdfb243-670a-4030-9c7d-8fabef045a67","854ceb25-7e1d-492d-82a2-a73d785611f7","Pushbullet",     "Share anything with your Focals from any other device!","http://www.stickpng.com/assets/images/58481546cef1014c0b5e496a.png",false,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=dfdfb243-670a-4030-9c7d-8fabef045a67","https://cloud.bynorth.com/v1/integration/disable?integrationId=dfdfb243-670a-4030-9c7d-8fabef045a67","","",false,null,null,null));
    ////a.put(makeIntegration("a689e3c9-59f6-4320-8c88-4ce98162fdc0","854ceb25-7e1d-492d-82a2-a73d785611f7","Evernote",       "View your notes on Focals.","https://assets.brandfolder.com/pdfgvi-g314ds-23ehtb/original/App_icon-circle.png",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=a689e3c9-59f6-4320-8c88-4ce98162fdc0","https://cloud.bynorth.com/v1/integration/disable?integrationId=a689e3c9-59f6-4320-8c88-4ce98162fdc0","","",false,null,null,null));
    ////a.put(makeIntegration("9f8e88a7-097e-4c85-b1be-4808bddf3176","854ceb25-7e1d-492d-82a2-a73d785611f7","OneNote",        "List and see your OneNote on Focals.","https://images-na.ssl-images-amazon.com/images/I/21PvrNoucFL._SY355_.png",true,"https://cloud.bynorth.com/v1/integration/enable-proxy?integrationId=9f8e88a7-097e-4c85-b1be-4808bddf3176","https://cloud.bynorth.com/v1/integration/disable?integrationId=9f8e88a7-097e-4c85-b1be-4808bddf3176","","",false,null,null,null));


    public void register(CloudMockService h) {
        h.getIntegrations().registerIntegration(INTEGRATION_ID, this, "Evernote", "View notes on Focals", null);
    }

    private static String buildIntegrationSpec(String id) {
        return "{\"functions\":{\"get_list\":{\"url\":\"https://" + CloudMockService.CLOUD_HOSTNAME + "/v1/integration/respond/" + 
            id + "\",\"method\":\"POST\",\"body\":{\"actionId\":\"notes:get_list\",\"maxNotes\":10}},\"get_detail\":{\"url\":\"" + 
            "https://" + CloudMockService.CLOUD_HOSTNAME + "/v1/integration/respond/" + id + "\",\"method\":\"POST\",\"body\":{\"actionId\":" + 
            "\"notes:get_detail\"}},\"search\":{\"url\":\"https://" + CloudMockService.CLOUD_HOSTNAME + "/v1/integration/respond/" + id + 
            "\",\"method\":\"POST\",\"body\":{\"actionId\":\"notes:search\",\"maxNotes\":10}}},\"actions\":[]}";
    }



    // {"actionId":"tasks:create","projectId":"2058959526","text":"test 3"}
    // {"id":"4064689420","title":"test 3","position":6,"status":"","functions":{"check":{"body":{"projectId":"2058959526","taskId":"4064689420"}}}}

    class Content {
        public String type;
        public String text;

        public Content(String type_, String text_) { 
            type = type_;
            text = text_;
        }
    };

    class Note {
        public String id;
        public String title;
        public int updateDate = 0;
        public String summary;
        public ArrayList<Content> content = new ArrayList<>();
        
        JSONObject getJsonSummaryObj() {
            try {
                JSONObject o = new JSONObject();
                o.put("id", id);
                o.put("title", title);
                o.put("updateDate", updateDate);
                o.put("summary", summary);
                o.put("actions", new JSONArray());
                
                JSONObject funs = new JSONObject();
                JSONObject cfun = new JSONObject();
                JSONObject body = new JSONObject();
                body.put("actionId", "notes:get_detail");
                body.put("noteId", id);
                cfun.put("body", body);
                funs.put("get_detail", cfun);
                o.put("functions", funs);
                return o;
            } catch (JSONException e) {
                throw new RuntimeException("Json failed");
            }
        }
        
        JSONObject getJsonObj() {
            try {
                JSONObject o = new JSONObject();
                o.put("id", id);
                o.put("title", title);
                o.put("updateDate", updateDate);
                JSONArray cs = new JSONArray();

                for (Content c : content) {
                    JSONObject csub = new JSONObject();
                    csub.put("type", c.type);
                    csub.put("text", c.text);
                    cs.put(csub);
                }

                o.put("content", cs);
                return o;
            } catch (JSONException e) {
                throw new RuntimeException("Json failed");
            }
        }
    };
        
    
    HashMap<String, Note> notes_ = new HashMap<>();

    int onid = 1;


    public JSONArray getNotesSummaryArr() {
        JSONArray a = new JSONArray();
        for (Note p : notes_.values()) {
            a.put(p.getJsonSummaryObj());
        }
        return a;
    }
    public void addNote(Note n) {
        notes_.put(n.id, n);
    }

    public Note nextNote() {
        Note n = new Note();
        n.id = Integer.toString(onid);
        onid += 1;
        return n;
    }

    public NotesIntegration() {

        Note p = nextNote();
        p.title = "Test note 1";
        p.summary = "Summary test 1";
        p.content.add(new Content("header", "Header test 1"));
        p.content.add(new Content("text", "Text test 1"));
        p.content.add(new Content("text", "Text2 test 1"));
        addNote(p);
        
        p = nextNote();
        p.title = "Test note 2";
        p.summary = "Summary test 2";
        p.content.add(new Content("header", "Header test 2"));
        p.content.add(new Content("text", "Text test 2"));
        p.content.add(new Content("text", "Text2 test 2"));
        addNote(p);

        p = nextNote();
        p.title = "Test note 3";
        p.summary = "Summary test 3";
        p.content.add(new Content("header", "Header test 3"));
        p.content.add(new Content("text", "Text test 3"));
        p.content.add(new Content("text", "Text2 test 3"));
        addNote(p);
    }


    public CloudIntegrationHandler.IntegrationResponse handleRequest(String method, String id, Map<String, String> params, Map<String, String> headers, JSONObject req) throws Exception {
        String action = req.getString("actionId");

        String response = null;
        Log.i(TAG, "Get task action (" + id + "): " + action + " : " + req.toString());
        if (!id.equals(INTEGRATION_ID)) {
            return new CloudIntegrationHandler.IntegrationResponse(404);
        }

        if (action.equals("notes:get_spec")) {
            Log.i(TAG, "Sending notes spec");
            return new CloudIntegrationHandler.IntegrationResponse(buildIntegrationSpec(id));
        } else {
            if (action.equals("notes:get_list")) {
                Log.i(TAG, "Sending notes list: " + getNotesSummaryArr().toString());
                return new CloudIntegrationHandler.IntegrationResponse(getNotesSummaryArr().toString());
            } else if (action.equals("notes:get_detail")) {
                Note n = notes_.get(req.getString("noteId"));
                if (n != null) {
                    Log.i(TAG, "Getting note detail: " + n.getJsonObj().toString());
                    return new CloudIntegrationHandler.IntegrationResponse(n.getJsonObj().toString());
                } else {
                    Log.e(TAG, "Failed to get task list");
                    return new CloudIntegrationHandler.IntegrationResponse("{\"ok\":false,\"name\":\"Error\",\"message\":\"Note not found\"}");
                }

            } else if (action.equals("notes:search")) {
                // for now return empty array
                Log.i(TAG, "Got notes search req - unimplemented: " + req.toString());
                return new CloudIntegrationHandler.IntegrationResponse("[]");

            } else {
                Log.e(TAG, "Unknown action");
                return new CloudIntegrationHandler.IntegrationResponse(
                        "{\"ok\":false,\"name\":\"ForbiddenError\",\"message\":\"Forbidden\"}"
                );
            }
        }
    }




}
