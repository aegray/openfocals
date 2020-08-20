package com.openfocals.services.network;

import android.os.AsyncTask;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import okio.Buffer;
import okio.Okio;
import okio.Sink;
import okio.Source;

public class NetworkSocketManager {

    /// consts
    private static final long MAX_DATA_CHUNK_SIZE = 950;
    //private static final long MAX_DATA_CHUNK_SIZE = 250;
    private static final String TAG = "NetworkSocketManager";
    private final Map<Integer, SocketHolder> sockets_ = new HashMap();


    /// Privates
    Executor executor_;
    EventBus event_bus_ = new EventBus();

    private ISocketManagerListener listener_;
    public void setListener(ISocketManagerListener l) { listener_ = l; }


    class SocketHolder {
        private final Socket socket_;
        public final int stream_id;
        private final Executor executor_;

        private Source data_source_;
        private Sink data_sink_;

        private BackgroundReader reader_;


        private class DataEvent {
            public int id;
            public final Buffer data;
            public DataEvent(int id, Buffer buf) {
                this.id = id;
                this.data = buf;
            }
        }
        private class SocketClosedEvent {
            public int id;
            public SocketClosedEvent(int id) {
                this.id = id;
            }
        }

        private class BackgroundReader implements Runnable {
            private boolean stop_requested_ = false;
            public void run() {
                Buffer buf = new Buffer();
                try {
                    Log.i(TAG, "READER Going to read data on socket");
                    while (!stop_requested_ && data_source_.read(buf, MAX_DATA_CHUNK_SIZE) > 0) {
                        Buffer buf2 = new Buffer();
                        buf2.writeAll(buf);
                        Log.i(TAG, "READER Read data, waiting to post");
                        long t1 = System.currentTimeMillis();
                        event_bus_.post(new DataEvent(stream_id, buf2));
                        long t2 = System.currentTimeMillis();
                        Log.i(TAG, "READER Finished posting data : took=" + (t2 - t1));
                    }
                    Log.i(TAG, "READER DONE WITH WHILE LOOP");
                } catch (IOException e) {
                    Log.w(TAG, "READER Thread stopped due to exception: " + e.toString());
                }
                Log.i(TAG, "READER EXIT READER WHILE LOOP");
                // send socket closed
                //NetworkSocketManager.this.onSocketClosed(SocketHolder.this);
                //NetworkSocketManager.this.onSocketError(SocketHolder.this);
                event_bus_.post(new SocketClosedEvent(SocketHolder.this.stream_id));
            }

            public void requestStop() {
                stop_requested_ = true;
            }
        }

        public class ConnectTask extends AsyncTask<Void, Void, Boolean> {
            private String host;
            private int port;

            private ConnectTask(String h, int p) {
                host = h;
                port = p;
            }

            public Boolean doInBackground(Void... args) {
                try {
                    try {
                        Log.i(TAG, "Trying to connect to host=" + host + " port=" + port);
                        SocketHolder.this.socket_.connect(
                                new InetSocketAddress(host, port)
                        );
                        return true;
                    } catch (IOException | IllegalArgumentException e) {
                        Log.e(TAG, "Failed creating connecting socket for " + host + ":" + port);
                    }

                } catch (IllegalArgumentException e2) {
                    Log.e(TAG, "Failed creating connecting socket (ilarg) for " + host + ":" + port);
                }
                return false;
            }

            public void onPostExecute(Boolean b) {
                if (b.booleanValue()) {
                    SocketHolder.this.onSocketOpened();
                } else {
                    SocketHolder.this.onSocketOpenFailed();
                }
            }
        }


        public SocketHolder(int id, Executor executor) {
            stream_id = id;
            socket_ = new Socket();
            executor_ = executor;
        }

