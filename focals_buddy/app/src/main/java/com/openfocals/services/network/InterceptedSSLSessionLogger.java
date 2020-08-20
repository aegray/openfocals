package com.openfocals.services.network;

import android.util.Log;

import com.openfocals.commutils.ssl.SSLInterceptDataHandler;
import com.openfocals.commutils.ssl.SSLServerDataHandler;
import com.openfocals.services.network.InterceptedNetworkServiceManager.InterceptedNetworkSession;

import java.security.spec.ECField;

import okio.Buffer;

public class InterceptedSSLSessionLogger implements InterceptedNetworkServiceManager.InterceptedNetworkSessionFactory
{
    public static final String TAG = "FOCALS_SSLLOG";
    class InterceptedSSLSession extends InterceptedNetworkSession implements SSLServerDataHandler.IDataSender {
        SSLServerDataHandler data_;

        InterceptedSSLSession() throws Exception {
            data_ = SSLInterceptDataHandler.createBroadInterceptSSLHandler();
            data_.setSender(this);
        }

        public void onOpen() {
        }

        public void onData(Buffer b) {
            Buffer b2 = b.clone();
            Log.i(TAG, "SSL Session got net data from focals: " + b2.toString());
            try {
                Buffer rdata = data_.read(b.clone());


                if (rdata.size() > 0) {
                    Log.i(TAG, "SSL Session read: " + rdata.toString());
                    data_.write(rdata);
                    //System.out.println("SSL Session read: " + rdata.toString());
                }
            } catch (Exception e) {
                Log.i(TAG, "SSL Session read failed");
            }
        }

        public void onError() {

        }

        public void onClose() {

        }

        @Override
        public void sendData(Buffer b) {
            Buffer b2 = b.clone();

            System.out.println("Sending net data to focals: " + b2.toString());

            super.sendData(b.clone());
        }
    }



    @Override
    public InterceptedNetworkSession createSession() throws Exception {
        return new InterceptedSSLSession();
    }
}
