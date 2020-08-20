package com.openfocals.services.network.present;

import android.util.Base64;
import android.util.Log;

import com.openfocals.services.network.InterceptedNetworkServiceManager;

import org.apache.commons.text.StringEscapeUtils;

import java.io.EOFException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import okio.Buffer;
import static java.nio.charset.StandardCharsets.UTF_8;

public class PresentationInterceptService implements InterceptedNetworkServiceManager.InterceptedNetworkSessionFactory {

    private static final String TAG = "FOCALS_PRESENT";
    private static final String PRESENT_CODE_PREFIX = "presentation_code=";
    private static final String WS_KEY_PREFIX = "Sec-WebSocket-Key: ";
    private static final String WS_UUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    HashMap<String, PresentationProvider> providers_ = new HashMap<>();

    PresentationSession session_ = null;

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



    public class PresentationSession
            extends InterceptedNetworkServiceManager.InterceptedNetworkSession
            implements IPresenter {

        PresentationProvider provider_ = null;


        // callbacks
        public void onOpen() {
            Log.i(TAG, "Presenter open (onOpen)");
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
            } else {
                Log.e(TAG, "Couldn't send message - too long: " + data.length());
                return;
            }

            Log.d(TAG, "Sending ws data raw to focals: " + bout.size() + ": " + bout.clone().readString(UTF_8));
            sendData(bout);
        }

        void writeCardData(String data) {
            Log.i(TAG, "Sending card data: " + data);
            writeWebsocketData("{\"type\": \"current_state\", \"state\": \"currently_presenting\", \"title\": \"stuff\", \"notes\": \"" +
                    StringEscapeUtils.escapeJava(data) + "\", \"slide_number\": -1, \"total_slides\": 3}");
        }

        public void sendCard(String data) {
            writeCardData(data);
        }

        private void handleStartConnection(Buffer b) {
            // haven't opened connection yet
            String s = b.readString(Charset.defaultCharset());
            String[] parts = s.split("\r\n");

            // get the
            String code = null;
            String wskey = null;

            int ind = parts[0].indexOf(PRESENT_CODE_PREFIX);
            if (ind >= 0) {
                String s1 = parts[0].substring(ind + PRESENT_CODE_PREFIX.length());
                int ind2 = s1.indexOf('&');
                if (ind2 >= 0) {
                    code = s1.substring(0, ind2);
                }
            }


            for (String a : parts) {
                if (a.startsWith(WS_KEY_PREFIX)) {
                    wskey = a.substring(WS_KEY_PREFIX.length());
                }
            }

            if ((wskey == null) || (code == null)) {
                Log.e(TAG, "Improper presenter request - closing");
                close();
                return;
            }

            // get provider for this code
            PresentationProvider p = providers_.get(code);

            if (p == null) {
                Log.e(TAG, "No presenter registered for code. Closing.  code=" + code);
                close();
                return;
            }
            provider_ = p;
            p.setPresenter(this);

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

            //Log.i(TAG, "Using websocket keys: in=" + key + " out=" + srespkey);

            sendData(bout);
            writeWebsocketData("{\"type\": \"connected\"}");

            provider_.resetPresentation();

            Log.i(TAG, "Presentation started");
        }

        private void handleWebsocketData(Buffer b) {
            String s = getPacketData(b);
            Log.i(TAG, "Got command data from glasses: " + s);

            if (s.contains("next_slide")) {
                provider_.onNext();
            } else if (s.contains("previous_slide")) {
                provider_.onPrevious();
            }
        }


        public void onData(Buffer b) {
            Log.i(TAG, "Presenter got data: " + b.clone().readUtf8());

            if (provider_ == null) {
                handleStartConnection(b);
            } else {
                handleWebsocketData(b);
            }
        }

        public void onError() {
            // same as on close
            Log.i(TAG, "Presenter closed (onError)");
            if (provider_ != null) {
                provider_.onClose();
            }
        }

        public void onClose() {
            Log.i(TAG, "Presenter closed (onClose)");
            if (provider_ != null) {
                provider_.onClose();
            }
        }
    }

    public void register(InterceptedNetworkServiceManager svc) {
        svc.registerServiceForDomain("north-teleprompter.herokuapp.com", this);
    }


    private static char chartToNum(char c) {
        switch (c) {
            case 'A': return '0';
            case 'B': return '1';
            case 'C': return '2';
            case 'D': return '3';
            case 'E': return '4';
            default:
                // not error handling - default to 0
                throw new RuntimeException("Invalid char");
        }
    }

    private static char numToChar(char c) {
        switch (c) {
            case '0': return 'A';
            case '1': return 'B';
            case '2': return 'C';
            case '3': return 'D';
            case '4': return 'E';
            default:
                // not error handling - default to 0
                throw new RuntimeException("Invalid char");
        }
    }

    private static String codeNumberToLetters(String codeNumbers) {
        String s = "";
        for (int i = 0; i < codeNumbers.length(); ++i) {
            char c = codeNumbers.charAt(i);
            s += numToChar(c);
        }
        return s;
    }

    private static String codeLettersToNumber(String codeLetters) {
        String s = "";
        for (int i = 0; i < codeLetters.length(); ++i) {
            char c = codeLetters.charAt(i);
            s += chartToNum(c);
        }
        return s;
    }


    // this must be a letter code of A-E
    public void registerPresentationProvider(String codeLetters, PresentationProvider p) {
        providers_.put(codeLettersToNumber(codeLetters), p);
    }

    public InterceptedNetworkServiceManager.InterceptedNetworkSession createSession() throws Exception {
        if (session_ != null) {
            session_.close();
        }
        session_ = new PresentationSession();

        return session_;
    }
}