        public void writeData(Buffer d) {
            final Buffer b2 = new Buffer();
            b2.write(d, d.size());
            executor_.execute(new Runnable() {
                final Buffer buf;
                {
                   buf = b2;
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
            //try {
            //    data_sink_.write(d, d.size());
            //} catch (IOException e) {
            //    Log.e(TAG, "Failed to write socket data: stream_id=" + stream_id);
            //}
        }

        public void open(String address, int port) {
            Log.i(TAG, "Calling open on SocketHolder");
            new ConnectTask(address, port).executeOnExecutor(executor_, new Void[0]);
        }

        public void close() {
            if (reader_ != null) {
                reader_.requestStop();
            }

            if (data_source_ != null) {
                try {
                    data_source_.close();
                } catch (IOException e) { }

                data_source_ = null;
            }
            if (data_sink_ != null) {
                try {
                    data_sink_.close();
                } catch (IOException e) {
                }

                data_sink_ = null;
            }
        }

        public void onSocketOpened() {
            try {
                data_sink_ = Okio.sink(socket_.getOutputStream());
                data_source_ = Okio.source(socket_.getInputStream());
            } catch (IOException e) {
                NetworkSocketManager.this.onSocketOpenFailed(this);
                try {
                    socket_.close();
                } catch (IOException e2) {

                }
                data_sink_ = null;
                data_source_ = null;
                return;
            }

            reader_ = new BackgroundReader();
            new Thread(reader_).start();
            NetworkSocketManager.this.onSocketOpened(this);
        }
        public void onSocketOpenFailed() {
            NetworkSocketManager.this.onSocketOpenFailed(this);
        }

    } // end SocketHolder


    //// cons
    public NetworkSocketManager(Executor exec) {
        executor_ = exec;
        event_bus_.register(this);
    }



    public void reset() {
        // close all sockets

        for (HashMap.Entry<Integer, SocketHolder> v : sockets_.entrySet()) {
            //for (SocketHolder v : sockets_.values()) {
            System.out.println("Resetting sockets: " + v.getKey() + " : " + v.getValue());
            internalForceCloseSocket(v.getKey());
        }
        sockets_.clear();
    }

    ///// Controls for socket
    public void openSocket(int id, String address, int port) {
        Log.i(TAG, "Calling openSocket on NetworkSocketManager");
        if (sockets_.containsKey(id)) {
            if (listener_ != null)
                listener_.onSocketOpenResult(id, false, 106);
            return;
        }

        SocketHolder s = new SocketHolder(id, executor_);
        s.open(address, port);
        sockets_.put(id, s);
    }


    private void internalForceCloseSocket(int id) {
        SocketHolder s = sockets_.get(id);
        if (s != null) {
            s.close();
            listener_.onSocketError(id, 104);
        }
        cleanupSocket(id);
    }


    //private void internalCloseSocket(SocketHolder s) {
    //    if (s == null) {
    //        if (listener_ != null)
    //            listener_.onSocketCloseResult(, false, 106);
    //        return;
    //    }
    //    s.close();
    //    if (listener_ != null)
    //        listener_.onSocketError(s.stream_id, 104);
    //}

    public void closeSocket(int id) {
        // this should be called only by a SocketClose request - if we are closing the socket
        // from an internal location, we should generate a SocketError
        SocketHolder s = sockets_.get(id);
        if (s != null) {
            s.close();
            listener_.onSocketCloseResult(id, true, 0);
        } else {
            listener_.onSocketCloseResult(id, false, 107);
        }
        cleanupSocket(id);
    }

    public void socketError(int id) {
        SocketHolder s = sockets_.get(id);
        if (s != null) {
            s.close();
            cleanupSocket(id);
        }
    }

    public void sendData(int id, Buffer data) {
        SocketHolder s = sockets_.get(id);
        if (s == null) {
            if (listener_ != null)
                listener_.onSocketError(id, 107);
        } else {
            s.writeData(data);
        }
    }


    //// Callbacks from socket holder
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocketData(SocketHolder.DataEvent d) {
        if (listener_ == null) return;
        // check if it's still in the map
        if (sockets_.containsKey(d.id)) {
            listener_.onSocketData(d.id, d.data);
        } else {
            listener_.onSocketError(d.id, 107);
        }
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocketClosedFromReader(SocketHolder.SocketClosedEvent d) {
        SocketHolder s = sockets_.get(d.id);

        if (s != null) {

            // this is a mess - @TODO once the framing bug is figured out, clean up these paths
            // there are 100 paths to close a socket / error
           internalForceCloseSocket(s.stream_id);
        }
    }

    public void onSocketOpened(SocketHolder s) {
        if (listener_ == null) return;
        listener_.onSocketOpenResult(s.stream_id, true, 0);
        //listener_.onSocketCloseResult(s.stream_id, true, 0);
    }

    public void onSocketError(SocketHolder s) {
        if (listener_ == null) return;

        if (sockets_.containsKey(s.stream_id)) {
            Log.i(TAG, "READER socketError");
            listener_.onSocketError(s.stream_id, 104);
        } else {
            listener_.onSocketError(s.stream_id, 104);
        }
    }

    public void onSocketClosed(SocketHolder s) {
        if (listener_ == null) return;

        if (sockets_.containsKey(s.stream_id)) {
            Log.i(TAG, "READER socketClose");
            listener_.onSocketCloseResult(s.stream_id, true, 0);
            //listener_.onSocketError(s.stream_id, 104);
            cleanupSocket(s.stream_id);
        } else {
            listener_.onSocketCloseResult(s.stream_id, true, 0);
        }
    }

    public void onSocketOpenFailed(SocketHolder s) {
        cleanupSocket(s.stream_id);
        listener_.onSocketOpenResult(s.stream_id, false, 101);
    }

    private void cleanupSocket(int id) {
        sockets_.remove(id);
    }

}
