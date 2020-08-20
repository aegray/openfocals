package com.openfocals.services.network;

import android.os.AsyncTask;
import android.util.Log;

import com.openfocals.focals.Device;
import com.openfocals.focals.events.FocalsBluetoothMessageEvent;
import com.openfocals.focals.events.FocalsDisconnectedEvent;
import com.openfocals.focals.messages.HostWhois;
import com.openfocals.focals.messages.SocketClose;
import com.openfocals.focals.messages.SocketData;
import com.openfocals.focals.messages.SocketError;
import com.openfocals.focals.messages.SocketOpen;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okio.Buffer;

public class NetworkService implements ISocketManagerListener {
    public static final String TAG = "FOCALS_NETWORK";

    static NetworkService instance_;
    public static NetworkService getInstance() { return instance_; }


    // this would be much cleaner as a singular Router that has the sasme interface for each
    // object that can handle opening sockets / sessions, but for now I bolted on the
    // internal router after the fact.  @TODO: rework
    NetworkSocketManager socks_;
    InterceptedNetworkServiceManager intercept_;

    Device device_;
    //Executor executor_ = Executors.newCachedThreadPool();
    Executor executor_; // = Executors.newFixedThreadPool(5);


    // stats
    int bytes_sent_ = 0;
    int bytes_recv_ = 0;
    int connections_open_ = 0;


    public NetworkService(Device device, Executor executor) {
        instance_ = this;

        device_ = device;
        device_.getEventBus().register(this);

        executor_ = executor;
        socks_ = new NetworkSocketManager(executor_);
        socks_.setListener(this);

        intercept_ = new InterceptedNetworkServiceManager();
        intercept_.setListener(this);
    }

    public InterceptedNetworkServiceManager interceptedNetworkServices() { return intercept_; }

    /// stats
    // everything is from focals perspective
    public int getBytesSent() { return bytes_sent_; }
    public int getBytesRecv() { return bytes_recv_; }
    public int getOpenConnections() { return connections_open_; }




    /// Utilties for interacting w glasses
    private class HostWhoisTask extends AsyncTask<Void, Void, InetAddress> {
        private String host_;
        private HostWhoisTask(String name) {
            host_ = name;
        }

        public InetAddress doInBackground(Void... args) {
            Log.i(TAG, "Running do in background in HostWhois");
            try {
                return InetAddress.getByName(this.host_);
            } catch (UnknownHostException e) {
                Log.e(TAG, "HostWhois failed : " + e.toString());
                return null;
            }
        }

        public void onPostExecute(InetAddress addr) {
            if (addr == null) {
                Log.i(TAG, "HostWhois failed");
                device_.sendHostWhoisResponse(
                        host_,
                        false,
                        null,
                        4
                );
            } else {
                Log.i(TAG, "Publishing HostWhois result: name=" + host_ + " result=" + addr.getHostAddress());
                device_.sendHostWhoisResponse(
                        host_,
                        true,
                        addr.getHostAddress(),
                        null
                );
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsDisconnected(FocalsDisconnectedEvent e) {
        socks_.reset();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsMessage(FocalsBluetoothMessageEvent e) {
        if (e.message.hasSocketOpen()) {
            SocketOpen r = e.message.getSocketOpen();
            Log.i(TAG, "Got FocalsMessage/Network: socketOpen(id=" + r.getId() + " host=" + r.getHost() + " port=" + r.getPort());
            if (intercept_.handlesHost(r.getHost())) {
                intercept_.openSocket(r.getId(), r.getHost(), r.getPort());
            } else {
                socks_.openSocket(r.getId(), r.getHost(), r.getPort());
            }
        } else if (e.message.hasSocketClose()) {
            SocketClose r = e.message.getSocketClose();
            Log.i(TAG, "Got FocalsMessage/Network: socketClose(id=" + r.getId() + ")");
            if (intercept_.handles(r.getId())) {
                intercept_.closeSocket(r.getId());
            } else {
                socks_.closeSocket(r.getId());
            }
        } else if (e.message.hasSocketData()) {
            SocketData r = e.message.getSocketData();
            Log.i(TAG, "Got FocalsMessage/Network: socketData(id=" + r.getId() + ", len=" + e.buffer.size() + ")");
            bytes_sent_ += e.buffer.size();
            if (intercept_.handles(r.getId())) {
                intercept_.socketData(r.getId(), e.buffer.clone());
            } else {
                socks_.sendData(r.getId(), e.buffer.clone());
            }
        } else if (e.message.hasSocketError()) {
            SocketError r = e.message.getSocketError();
            Log.i(TAG, "Got FocalsMessage/Network: socketError(id=" + r.getId() + ")");
            if (intercept_.handles(r.getId())) {
                intercept_.socketError(r.getId());
            } else {
                socks_.socketError(r.getId());
            }
        } else if (e.message.hasHostWhois()) {
            HostWhois r = e.message.getHostWhois();
            Log.i(TAG, "Got FocalsMessage/Network: hostWhois(host=" + r.getHost() + ")");
            // post a request
            String s = intercept_.getHostWhois(r.getHost());
            if (s != null) {
                Log.i(TAG, "Publishing internally redirected HostWhois result: name=" + r.getHost() + " result=" + s);
                device_.sendHostWhoisResponse(
                        r.getHost(),
                        true,
                        s,
                        null
                );
            } else {
                new HostWhoisTask(r.getHost()).executeOnExecutor(this.executor_, new Void[0]);
            }
        }
    }


    public void onSocketOpenResult(int id, boolean success, int error_code)
    {
        if (success) {
            connections_open_ += 1;
        }

        device_.sendSocketOpenResponse(
                id,
                success,
                success ? null : Integer.valueOf(error_code)
        );
    }

    public void onSocketError(int id, int error_code) {
        // its for general stats - don't really care about corner cases so I'm cutting corners here
        if (connections_open_ > 0) {
            connections_open_ -= 1;
        }

        Log.e(TAG, "Got socket error: id=" + id + " error=" + error_code);
        device_.sendSocketError(id, error_code);
    }

    public void onSocketData(int id, Buffer data) {
        Buffer bc = data.clone();
        Log.i(TAG, "Sending sock data to glasses: id=" + id + " len=" + bc.size());
        bytes_recv_ += data.size();
        device_.sendSocketData(id, data);
    }

    public void onSocketCloseResult(int id, boolean success, int error_code) {
        Log.i(TAG, "onSocketCloseResult: id=" + id + " success=" + success + " errcode=" + error_code);
        if (success && (connections_open_ > 0)) {
            connections_open_ -= 1;
        }
        device_.sendSocketCloseResult(id, success, error_code);
    }


}
