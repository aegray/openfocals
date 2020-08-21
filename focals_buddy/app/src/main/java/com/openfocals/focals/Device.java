package com.openfocals.focals;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.UnsignedInteger;
import com.openfocals.focals.events.FocalsBatteryStateEvent;
import com.openfocals.focals.events.FocalsBluetoothMessageEvent;
import com.openfocals.focals.events.FocalsConnectedEvent;
import com.openfocals.focals.events.FocalsConnectionFailedEvent;
import com.openfocals.focals.events.FocalsDisconnectedEvent;
import com.openfocals.focals.messages.AlexaAuthActionToFocals;
import com.openfocals.focals.messages.AlexaAuthInfoRequest;
import com.openfocals.focals.messages.AlexaDeauthorizeRequest;
import com.openfocals.focals.messages.AlexaDoAuthorizeRequest;
import com.openfocals.focals.messages.AlexaDoAuthorizeResponse;
import com.openfocals.focals.messages.AlexaUser;
import com.openfocals.focals.messages.BTMessageToBuddy;
import com.openfocals.focals.messages.BTMessageToFocals;
import com.openfocals.focals.messages.Calibration;
import com.openfocals.focals.messages.DateTimeUpdate;
import com.openfocals.focals.messages.DeviceOptions;
import com.openfocals.focals.messages.EstablishConnection;
import com.openfocals.focals.messages.FileDataResponse;
import com.openfocals.focals.messages.FileTransferResponse;
import com.openfocals.focals.messages.FileTransferStartResponse;
import com.openfocals.focals.messages.FileTransferStatus;
import com.openfocals.focals.messages.FocalsFeaturesAction;
import com.openfocals.focals.messages.HostWhoisResponse;
import com.openfocals.focals.messages.Location;
import com.openfocals.focals.messages.LocationData;
import com.openfocals.focals.messages.LocationUpdate;
import com.openfocals.focals.messages.LoopConnectionState;
import com.openfocals.focals.messages.Notification;
import com.openfocals.focals.messages.NotificationRemove;
import com.openfocals.focals.messages.ProgramInput;
import com.openfocals.focals.messages.QueryFeatures;
import com.openfocals.focals.messages.SetCalibrationMode;
import com.openfocals.focals.messages.SetCloudToken;
import com.openfocals.focals.messages.SetCloudUserId;
import com.openfocals.focals.messages.SetDisplayOffsets;
import com.openfocals.focals.messages.SetFeatures;
import com.openfocals.focals.messages.SocketCloseResponse;
import com.openfocals.focals.messages.SocketData;
import com.openfocals.focals.messages.SocketError;
import com.openfocals.focals.messages.SocketOpenResponse;
import com.openfocals.focals.messages.SoftwareUpdate;
import com.openfocals.focals.messages.SoftwareUpdateCancel;
import com.openfocals.focals.messages.SoftwareUpdateStart;
import com.openfocals.focals.messages.StartCalibration;
import com.openfocals.focals.messages.StartLoopPairing;
import com.openfocals.focals.messages.StartProgram;
import com.openfocals.focals.messages.State;
import com.openfocals.focals.messages.StateUpdate;
import com.openfocals.focals.messages.Status;
import com.openfocals.focals.messages.StopCalibration;
import com.openfocals.focals.messages.StopProgram;
import com.openfocals.focals.messages.UnpairLoop;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okio.Buffer;
import okio.ByteString;
import okio.Okio;
import okio.Sink;
import okio.Source;


public class Device {
    private static final String TAG = "FOCALS_DEVICE";


    //EventBus internal_event_bus_ = new EventBus();

    private static final int MAX_CHUNK_SIZE = 950;
    private static final UUID FOCALS_UUID     = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final UUID FOCALS_DBG_UUID = UUID.fromString("00000000-DECA-FADE-DECA-DEAFDECACAFF");
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

    private static final int SHOULD_DISCOVER_AFTER_MILLIS = 1000 * 5 * 60; // five minute

