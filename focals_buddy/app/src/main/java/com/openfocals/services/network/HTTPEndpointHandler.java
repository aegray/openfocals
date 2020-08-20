package com.openfocals.services.network;

//Log.e(TAG, "No endpoint registered for path: " + path);
//import android.util.Log;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okio.Buffer;

public class HTTPEndpointHandler {
    private static final String TAG = "FOCALS_HTTP";


    public static class HTTPEndpoint {
        public boolean shouldHandle(String path) { return false; }

        public void handle(HTTPHandler h, String method, String path, Map<String, String> params, Map<String, String> headers, Buffer postdata) throws Exception { }
    }

    List<HTTPEndpoint> endpoints_ = new ArrayList<>();

    public class HTTPEndpointHandlerSession extends HTTPHandler {
        @Override
        public void handleRequest(String method, String path, Map<String, String> params, Map<String, String> headers, Buffer postdata) throws Exception {
            System.out.println("\n\ngot handleRequest\n\n");
            StringBuilder b = new StringBuilder();

            b.append("Cloud got request: method=" + method + " path=" + path);
            for (Map.Entry<String, String> e : params.entrySet()) {
                b.append("\nParam: " + e.getKey() + "=" + e.getValue());
            }
            for (Map.Entry<String, String> e : headers.entrySet()) {
                b.append("\nHeader: " + e.getKey() + "=" + e.getValue());
            }

            Log.i(TAG, b.toString());


            for (HTTPEndpoint e : endpoints_) {
                if (e.shouldHandle(path)) {
                    e.handle(this, method, path, params, headers, postdata);
                    return;
                }
            }

            // Nothing found
            sendResponse(404);
            finishHeaders();
            finishResponse();
            close();
        }
    }


    public void registerEndpoint(HTTPEndpoint e) {
        endpoints_.add(e);
    }

    public HTTPEndpointHandlerSession getSessionHandler() {
        return new HTTPEndpointHandlerSession();
    }

}
