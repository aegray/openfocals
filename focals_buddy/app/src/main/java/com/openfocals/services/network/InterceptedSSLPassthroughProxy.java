package com.openfocals.services.network;

import android.os.AsyncTask;
import android.util.Log;

import com.openfocals.commutils.ssl.SSLInterceptDataHandler;
import com.openfocals.commutils.ssl.SSLServerDataHandler;
import com.openfocals.services.network.InterceptedNetworkServiceManager.InterceptedNetworkSession;

import org.apache.http.params.HttpParams;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.EventListener;
import java.util.HashMap;
import java.util.concurrent.Executor;

//import javax.net.ssl.SSLContext;
//import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okio.Buffer;
import okio.Okio;
import okio.Sink;
import okio.Source;

public class InterceptedSSLPassthroughProxy implements InterceptedNetworkServiceManager.InterceptedNetworkSessionFactory
{
    private static final int MAX_DATA_CHUNK_SIZE = 950;
    private static final String TAG = "FOCALS_SSLPROXY";

    String target_;
    int port_ = 443;

    Executor executor_;
    EventBus event_bus_ = new EventBus();

    int onid_ = 0;

    HashMap<Integer, InterceptedSSLSession> sessions_ = new HashMap<>();



    public class DataEvent {
        int id;
        Buffer data;

        public DataEvent(int id, Buffer data) {
            this.id = id;
            this.data = data;
        }
    }

    public class CloseEvent {
        int id;

        public CloseEvent(int id) {
            this.id = id;
        }
    }


    class InterceptedSSLSession extends InterceptedNetworkSession implements SSLServerDataHandler.IDataSender {
        SSLServerDataHandler data_;
        SSLSocket sock_;
        Sink data_sink_;
        Source data_source_;


        int id_ = 0;
        Buffer pending_ = new Buffer();
        boolean connected_ = false;
        boolean opened_ = false;


        private class BackgroundReader implements Runnable {
            private boolean stop_requested_ = false;

            public void run() {
                Buffer buf = new Buffer();
                try {
                    Log.i(TAG, "Started writer loop");
                    while (!stop_requested_ && data_source_.read(buf, MAX_DATA_CHUNK_SIZE) > 0) {
                        Buffer buf2 = new Buffer();
                        buf2.writeAll(buf);
                        Log.i(TAG, "Read data from socket: " + buf.clone().toString());
                        event_bus_.post(new DataEvent(id_, buf2));
                        Log.i(TAG, "Posted data event");
                    }
                    Log.i(TAG, "Done with background reader loop");
                } catch (IOException e) {
                    Log.w(TAG, "Background reader stopping due to exception: " + e.toString());
                }
                Log.d(TAG, "Done with background reader loop");
                // send socket closed
                event_bus_.post(new CloseEvent(id_));
            }

            public void requestStop() {
                stop_requested_ = true;
            }
        }

        public class ConnectTask extends AsyncTask<Void, Void, SSLSocket> {
            private String host;
            private int port;

            private ConnectTask(String h, int p) {
                host = h;
                port = p;
            }

            public SSLSocket doInBackground(Void... args) {
                try {
                    try {
                        Log.i(TAG, "Trying to connect to host=" + host + " port=" + port);
                        SSLContext context = SSLInterceptDataHandler.createBroadSSLContext();
                        SSLSocket sock = (SSLSocket)context.getSocketFactory().createSocket(host, port);
                        sock.startHandshake();
                        Log.i(TAG, "finishedHandshake");
                        return sock;
                    } catch (Exception e) {
                        Log.e(TAG, "Failed creating connecting socket for " + host + ":" + port);
                    }

                } catch (IllegalArgumentException e2) {
                    Log.e(TAG, "Failed creating connecting socket (ilarg) for " + host + ":" + port);
                }
                return null;
            }

            public void onPostExecute(SSLSocket s) {
                if (s != null) {
                    Log.i(TAG, "Opened socket connection to target");
                    // possibly send pending data
                    InterceptedSSLSession.this.onSocketOpened(s);
                } else {
                    InterceptedSSLSession.this.shutdown();
                }
            }
        }


        InterceptedSSLSession(int id) throws Exception {

            id_ = id;
            data_ = SSLInterceptDataHandler.createBroadInterceptSSLHandler();
            data_.setSender(this);
            //sock_ = new Socket();
            //Socket socket = new Socket();
        }