    //Executor executor_ = Executors.newFixedThreadPool(5); //newSingleThreadExecutor();
    Executor executor_ = Executors.newSingleThreadExecutor(); //newFixedThreadPool(5); //newSingleThreadExecutor();
    BluetoothAdapter bt_adapter_;
    BluetoothDevice bt_device_;
    BluetoothSocket bt_socket_;

    Buffer recv_data_ = new Buffer(); // buffer framed data
    long pending_frame_size_ = 0;

    String target_name_ = null;
    String target_mac_ = null;
    Long device_last_seen_ = null;

    Source data_source_;
    Sink data_sink_;
    ConnectTask connect_task_;

    boolean started_ = false;
    boolean connected_ = false;
    boolean established_ = false;

    LoopConnectionState.State loop_state_ = LoopConnectionState.State.NOT_CONNECTED;

    FocalsBatteryStateEvent last_battery_ = new FocalsBatteryStateEvent();

    public FocalsBatteryStateEvent getLastBatteryState() {
        return last_battery_;
    }


    public Device() {
        getEventBus().register(this);
    }


    public void start() {
        if (!started_) {
            if (connect()) {
                started_ = true;
            }
        }
    }

    public Executor getExecutor() { return executor_; }

    public LoopConnectionState.State getLoopState() { return loop_state_; }


    // fully connected
    public boolean isConnected() { return established_; }

    // in process of connecting
    public boolean isConnecting() { return started_ && !established_; }

    // either isConnected or isConnecting
    public boolean wantConnection() { return started_; }

    // hint for frontend to say if we should do discovery or just try to connect to previous device
    public boolean shouldDiscover() {
        return (
                (target_mac_ == null) ||
                (device_last_seen_ == null) ||
                (System.currentTimeMillis() > device_last_seen_.longValue() + SHOULD_DISCOVER_AFTER_MILLIS)
        );
    }



    public void setTarget(String devname, String target_mac) {
        target_name_ = devname;
        target_mac_ = target_mac;
    }

    public String getTargetName() { return target_name_; }

    public String getTargetMac() { return target_mac_; }


    public void stop() {
        if (started_) {
            established_ = false;
            started_ = false;
            try {
                bt_socket_.close();
            } catch (IOException e) {
                // already closed
            }
        }
    }


    public void onBluetoothStateChanged() {
        BluetoothAdapter b = BluetoothAdapter.getDefaultAdapter();
        if (!b.isEnabled()) {
            stop();
        }
    }

    public EventBus getEventBus() { return EventBus.getDefault(); }



    private void sendMessageInternal(BTMessageToFocals msg, Buffer buf) {
        String str = "";
        if (buf != null && buf.size() > 0) {
            str = str + " payload=(hash=" + (buf.hmacSha1(ByteString.of((byte)'a'))) + ", size=" + buf.size() + ")";
        }
        Log.i(TAG, "Sent msg: " + str + " : " + msg.toString().replace('\n', ' '));
        try {
            Buffer b2 = new Buffer();

            ProtoWriter pw = new ProtoWriter(b2);
            pw.writeVarint32(msg.getSerializedSize());

            b2.write(msg.toByteArray());
            if (buf != null) {
                b2.write(buf, buf.size());
            }

            sendFramedData(b2);

        } catch (IOException e) {
            Log.e(TAG, "Could not write: " + e.toString());
        }
    }

    public void sendMessage(BTMessageToFocals msg, Buffer buf) {
        if (!isConnected()) {
            Log.e(TAG, "Attempted to call sendMessage when not connected: message=" + msg);
            return;
        }
        sendMessageInternal(msg, buf);
    }


    public void sendMessage(BTMessageToFocals msg) {
        sendMessage(msg, null);
    }



    //////////////// privates
    private class ConnectTask extends AsyncTask<Void, Void, Boolean> {
        BluetoothSocket socket;

        private ConnectTask(BluetoothSocket s)
        {
            this.socket = s;
        }

