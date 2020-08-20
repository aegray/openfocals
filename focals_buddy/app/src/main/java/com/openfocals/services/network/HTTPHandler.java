
package com.openfocals.services.network;
//import android.util.Log;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import okio.Buffer;
import okio.ByteString;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;


public class HTTPHandler {
    private static final String STR_CONTENT_LENGTH = "content-length";
    private static final String STR_CONTENT_TYPE = "content-type";
    private static final String STR_CONNECTION = "connection";
    private static final String STR_KEEP_ALIVE = "keep-alive";

    private static final String TAG = "FOCALS_HTTP";
    private static final ByteString crcr = ByteString.of(
            (byte)'\r',
            (byte)'\n',
            (byte)'\r',
            (byte)'\n'
        );


    int waiting_content_ = 0;
    boolean waiting_headers_ = true;

    boolean keep_alive_ = false;

    String method_ = "";
    String path_ = "";

    Buffer request = new Buffer();
    HashMap<String, String> params_ = new HashMap<>();
    HashMap<String, String> headers_ = new HashMap<>();

    Buffer resp_out_ = new Buffer();
    Buffer content_out_ = new Buffer();
    int finish_header_off_ = -1;

    public interface HTTPHandlerSender {
        public void sendData(Buffer b) throws Exception;
        public void close() throws Exception;
    }

    HTTPHandlerSender sender_;

    public HTTPHandler() { }

    public void setSender(HTTPHandlerSender s) { sender_ = s; }
    public HTTPHandlerSender getSender() { return sender_; }

    // override
    //
    public void printRequest() {
        System.out.println("\n\nHandling request: " + method_ + " : " + path_ + " : ");
        for (Map.Entry<String, String> p : params_.entrySet()) {
            System.out.println("\n\n    param: " + p.getKey() + " = " + p.getValue());
        }
        System.out.println("\n");
        for (Map.Entry<String, String> p : headers_.entrySet()) {
            System.out.println("\n\n    header: " + p.getKey() + " = " + p.getValue());
        }
    }

    public void handleRequest(String method, String path, Map<String, String> params, Map<String, String> headers, Buffer postdata) throws Exception {
        printRequest();
        if (postdata != null) {
            System.out.println("\n Postdata: " + postdata.toString());
        }
    }


    private void resetQueryState() {
        method_ = "";
        path_ = "";
        headers_ = new HashMap<>();
        params_ = new HashMap<>();
        resp_out_ = new Buffer();
        content_out_ = new Buffer();
        finish_header_off_ = -1;
        keep_alive_ = false;

        waiting_headers_ = true;
        waiting_content_ = 0;
    }
                
    private void sendServerError() {
        resp_out_ = new Buffer();
        content_out_ = new Buffer();
        finish_header_off_ = -1;

        try {
            sendResponse(500);
            finishHeaders();
            finishResponse();
            close();
        } catch (Exception ex) {
            Log.e(TAG, "Failed to send server error");
        }
    }

    private void doHandleRequest(String method, String path, Map<String, String> params, Map<String, String> headers, Buffer postdata) {
        try {
            handleRequest(method, path, params, headers, postdata);
        } catch (Exception e) {
            Log.e(TAG, "Got exception in handleRequest: " + e.toString());
            e.printStackTrace();
            try {
                sendServerError();
            } catch (Exception e2) {
                Log.e(TAG, "Failed to send data for server error");
            }
        }
        resetQueryState();
    }


    // parse a get string or post data if content type is form encoded
    private void parseFormParams(String s) {
        String[] parts = s.split("[&]");
        for (String p : parts) {
            String[] subparts = p.split("[=]");

            if (subparts.length == 2) {
                try {
                    params_.put(subparts[0], URLDecoder.decode(subparts[1], StandardCharsets.UTF_8.name()));
                } catch (UnsupportedEncodingException ex) {
                    Log.e(TAG, "Error parsing form param: " + p);
                }
            } else {
                Log.e(TAG, "Error parsing form param: " + p);
            }
        }
    }
    
    private void parseFormParams(Buffer b) {
        // Doing this the hackiest / most inefficient way possible - rewrite this if any 
        // performance concerns.  I don't know java very well so I'm just pulling everything 
        // out to a string so i can split
        String s = b.readString(StandardCharsets.UTF_8);
        parseFormParams(s);
    }

    private void parsePostData(Buffer b) {
        String c = headers_.get(STR_CONTENT_TYPE);
        if (c != null) {
            if (c.equals("application/x-www-form-urlencoded")) {
                parseFormParams(b);
            }
        }
    }