        public void onOpen() {
            Log.i(TAG, "onOpen");
            // open a socket
            opened_ = true;
            new ConnectTask(target_, port_).executeOnExecutor(executor_, new Void[0]);
        }

        public void onData(Buffer b) {
            Log.i(TAG, "onData: " + b.clone().toString());
            Buffer b2 = b.clone();
            try {
                Buffer rdata = data_.read(b.clone());

                if (rdata.size() > 0) {
                    Log.i(TAG, "Intercept SSL Proxy got data from glasses: " + rdata.toString());

                    socketWrite(rdata);
                    //data_.write(rdata);
                }
            } catch (Exception e) {
                Log.e(TAG, "Intercept SSL Proxy failed to read data from ssl");
                shutdown();
            }
        }


        public void shutdown() {
            Log.i(TAG, "shutdown");
            if (opened_) {
                close();
            }
            socketSafeClose();
        }

        public void onError() {
            Log.i(TAG, "onError");
            shutdown();
        }

        public void onClose() {
            Log.i(TAG, "onClose");
            opened_ = false;
            InterceptedSSLPassthroughProxy.this.onClose(id_);
        }

        public void socketWrite(final Buffer b) {
            Log.i(TAG, "socketWrite: " + b.clone().toString());
            if (!connected_) {
                try {
                    pending_.writeAll(b);
                } catch (IOException e) {
                    Log.e(TAG, "Error writing socket data");
                    shutdown();
                }
            } else {
                executor_.execute(new Runnable() {
                    final Buffer buf;

                    {
                        buf = b;
                    }

                    @Override
                    public void run() {
                        try {
                            if (data_sink_ != null) {
                                data_sink_.write(buf, buf.size());
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Failed to write to socket: " + e.toString());
                        } catch (NullPointerException e) {
                            Log.e(TAG, "Failed to write to socket: " + e.toString());
                        }
                    }
                });
            }
        }

        public void onSocketOpened(SSLSocket s) {
            Log.i(TAG, "onSocketEnd");
            sock_ = s;
            try {
                data_sink_ = Okio.sink(sock_.getOutputStream());
                data_source_ = Okio.source(sock_.getInputStream());
            } catch (Exception e) {
                Log.e(TAG, "Got error constructing streams");
                shutdown();
                return;
            }
            if (pending_.size() != 0) {
                Log.i(TAG, "Writing pending");
                socketWrite(pending_);
            }

            // start background reader
            Log.i(TAG, "Starting reader");
            new Thread(new BackgroundReader()).start();
        }

        public void onSocketData(Buffer b) {
            Log.i(TAG, "onSocketData: " + b.clone().toString());
            sendData(b);
        }

        public void onSocketClosed() {
            Log.i(TAG, "onSocketClosed");
            if (opened_) {
                close();
            }
            sock_ = null;
        }

        public void socketSafeClose() {
            Log.i(TAG, "socketSafeClose");
            if (sock_ != null) {
                try {
                    sock_.close();
                } catch (Exception ex) {

                }
                sock_ = null;
            }
        }

        @Override
        public void sendData(Buffer b) {
            Buffer b2 = b.clone();
            Log.i(TAG, "Sending net data to focals: " + b2.toString());

            //System.out.println("Sending net data to focals: " + b2.toString());

            super.sendData(b.clone());
        }
    }


    public InterceptedSSLPassthroughProxy(Executor executor, String targetHost, int targetPort) {
        executor_ = executor;
        target_ = targetHost;
        port_ = targetPort;
        event_bus_.register(this);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocketData(DataEvent d) {
        InterceptedSSLSession s = sessions_.get(d.id);
        if (s != null) {
            s.onSocketData(d.data);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocketClose(CloseEvent d) {
        InterceptedSSLSession s = sessions_.get(d.id);
        if (s != null) {
            s.onSocketClosed();
        }
    }

    public void onClose(int id) {
        if (sessions_.containsKey(id)) {
            sessions_.remove(id);
        }
    }

    @Override
    public InterceptedNetworkSession createSession() throws Exception {
        onid_ += 1;
        InterceptedSSLSession s = new InterceptedSSLSession(onid_);
        sessions_.put(onid_, s);
        return s;
    }
}
