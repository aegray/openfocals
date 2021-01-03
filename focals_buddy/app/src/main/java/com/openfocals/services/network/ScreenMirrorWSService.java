package com.openfocals.services.network;

import android.util.Base64;
import android.util.Log;

import com.openfocals.services.DeviceService;

import java.io.EOFException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import okio.Buffer;

import com.openfocals.services.screenmirror.ScreenFrameListener;


public class ScreenMirrorWSService implements InterceptedNetworkServiceManager.InterceptedNetworkSessionFactory, ScreenFrameListener {

    private static final String TAG = "FOCALS_SCREEN_MIRROR";
    private static final String WS_KEY_PREFIX = "Sec-WebSocket-Key: ";
    private static final String WS_UUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    ScreenMirrorSession session_ = null;

    String last_frame_ = null;


    private final Object lock = new Object();

    static String getPacketData(Buffer b) {
        try {

            int h0 = b.readByte();
            int h1 = b.readByte();

            if ((h1 & 0x80) != 0x80) {
                Log.e(TAG, "Expected to have a mask on input data, but missing");
                return null;
            }

            int len = h1 & 0x7f;
            if (len == 126) {
                len = b.readShort();
            } else if (len == 127) {
                Log.e(TAG, "Not handling too large message from phone");
                return null;
            }

            byte[] mask_bits = new byte[4];
            for (int i = 0; i < 4; ++i) {
                mask_bits[i] = b.readByte();
            }

            Buffer res = new Buffer();
            int onind = 0;
            for (int i = 0; i < len; ++i) {
                byte c = b.readByte();
                res.writeByte(c ^ mask_bits[onind]);
                onind += 1;
                if (onind > 3) onind = 0;
            }
            return res.readString(Charset.defaultCharset());
        } catch (EOFException e) {
            Log.e(TAG, "EOF when trying to decode data from phone");
        }
        return null;
    }

    @Override
    public void onFrameData(String data) {

        synchronized (lock) {
            last_frame_ = data;
        }
        if (session_ != null) {
            session_.check_frame();
        }

        // for push mode (didn't work/ was getting backed up)
        //if (session_ != null)
        //{
        //    session_.send(data);
        //}
    }


    public class ScreenMirrorSession
            extends InterceptedNetworkServiceManager.InterceptedNetworkSession
    {

        boolean connected_ = false;

        boolean need_frame_ = false;





        // callbacks
        public void onOpen() {
            Log.i(TAG, "Screen mirror ws session open (onOpen)");
            connected_ = false;
        }

        public void check_frame() {
            synchronized (lock) {
                if (need_frame_ && connected_ && (last_frame_ != null)) {
                    sendFrame();
                }
            }
        }

        public void sendFrame() {
            if (last_frame_ != null) {

                Log.i(TAG, "Sending frame size=" + last_frame_.length());
                send(last_frame_);
                last_frame_ = null;
                need_frame_ = false;
            }
        }

        void writeWebsocketData(String data) {
            Buffer bout = new Buffer();

            if (data.length() < 126) {
                bout.writeByte(0x81);
                bout.writeByte(data.length());
                bout.write(data.getBytes());
            } else if (data.length() < 65536) {
                bout.writeByte(0x81);
                bout.writeByte(126);
                short s = (short) data.length();
                bout.writeShort(s);

                bout.write(data.getBytes());
            } else if (data.length() >= 65536) {
                bout.writeByte(0x81);
                bout.writeByte(127);
                long l = data.length();
                bout.writeByte((byte)((l >> 56) & 0xff));
                bout.writeByte((byte)((l >> 48) & 0xff));
                bout.writeByte((byte)((l >> 40) & 0xff));
                bout.writeByte((byte)((l >> 32) & 0xff));
                bout.writeByte((byte)((l >> 24) & 0xff));
                bout.writeByte((byte)((l >> 16) & 0xff));
                bout.writeByte((byte)((l >> 8) & 0xff));
                bout.writeByte((byte)(l & 0xff));




            } else {
                Log.e(TAG, "Couldn't send message - too long: " + data.length());
                return;
            }

            sendData(bout);
        }

        public void send(String data) {
            if (connected_) {
                writeWebsocketData(data);
            }
        }

        private void handleStartConnection(Buffer b) {
            // haven't opened connection yet
            String s = b.readString(Charset.defaultCharset());
            String[] parts = s.split("\r\n");

            // get the
            String wskey = null;

            for (String a : parts) {
                if (a.startsWith(WS_KEY_PREFIX)) {
                    wskey = a.substring(WS_KEY_PREFIX.length());
                }
            }

            if ((wskey == null)) {
                Log.e(TAG, "Improper websocket request - closing");
                close();
                return;
            }


            MessageDigest md;
            try {
                md = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, "Could not get sha1 provider: " + e.toString());
                return;
            }
            byte[] respkey = md.digest((wskey + WS_UUID).getBytes());

            String srespkey = Base64.encodeToString(respkey, Base64.NO_WRAP);

            Buffer bout = new Buffer();
            bout.writeString("HTTP/1.1 101 Switching Protocols", Charset.defaultCharset());
            bout.writeByte(13); bout.writeByte(10);
            bout.writeString("Upgrade: websocket", Charset.defaultCharset());
            bout.writeByte(13); bout.writeByte(10);
            bout.writeString("Connection: Upgrade", Charset.defaultCharset());
            bout.writeByte(13); bout.writeByte(10);
            bout.writeString("Sec-WebSocket-Accept: " + srespkey, Charset.defaultCharset());
            bout.writeByte(13); bout.writeByte(10);
            bout.writeByte(13); bout.writeByte(10);


            sendData(bout);

            connected_ = true;

            Log.i(TAG, "Screeen mirror websocket session started");
            ScreenMirrorWSService.this.session_ = this;
        }

        private void handleWebsocketData(Buffer b) {
            String s = getPacketData(b);
            //Log.i(TAG, "Got command data from glasses: " + s);


            need_frame_ = true;
            check_frame();

           // if (s.contains("next_slide")) {
           //     provider_.onNext();
           // } else if (s.contains("previous_slide")) {
           //     provider_.onPrevious();
           // }
        }


        public void onData(Buffer b) {

            if (connected_) {
                handleWebsocketData(b);
            } else {
                handleStartConnection(b);
            }
        }

        public void onError() {
            // same as on close
            Log.i(TAG, "Screen mirror websocket sessino closed (onError)") ;
            ScreenMirrorWSService.this.session_ = null;
        }

        public void onClose() {
            Log.i(TAG, "Screen mirror websocket session closed (onClose)") ;
            ScreenMirrorWSService.this.session_ = null;
        }
    }

    public void register(InterceptedNetworkServiceManager svc) {
        svc.registerServiceForDomain("screenmirror.ofocals.com", this);
    }


    public ScreenMirrorWSService()
    {
        DeviceService.getInstance().screenListener = this;
    }



    public InterceptedNetworkServiceManager.InterceptedNetworkSession createSession() throws Exception {
        if (session_ != null) {
            session_.close();
        }
        session_ = new ScreenMirrorSession();
        return session_;
    }
}
