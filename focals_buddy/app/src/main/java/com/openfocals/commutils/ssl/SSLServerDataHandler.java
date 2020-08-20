package com.openfocals.commutils.ssl;

import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSession;

import okio.Buffer;

public class SSLServerDataHandler {
    private static final String TAG = "FOCALS_SSL";

    SSLContext ctx_;

    SSLEngine server_engine_;

    ByteBuffer server_app_in_;
    ByteBuffer server_app_out_;

    ByteBuffer server_net_in_;
    ByteBuffer server_net_out_;


    public interface IDataSender {
        public abstract void sendData(Buffer b);
    }

    IDataSender sender_;


    public SSLServerDataHandler(String privkey, String cert) throws Exception {
        try {
            ctx_ = PEMImporter.createSSLContextForStrings(privkey, cert, "");

        } catch (Exception e) {
            throw e;
            //throw new RuntimeException("Could not load ssl key+cert");
        }
        server_engine_ = ctx_.createSSLEngine();
        server_engine_.setUseClientMode(false);
        server_engine_.setNeedClientAuth(false);

        SSLSession session = server_engine_.getSession();
        int appBufferMax = session.getApplicationBufferSize();
        int netBufferMax = session.getPacketBufferSize();


        // everything starts in write mode, expected to be there by default
        server_app_in_ = ByteBuffer.allocate(appBufferMax + 50);
        server_app_out_ = ByteBuffer.allocate(appBufferMax + 50);

        server_net_in_ = ByteBuffer.allocate(netBufferMax);
        server_net_out_ = ByteBuffer.allocate(netBufferMax);
    }

    public void setSender(IDataSender sender) {
        sender_ = sender;
    }


    // helpers
    private static SSLEngineResult.HandshakeStatus runDelegatedTasks(SSLEngineResult result,
                                                                     SSLEngine engine) throws Exception {

        if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
            Runnable runnable;
            while ((runnable = engine.getDelegatedTask()) != null) {
                runnable.run();
            }
            SSLEngineResult.HandshakeStatus hsStatus = engine.getHandshakeStatus();
            if (hsStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                throw new Exception(
                        "handshake shouldn't need additional tasks");
            }
            return hsStatus;
        }
        return result.getHandshakeStatus();
    }

    private static boolean isEngineClosed(SSLEngine engine) {
        return (engine.isOutboundDone() && engine.isInboundDone());
    }


    // read net data -> return app data
    public Buffer read(Buffer b) throws Exception {
        // probably crazy inefficient
        b.read(server_net_in_);

        process();

        Buffer bout = new Buffer();
        server_app_in_.flip();
        bout.write(server_app_in_);
        server_app_in_.compact();

        return bout;
    }
    
    // write app data
    public void write(Buffer b) throws Exception {
        System.out.println("Sending app data: " + b.clone().toString());
        b.read(server_app_out_);
        process();
    }


    private void sendData(ByteBuffer data) throws IOException {

        Buffer b = new Buffer();

        b.write(data);
        if (sender_ != null) {
            //while (b.size() > 512) {
            //    Buffer b2 = new Buffer();
            //    b2.write(b, 512);
            //    sender_.sendData(b2);

            //}
            sender_.sendData(b);
        }

    }


    private void process() throws Exception {

        boolean need_run = true;

        SSLEngineResult.HandshakeStatus hs;
        int i = 0;

        while (need_run || (i < 4)) {
            need_run = false;

            server_app_out_.flip();
            SSLEngineResult result = server_engine_.wrap(server_app_out_, server_net_out_);
            hs = runDelegatedTasks(result, server_engine_);
            server_app_out_.compact();
            
            need_run |= (hs == SSLEngineResult.HandshakeStatus.NEED_WRAP); 


            // write to client
            if (result.bytesProduced() > 0) {
                server_net_out_.flip();
                sendData(server_net_out_);
                server_net_out_.compact();
            }

            // write to server
            server_net_in_.flip();
            int lim = server_net_in_.limit(); 
            result = server_engine_.unwrap(server_net_in_, server_app_in_);
            hs = runDelegatedTasks(result, server_engine_);
            server_net_in_.compact();

            Log.d(TAG, "Handshake state: " + hs + " need_run=" + need_run + " consumed=" + result.bytesConsumed() + " lim=" + lim);

            // this probably isn't right (or is overly verbose), but it seems to work
            //need_run |= (result.bytesConsumed() < lim) || (hs == SSLEngineResult.HandshakeStatus.NEED_WRAP);
            need_run |= ((hs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) && (result.bytesConsumed() < lim)) ||
                    (hs == SSLEngineResult.HandshakeStatus.NEED_WRAP);
            i += 1;
        }
    }
}

