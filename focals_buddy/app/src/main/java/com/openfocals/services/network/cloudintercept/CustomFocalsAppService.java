package com.openfocals.services.network.cloudintercept;

import android.content.Context;
import android.util.Log;

import com.openfocals.commutils.ssl.SSLInterceptDataHandler;
import com.openfocals.commutils.ssl.SSLServerDataHandler;
import com.openfocals.services.network.HTTPEndpointHandler;
import com.openfocals.services.network.HTTPHandler;
import com.openfocals.services.network.InterceptedNetworkServiceManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okio.Buffer;

public class CustomFocalsAppService implements InterceptedNetworkServiceManager.InterceptedNetworkSessionFactory {

    private static final String TAG = "FOCALS_APPS";
    private static final String ROOT_PATH = "/ofocals/apps";

    static CustomFocalsAppService instance_;
    public static CustomFocalsAppService getInstance() { return instance_; }

    boolean enabled_ = false;
    int update_seq_ = 0;

    HTTPEndpointHandler file_server_ = new HTTPEndpointHandler();




    public static class AppDefinition {
        public String name;
        public String description;
        public boolean is_enabled = false;
        public String qmldata;

        public AppDefinition(String name, String description, boolean is_enabled, String qmldata) {
            this.name = name;
            this.description = description;
            this.is_enabled = is_enabled;
            this.qmldata = qmldata;
        }

        public AppDefinition copy() {
            return new AppDefinition(name, description, is_enabled, qmldata);
        }
    }

    class FileServerSession
            extends InterceptedNetworkServiceManager.InterceptedNetworkSession {
        HTTPEndpointHandler.HTTPEndpointHandlerSession reqhandler_ = file_server_.getSessionHandler();


        FileServerSession() throws Exception {
            reqhandler_.setSender(new HTTPHandler.HTTPHandlerSender() {
                @Override
                public void sendData(Buffer b) throws Exception {
                    Log.i(TAG, "FileServerSession sending http response data: " + b.clone().toString());
                    FileServerSession.super.sendData(b.clone());
                }

                @Override
                public void close() throws Exception {
                    Log.i(TAG, "FileServerSession closing socket");
                    FileServerSession.super.close();
                }
            });
        }

        public void onOpen() {
        }

        public void onData(Buffer b) {
            // this is a super hacky http parser - for now I'm just adding to a buffer until
            // I see \r\n\r\n at the end, then parsing the http path + headers
            Log.d(TAG, "FileServerSession got data from focals: " + b.clone());

            try {
                reqhandler_.onData(b.clone());
            } catch (Exception e) {
                e.printStackTrace();
                close();
            }
        }

        public void onError() {

        }

        public void onClose() {

        }

        @Override
        public void sendData(Buffer b) {
            Buffer b2 = b.clone();
            Log.d(TAG, "FileServerSesssion sending data to focals: " + b2.toString());
            super.sendData(b.clone());
        }
    }


    // helper to read a qml
    public static String readRawTextFile(Context ctx, int resId)
    {
        InputStream inputStream = ctx.getResources().openRawResource(resId);

        InputStreamReader inputreader = new InputStreamReader(inputStream);
        BufferedReader buffreader = new BufferedReader(inputreader);
        String line;
        StringBuilder text = new StringBuilder();

        try {
            while (( line = buffreader.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
        } catch (IOException e) {
            return null;
        }
        return text.toString();
    }

    HashMap<String, AppDefinition> apps_ = new HashMap<>();

    public List<AppDefinition> getApps() {
        ArrayList<AppDefinition> apps = new ArrayList<>();
        for (AppDefinition a : apps_.values()) {
            apps.add(a.copy());
        }
        apps.sort(new Comparator<AppDefinition>() {
            @Override
            public int compare(AppDefinition o1, AppDefinition o2) {
                return o1.name.compareTo(o2.name);
            }
        });
        return apps;
    }


    HTTPEndpointHandler.HTTPEndpoint endpoint = new HTTPEndpointHandler.HTTPEndpoint() {
        public boolean shouldHandle(String path) {
            return path.startsWith(ROOT_PATH);
        }

        public void handle(HTTPHandler h, String method, String path, Map<String, String> params, Map<String, String> headers, Buffer postdata) throws Exception
        {
            // for now if we get any request that matches, we'll enable
            enabled_ = true;

            String s = "Apps got request: method=" + method + " path=" + path;
            for (Map.Entry<String, String> e : params.entrySet()) {
                s += "\n    param: " + e.getKey() + "=" + e.getValue();
            }
            Log.i(TAG, s);

            if (path.equals(ROOT_PATH + "/enable")) {
                h.sendResponse(200);
                h.sendHeader("Content-Type", "application/json; charset=utf-8");
                h.finishHeaders();
                h.sendContent("{}");
                h.finishResponse();

                Log.i(TAG, "Enabling custom applications");
                enabled_ = true;
            } else if (path.equals(ROOT_PATH + "/list")) {
                h.sendResponse(200);
                h.sendHeader("Content-Type", "application/json; charset=utf-8");
                h.finishHeaders();
                JSONArray a = new JSONArray();
                for (AppDefinition app : apps_.values()) {
                    if (app.is_enabled) {
                        a.put(new JSONObject().put("name", app.name).put("description", app.description));
                    }
                }
                h.sendContent(a.toString());
                Log.i(TAG, "Sending apps list: " + a.toString());
                h.finishResponse();
            } else if (path.equals(ROOT_PATH + "/impl")) {

                String name = params.get("name");

                boolean handled = false;
                if (name != null) {
                    AppDefinition app = apps_.get(name);
                    if (app != null) {
                        handled = true;
                        h.sendResponse(200);
                        h.finishHeaders();
                        h.sendContent(app.qmldata);
                        Log.i(TAG, "Sending qml impl: " + app.qmldata.length());
                        h.finishResponse();
                    }
                }

                if (!handled) {
                    Log.i(TAG, "Unknown app: " + name);
                    h.sendResponse(404);
                    h.finishHeaders();
                    h.finishResponse();
                }
            } else {
                h.sendResponse(404);
                Log.i(TAG, "Unknown request");
                h.finishHeaders();
                h.finishResponse();
            }
        }
    };


    public void updateAppsEnabled(List<AppDefinition> apps) {
        for (AppDefinition a : apps) {
            AppDefinition d = apps_.get(a.name);
            if (d != null) {

                if (d.is_enabled != a.is_enabled) {
                    Log.i(TAG, "Updating enabled status of app: name=" + a.name + " new_enabled=" + a.is_enabled);
                }
                d.is_enabled = a.is_enabled;
            }
        }
    }


    public boolean appsEnabled() { return enabled_; }

    public void registerApplication(String name, String description, String qmlimpl) {
        apps_.put(name, new AppDefinition(name, description, true, qmlimpl));
    }


    public void register(CloudMockService s) {
        s.getHttpEndpoints().registerEndpoint(endpoint);
    }

    public void register(InterceptedNetworkServiceManager s) {
        s.registerServiceForDomain("app.ofocals.com", this);
    }

    @Override
    public InterceptedNetworkServiceManager.InterceptedNetworkSession createSession() throws Exception {
        return new FileServerSession();
    }


    public CustomFocalsAppService() {
        instance_ = this;
        file_server_.registerEndpoint(endpoint);
    }
}