    private void processHeaders() throws IOException {
        if (request.indexOf(crcr) >= 0) {
            // process request
            String l;

            int onstate = 0;

            method_ = "";
            path_ = "";
            headers_ = new HashMap<>();
            params_ = new HashMap<>();
            while ((l = request.readUtf8Line()) != null) {
                if (l.equals("")) break;  
                if (onstate == 0) {
                    String[] parts = l.split(" ");
            
                    if (parts.length >= 2) {
                        method_ = parts[0];
                        path_ = parts[1];
                    }
                    onstate = 1;
                } else {
                    int i = l.indexOf(":");

                    if (i >= 0) {
                        String key = l.substring(0, i).toLowerCase();
                        String value = l.substring(i+2);

                        headers_.put(key, value);

                        if (key.equals(STR_CONNECTION) && value.toLowerCase().equals(STR_KEEP_ALIVE)) {
                            keep_alive_ = true;
                        }
                    } else {
                        Log.e(TAG, "Got unknown line in http headers: " + l);
                    }
                }
            }

            String[] parts = path_.split("\\?");

            if (parts.length > 0)
            {
                path_ = parts[0];
                for (int i = 1; i < parts.length; i++) {
                    parseFormParams(parts[i]);
                }
            }

            
            if (headers_.get(STR_CONTENT_LENGTH) == null) {
                Log.i(TAG, "No content request, handling");
                doHandleRequest(method_, path_, params_, headers_, null);
            } else {
                waiting_headers_ = false;
                try {
                    waiting_content_ = Integer.parseInt(headers_.get(STR_CONTENT_LENGTH));
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing content length: " + headers_.get(STR_CONTENT_LENGTH));
                    sendServerError();
                }
            }
        }
    }

    public void processContent() throws IOException {
        if (request.size() >= waiting_content_) {
            Buffer data = new Buffer();
            data.write(request, waiting_content_);
            parsePostData(data);
            doHandleRequest(method_, path_, params_, headers_, data);
        }
    }

    private void process() throws IOException {
        if (waiting_headers_) {
            processHeaders();
        }

        if (waiting_content_ > 0) {
            processContent();
        }
    }

    public void onData(Buffer b) throws IOException {
        request.writeAll(b);
        process();
    }


    public void sendResponse(int code) throws Exception {

        resp_out_.writeUtf8("HTTP/1.0 " + code + " ");
        
        switch (code) {
        case 200: resp_out_.writeUtf8("OK"); break;
        case 201: resp_out_.writeUtf8("Created"); break;
        case 301: resp_out_.writeUtf8("Moved permanently"); break;
        case 304: resp_out_.writeUtf8("Not modified"); break;
        case 307: resp_out_.writeUtf8("Temporary redirect"); break;
        case 400: resp_out_.writeUtf8("Bad request"); break;
        case 401: resp_out_.writeUtf8("Unauthorized"); break;
        case 403: resp_out_.writeUtf8("Forbidden"); break;
        case 404: resp_out_.writeUtf8("Not found"); break;
        case 405: resp_out_.writeUtf8("Method not allowed"); break;
        case 408: resp_out_.writeUtf8("Request timeout"); break;
        case 411: resp_out_.writeUtf8("Length required"); break;
        case 501: resp_out_.writeUtf8("Not implemented"); break;
        case 502: resp_out_.writeUtf8("Bad gateway"); break;
        case 503: resp_out_.writeUtf8("Service unavailable"); break;
       
        // passthrough 
        case 500: 
        default:
            resp_out_.writeUtf8("Internal server error"); 
            break;
        }
        resp_out_.writeUtf8("\r\n");
    }
    
    public void sendHeader(String key, String value) throws Exception {
        resp_out_.writeUtf8(key + ": " + value + "\r\n");
    }
    
    public void finishHeaders() throws Exception {
        if (finish_header_off_ < 0) {
            finish_header_off_ = (int)resp_out_.size();
        }
    }

    public void sendContent(String data) throws Exception {
        finishHeaders();
        content_out_.writeUtf8(data);
    }
    
    public void sendContent(Buffer data) throws Exception {
        finishHeaders();
        content_out_.writeAll(data); 
    }


    
    private void internalSendData(Buffer data) throws Exception {
        if (sender_ != null) {
            sender_.sendData(data);
        }
    }


    public void finishResponse() throws Exception {
        internalSendData(resp_out_);
        Buffer b = new Buffer();
        b.writeUtf8(STR_CONTENT_LENGTH + ": " + content_out_.size() + "\r\n");
        if (keep_alive_) {
            b.writeUtf8(STR_CONNECTION + ": " + STR_KEEP_ALIVE + "\r\n");
        } 
        b.writeUtf8("\r\n");
        internalSendData(b);
        internalSendData(content_out_);

        if (!keep_alive_) {
            close(); 
        }

        resetQueryState();
    }



    public void close() {
        try {
            if (sender_ != null) {
                sender_.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to close HTTPHandler");
        }
    }
}