        public Boolean doInBackground(Void... voidArr) {
            try{
                Log.i(TAG, "calling connect() direct");
                this.socket.connect();
                return true;
            } catch (IOException e) {
                Log.e(TAG, "GOT IOEXCEPTION B: " + e.toString());
                return false;
            } catch (Throwable e) {
                Log.e(TAG, "GOT THROWABLE: " + e.toString());
                throw e;
            }
        }
    }





    private boolean connect()
    {
        if (connected_) return true;
        if (target_mac_ == null) return false;

        bt_adapter_ = BluetoothAdapter.getDefaultAdapter();
        bt_device_ = bt_adapter_.getRemoteDevice(target_mac_);

        try {
            bt_socket_ = bt_device_.createRfcommSocketToServiceRecord(FOCALS_UUID);
            connect_task_ = new ConnectTask(bt_socket_) {
                public void onPostExecute(Boolean b) {
                    if (b.booleanValue()) {
                        Device.this.onBluetoothConnected(this.socket);
                        Log.i(TAG, "Connected");
                    } else {
                        Log.e(TAG, "COULD NOT CONNECT");
                        Device.this.onBluetoothConnectionFailed();
                    }
                }
            };
            this.connect_task_.executeOnExecutor(this.executor_, new Void[0]);
        } catch (IOException e) {
            Log.e(TAG, "Bluetooth could not connect: " + e.toString());

            Device.this.onBluetoothConnectionFailed();
            return false;
        }

        return true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDisconnect(DisconnectedEvent d) {
        onBluetoothDisconnected();
    }



    private void onBluetoothConnectionFailed() {
        connected_ = false;
        established_ = false;

        getEventBus().post(new FocalsConnectionFailedEvent());

        if (started_) {
            connect();
        }
    }

    private void onBluetoothDisconnected() {
        connected_ = false;
        established_ = false;

        device_last_seen_ = Long.valueOf(System.currentTimeMillis());

        getEventBus().post(new FocalsDisconnectedEvent());

        if (started_) {
            connect();
        }
    }

    private void onBluetoothConnected(BluetoothSocket s) {
        // initial connection established
        Log.i(TAG, "BLUETOOTH CONNECTED AND READY");

        device_last_seen_ = Long.valueOf(System.currentTimeMillis());

        try {
            data_sink_ = Okio.sink(s.getOutputStream());
            data_source_ = Okio.source(s.getInputStream());

            // start the data reader
            BackgroundReader r = new BackgroundReader();
            new Thread(r).start();
            connected_ = true;
        } catch (IOException e) {
            Log.e(TAG, "Got IOException trying to open stream: " + e.toString());
        }

        // send initial establish message
        Log.i(TAG, "Sending establish connection");
        sendEstablishConnectionMessage();
    }


    void reset_recv() {
        recv_data_.clear();
        pending_frame_size_ = 0;
    }


    // internal events
    private class DisconnectedEvent {

    };

    private class BluetoothDataEvent {
        public final Buffer data;
        public BluetoothDataEvent(Buffer buf) {
            this.data = buf;
        }
    }






    private class BackgroundReader implements Runnable {
        public void run() {
            Log.i(TAG, "STARTING READER LOOP");
            Buffer buf = new Buffer();
            try {
                while (Device.this.started_ && Device.this.data_source_.read(buf, MAX_CHUNK_SIZE) > 0) {
                    Buffer buf2 = new Buffer();
                    buf2.writeAll(buf);
                    Device.this.getEventBus().post(new BluetoothDataEvent(buf2));
                }
                Log.i(TAG, "DONE WITH WHILE LOOP");
            } catch (IOException e) {
                Log.w(TAG, "Thread stopped due to exception: " + e.toString());
            }
            Log.i(TAG, "EXIT READER WHILE LOOP");
            Device.this.getEventBus().post(new DisconnectedEvent());
        }
    }





    void sendEstablishConnectionMessage() {
        sendMessageInternal(BTMessageToFocals.newBuilder().setEstablishConnection(
                EstablishConnection.newBuilder().setVersionMajor(4).setVersionMinor(40).build()
            ).build(), null);
    }



    void sendFramedData(Buffer b) throws IOException {
        Buffer b2 = new Buffer();
        long size = b.size();
        if (size <= VarInt.MAX_UVARINT32_VALUE) {
            new ProtoWriter(b2).writeVarint32((int)b.size());
            b2.writeAll(b);
            Log.i(TAG, "WRITING DATA: " + b2.size());
            data_sink_.write(b2, b2.size());
            return;
        }
        throw new IOException("Data of size " + size + " is too large");
    }



    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessage(FocalsBluetoothMessageEvent e) {
        if (e.buffer != null) {
            Log.i(TAG, "Got message: " + e.message.toString().replace('\n', ' ') + " buf=(hash=" + e.buffer.hmacSha1(ByteString.of((byte)'a')) + ") : " + e.buffer);
        } else {
            Log.i(TAG, "Got message: " + e.message.toString().replace('\n', ' ') + " buf=(hash=" + e.buffer);
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDataReceivedFromBluetooth(BluetoothDataEvent d) {

        try {
            recv_data_.writeAll(d.data);

            while (recv_data_.size() > 0) {
                if (pending_frame_size_ <= 0) {
                    try {
                        pending_frame_size_ = VarInt.readUVarInt32(recv_data_);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        return;
                    } catch (IOException e) {
                        reset_recv();
                        Log.e(TAG, "Failed to read varint from recv data: " + e.toString());
                        return;
                    }
                }
                long size = recv_data_.size();
                long j = pending_frame_size_;
                if (size >= j) {
                    if (j == 0) {
                        Log.e(TAG, "Received frame with size 0");
                    } else {
                        Buffer b2 = new Buffer();
                        b2.write(recv_data_, pending_frame_size_);
                        pending_frame_size_ = 0;
                        onUnframedDataReceivedFromBluetooth(b2);
                    }
                } else {
                    return;
                }
            }
        } catch (IOException e2) {
            Log.e(TAG, "Failed to unframe data: " + e2.toString());
            reset_recv();
        }
    }

    private void onUnframedDataReceivedFromBluetooth(Buffer b) {
        try {
            int len = new ProtoReader(b).readVarint32();
            Buffer b2 = new Buffer();
            b2.write(b, (long)len);

            BTMessageToBuddy m = BTMessageToBuddy.parseFrom(b2.readByteArray());

            onMessageReceivedFromBluetooth(m, b);
        } catch (IOException e) {
            Log.e(TAG, "Failed to decode message: " + e.toString());
        }
    }


    private void onMessageReceivedFromBluetooth(BTMessageToBuddy m, Buffer b) {
        if (m.hasEstablishConnectionResponse()) {
            Log.i(TAG, "Established connection");

            established_ = true;
            Log.i(TAG, "Posting FocalsConnectedEvent");
            sendDateTimeUpdate();
            getEventBus().post(new FocalsConnectedEvent());
        } else if (m.hasBatteryState()) {
            FocalsBatteryStateEvent ev = new FocalsBatteryStateEvent(
                    m.getBatteryState().getFocalsBatteryLevel(),
                    m.getBatteryState().getLoopBatteryLevel(),
                    m.getBatteryState().getCharging());
            last_battery_ = ev;
            Log.i(TAG, "Posting FocalsBatteryStateEvent");
            getEventBus().post(ev);
        } else if (m.hasLoopState()) {
            // update our loop state so we have a cache
            loop_state_ = m.getLoopState().getState();
            getEventBus().post(new FocalsBluetoothMessageEvent(m, b));
        } else {
            getEventBus().post(new FocalsBluetoothMessageEvent(m, b));
        }
    }


    //// Message shorthands / utilities



    ///////////////////// Sync + state related
    public void setNetworkEnabled(boolean is_enabled) {
        sendMessage(BTMessageToFocals.newBuilder().setState(
                State.newBuilder().setNetworkConnected(is_enabled).build()
        ).build());
    }


    public void setDisplayOffsets(int x, int y) {
        sendMessage(BTMessageToFocals.newBuilder().setDeviceOptions(
            DeviceOptions.newBuilder().setDisplayOffsets(
                    SetDisplayOffsets.newBuilder()
                            .setX(x)
                            .setY(y)
                            .build()
            ).build()
        ).build());
    }

    public void setCloudHost(String host) {
        sendMessage(BTMessageToFocals.newBuilder().setDeviceOptions(
                DeviceOptions.newBuilder().setCloudHost(host).build()
        ).build());
    }

    public void setCloudToken(String token) {
        sendMessage(BTMessageToFocals.newBuilder().setState(
                State.newBuilder().setCloudToken(
                        SetCloudToken.newBuilder().setToken(token).build()
                ).build()
        ).build());
    }

    public void setCloudUserId(String userid) {
        sendMessage(BTMessageToFocals.newBuilder().setState(
                State.newBuilder().setCloudUserId(
                        SetCloudUserId.newBuilder().setId(userid).build()
                ).build()
        ).build());
    }

    public void setupCloud(String host, String userid, String token, String dummyid) {
        sendMessage(BTMessageToFocals.newBuilder().setStateUpdate(
                StateUpdate.newBuilder()
                        .setOptions(
                                DeviceOptions.newBuilder()
                                        .setCloudHost(host)
                                        .build()
                        )
                        .setState(
                                State.newBuilder()
                                        .setCloudToken(
                                                SetCloudToken.newBuilder().setToken(token).build()
                                        )
                                        .setCloudUserId(
                                                SetCloudUserId.newBuilder().setId(userid).build()
                                        )
                                        .setDummyid(dummyid)
                                        .setEnableCalendar(true)
                                        .build()
                        )
                        .build()

        ).build());

    }


    /////// basics
    public void sendDateTimeUpdate() {
        sendMessage(BTMessageToFocals.newBuilder().setDatetime(
                DateTimeUpdate.newBuilder()
                        .setDatetime(DATETIME_FORMAT.format(Calendar.getInstance().getTime()))
                        .setTz("CST")
                        .build()
        ).build());
    }



    ///////////////////  alexa

    public void alexaRequestAuthInfo() {
        sendMessage(BTMessageToFocals.newBuilder().setAlexaAuth(
                AlexaAuthActionToFocals.newBuilder().setAuthInfo(
                        AlexaAuthInfoRequest.newBuilder()
                                .setName("alexa")
                                .build()
                ).build()
        ).build());
    }

    public void alexaDoAuthorize(
            String authCode,
            String redirectUri,
            String clientId,
            AlexaUser user) {

        AlexaDoAuthorizeRequest.Builder b = AlexaDoAuthorizeRequest.newBuilder();
        b = b.setName("alexa")
            .setAuthorizationCode(authCode)
            .setClientId(clientId)
            .setRedirectUri(redirectUri);
        if (user != null) {
            b.setUser(user);
        }

        sendMessage(BTMessageToFocals.newBuilder().setAlexaAuth(
                AlexaAuthActionToFocals.newBuilder().setAuthorize(
                        b.build()
                ).build()
        ).build());
    }

    public void alexaDoAuthorize(
            String authCode,
            String redirectUri,
            String clientId) {
        alexaDoAuthorize(authCode, redirectUri, clientId, null);
    }

    public void alexaDeauthorize() {
        sendMessage(BTMessageToFocals.newBuilder().setAlexaAuth(
                AlexaAuthActionToFocals.newBuilder().setDeauthorize(
                        AlexaDeauthorizeRequest.newBuilder()
                                .setName("alexa")
                                .build()
                ).build()
        ).build());
    }









    ////////////////// Calibration

    public void calibrationStart() {
        sendMessage(BTMessageToFocals.newBuilder().setCalibration(
                Calibration.newBuilder().setStart(
                        StartCalibration.newBuilder().setSomeintshoudlbe1(1).build()
                ).build()
        ).build());
    }

    public void calibrationStop() {
        sendMessage(BTMessageToFocals.newBuilder().setCalibration(
                Calibration.newBuilder().setStop(
                        StopCalibration.newBuilder().build()
                ).build()
        ).build());
    }

    public void calibrationSetMainMode() {
        sendMessage(BTMessageToFocals.newBuilder().setCalibration(
                Calibration.newBuilder().setSetMode(
                        SetCalibrationMode.newBuilder()
                            .setMode(SetCalibrationMode.Mode.MODE_MAIN)
                            .build()
                ).build()
        ).build());
    }


    ////////////////// Time updates
    //public void updateCurrentTime() { CurrentTime }




    ////////////////// Loop pairing
    public void startLoopPairing() {
        sendMessage(BTMessageToFocals.newBuilder().setStartLoopPairing(
                StartLoopPairing.newBuilder().build()
        ).build());
    }

    public void unpairLoop() {
        sendMessage(BTMessageToFocals.newBuilder().setUnpairLoop(
                UnpairLoop.newBuilder().build()
        ).build());
    }



    //////////////////// Notifications
    public void dismissNotification(String id) {
        sendMessage(BTMessageToFocals.newBuilder().setNotificationRemove(
                NotificationRemove.newBuilder().setId(id).build()
        ).build());
    }


    public void sendNotification(Notification n) {
        sendMessage(BTMessageToFocals.newBuilder().setNotification(n).build());
    }


    ////////////////// Features
    public void getFeatureList() {
        sendMessage(BTMessageToFocals.newBuilder().setFeaturesAction(
                FocalsFeaturesAction.newBuilder().setQueryFeatures(
                        QueryFeatures.newBuilder().build()
                ).build()
        ).build());
    }

    public void setEnabledFeatures(List<String> enabledFeats) {
        sendMessage(BTMessageToFocals.newBuilder().setFeaturesAction(
                FocalsFeaturesAction.newBuilder().setSetFeatures(
                        SetFeatures.newBuilder().addAllId(enabledFeats).build()
                ).build()
        ).build());
    }



    ///////////////////// Experiences

    public void startExperience(String name, Map<String, String> inputs) {
        List<ProgramInput> linputs = null;
        if (inputs != null) {
            linputs = new ArrayList<>();
            for (Map.Entry<String, String> e : inputs.entrySet()) {
                linputs.add(ProgramInput.newBuilder().setKey(e.getKey()).setValue(e.getValue()).build());
            }
        }
        sendMessage(BTMessageToFocals.newBuilder().setStartProgram(
            StartProgram.newBuilder().setName(name).addAllInputs(linputs).build()
        ).build());
    }

    public void startExperience(String name) {
        startExperience(name, null);
    }

    public void stopExperience(String name) {
        sendMessage(BTMessageToFocals.newBuilder().setStopProgram(
                StopProgram.newBuilder().setName(name).build()
        ).build());
    }

    public void startScreencast(String host, int port) {
        startExperience("Konacast Service", ImmutableMap.of(
                "host", host,
                "port", Integer.toString(port),
                "castName", "cast"
        ));
    }

    public void stopScreencast() {
        stopExperience("Konacast Service");
    }



    /////////////////// Settings
    public void getTemplatedSettings() {
        throw new RuntimeException("Not impl");
    }

    public void setTemplatedSettings(String jsonval) {
        throw new RuntimeException("Not impl");
    }


    //////////////////// Location

    public void sendLocation(double latitude, double longitude) {
        sendMessage(BTMessageToFocals.newBuilder().setLocation(
                Location.newBuilder().setData(
                        LocationUpdate.newBuilder()
                                .setStatus(LocationUpdate.LocationStatus.OK)
                                .setData(
                                        LocationData.newBuilder()
                                                .setLatitude(latitude)
                                                .setLongitude(longitude)
                                                .build()
                                ).build()
                ).build()
            ).build()
        );
    }


    /////////////////// Feature list

    ////// Files

    public void sendFileTransferStartResponse(String id, FileTransferStatus status, Long length, Long checksum) {
        FileTransferStartResponse.Builder b = FileTransferStartResponse.newBuilder()
                .setId(id)
                .setStatus(status);
        if (length != null)
            b.setLength(length.intValue());

        if (checksum != null)
            b.setChecksum(checksum.intValue());

        sendMessage(BTMessageToFocals.newBuilder().setFileTransfer(
                FileTransferResponse.newBuilder().setFileTransfer(b.build()).build()
        ).build());
    }

    public void sendFileData(String id, FileTransferStatus status, Long offset, Long checksum, Buffer data) {
        FileDataResponse.Builder b = FileDataResponse.newBuilder()
                .setId(id)
                .setStatus(status);
        if (offset != null)
            b.setOffset(offset.intValue());

        if (checksum != null)
            b.setChecksum(checksum.intValue());

        sendMessage(BTMessageToFocals.newBuilder().setFileTransfer(
                FileTransferResponse.newBuilder().setFileData(b.build()).build()
        ).build(), data);
    }



    /////// Updates
    public void sendSoftwareUpdateStart(String id, String version, String minFromVersion) {
        SoftwareUpdateStart.Builder b = SoftwareUpdateStart.newBuilder().setId(id).setVersion(version);
        if (minFromVersion != null) {
            b.setMinFromVersion(minFromVersion);
        }

        sendMessage(BTMessageToFocals.newBuilder().setSoftwareUpdate(
                SoftwareUpdate.newBuilder().setStart(b.build()).build()
        ).build());
    }


    public void sendSoftwareUpdateCancel() {
        sendMessage(BTMessageToFocals.newBuilder().setSoftwareUpdate(
                SoftwareUpdate.newBuilder().setCancel(
                        SoftwareUpdateCancel.newBuilder().build()
                ).build()
        ).build());
    }



    /////////////////// Calendar
    /// Calendar


    ////////////////// socket related
    public void sendHostWhoisResponse(String host, boolean success, String host_address, Integer error_code) {

        Log.i(TAG, "Sending whois resposne: " + host + " / " + host_address + " / " + error_code + " / " + success);
        HostWhoisResponse.Builder b = HostWhoisResponse.newBuilder();
        b = b.setHost(host);
        if (success) {
            b = b.setStatus(Status.STATUS_OK)
                .setError(0)
                .setAddress(host_address);
        } else {
            b = b.setStatus(Status.STATUS_ERROR)
                    .setError(error_code);
        }

        sendMessage(BTMessageToFocals.newBuilder().setHostWhoisResponse(
                b.build()
        ).build());
    }

    public void sendSocketOpenResponse(int id, boolean success, Integer error_code) {
        sendMessage(BTMessageToFocals.newBuilder().setSocketOpenResponse(
                SocketOpenResponse.newBuilder()
                        .setId(id)
                        .setStatus(success ? Status.STATUS_OK : Status.STATUS_ERROR)
                        .setError(success ? 0 : error_code)
                        .build()
        ).build());
    }


    public void sendSocketError(int id, int error_code) {
        sendMessage(BTMessageToFocals.newBuilder().setSocketError(
                SocketError.newBuilder().setId(id).setError(error_code).build()
        ).build());
    }

    public void sendSocketData(int id, Buffer data) {
        sendMessage(BTMessageToFocals.newBuilder().setSocketData(
                SocketData.newBuilder().setId(id).build()
        ).build(), data);
    }

    public void sendSocketCloseResult(int id, boolean success, int error_code) {
        sendMessage(BTMessageToFocals.newBuilder().setSocketCloseResponse(
                SocketCloseResponse.newBuilder()
                        .setId(id)
                        .setStatus(success ? Status.STATUS_OK : Status.STATUS_ERROR)
                        .setError(error_code).build()
        ).build());
    }


}
