package com.openfocals.services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

import com.openfocals.buddy.R;
import com.openfocals.focals.Device;
import com.openfocals.services.alexa.AlexaAuthState;
import com.openfocals.services.files.FileTransferService;
import com.openfocals.services.location.LocationService;
import com.openfocals.services.media.MediaPlaybackService;
import com.openfocals.services.network.InterceptedSSLPassthroughProxy;
import com.openfocals.services.network.InterceptedSSLSessionLogger;
import com.openfocals.services.network.cloudintercept.CloudMockService;
import com.openfocals.services.network.NetConnectedService;
import com.openfocals.services.network.NetworkService;
import com.openfocals.services.network.cloudintercept.CloudWeatherService;
import com.openfocals.services.network.cloudintercept.CustomFocalsAppService;
import com.openfocals.services.network.cloudintercept.integrations.MusicIntegration;
import com.openfocals.services.network.cloudintercept.integrations.NotesIntegration;
import com.openfocals.services.network.cloudintercept.integrations.TasksIntegration;
import com.openfocals.services.network.present.PresentationInterceptService;
import com.openfocals.services.network.present.providers.AudioSubtitlePresentationProvider;
import com.openfocals.services.network.present.providers.StreamingTextPresentationProvider;
import com.openfocals.services.notifications.NotificationSender;
import com.openfocals.services.update.SoftwareUpdateService;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DeviceService extends Service {

    private static final String TAG = "FOCALS_DEVSERVICE";
    static Device device = new Device();


    // services + handlers to publish data to / from glasses
    Executor executor;
    NetworkService network;
    NetConnectedService network_connected;
    NotificationSender notification_sender;
    AlexaAuthState alexa_auth;
    CloudMockService cloud_mock;
    LocationService location;
    CustomFocalsAppService apps;
    PresentationInterceptService presentation;
    MediaPlaybackService media;
    FileTransferService files;
    SoftwareUpdateService update;



    //static DeviceService instance;

    public static Device getDevice() { return device; }
    //public static DeviceService getInstance() { return instance; }


    private final IBinder binder = new DeviceServiceBinder();
    public class DeviceServiceBinder extends Binder {
        public DeviceService getService() {
            return DeviceService.this;
        }
    }

    private final BroadcastReceiver bt_state_receiver = new BroadcastReceiver() {
        public void onReceive (Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                device.onBluetoothStateChanged();
            }
        }
    };



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "Service onStartCommand");
        //device.getEventBus().register(this);
        return START_STICKY;
    }
    

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service onCreate");

        executor = Executors.newSingleThreadExecutor();
        network = new NetworkService(device, executor);
        network_connected = new NetConnectedService(this, device);
        notification_sender = new NotificationSender(this, device);
        alexa_auth = new AlexaAuthState(device);
        cloud_mock = new CloudMockService(device);
        location = new LocationService(device);
        apps = new CustomFocalsAppService();
        presentation = new PresentationInterceptService();
        media = new MediaPlaybackService(this);
        files = new FileTransferService(this, device);
        update = new SoftwareUpdateService(device);



        // network services
        network.interceptedNetworkServices().registerServiceForDomain(CloudMockService.CLOUD_HOSTNAME, cloud_mock);
        new TasksIntegration().register(cloud_mock);
        new NotesIntegration().register(cloud_mock);
        new MusicIntegration().register(cloud_mock);
        new CloudWeatherService().register(cloud_mock);

        // https / cloud
        apps.register(cloud_mock);

        // http app.ofocals.com
        apps.register(network.interceptedNetworkServices());
        apps.registerApplication("2048", "game: 2048", CustomFocalsAppService.readRawTextFile(this, R.raw.game_2048));

        presentation.register(network.interceptedNetworkServices());
        presentation.registerPresentationProvider("AAA", new StreamingTextPresentationProvider());
        presentation.registerPresentationProvider("BAA", new AudioSubtitlePresentationProvider(this));



        //network.interceptedNetworkServices().registerRemapping("bynorth.com", "192.168.1.6");
        //network.interceptedNetworkServices().registerRemapping("prod.bynorth.com", "192.168.1.6");
        //network.interceptedNetworkServices().registerRemapping("cloud.bynorth.com", "192.168.1.6");
        //network.interceptedNetworkServices().registerRemapping("cloud.bysouth.com", "192.168.1.6");
        network.interceptedNetworkServices().registerRemapping("cloud.bynorth.com", "192.168.1.6");

        //network.interceptedNetworkServices().registerRemapping("cloud.ofocals.com", "192.168.1.6");
        //network.interceptedNetworkServices().registerRemapping("dog.ofocals.com", "192.168.1.6");

        // Cloud mock service will point the glasses at a made up hostname on connect, provide
        // the remapping here to keep all network remappings in one place
        // - This will setup cloud_mock to create a local data ssl session for each
        // socket open request to this new cloud host
        //network.interceptedNetworkServices().registerServiceForDomain(CloudMockService.CLOUD_HOSTNAME, new InterceptedSSLSessionLogger());
        //network.interceptedNetworkServices().registerServiceForDomain("cloud.bynorth.com", new InterceptedSSLSessionLogger());
        //network.interceptedNetworkServices().registerServiceForDomain("cloud.bysouth.com", new InterceptedSSLSessionLogger());
        //network.interceptedNetworkServices().registerServiceForDomain("api.amazon.com", new InterceptedSSLSessionLogger());
        //network.interceptedNetworkServices().registerServiceForDomain("speech.googleapis.com", new InterceptedSSLSessionLogger());
        //network.interceptedNetworkServices().registerServiceForDomain("speech.googleapis.com",
        //        new InterceptedSSLPassthroughProxy(executor, "speech.googleapis.com", 443));


        //// amazon/alexa
 //       network.interceptedNetworkServices().registerServiceForDomain("api.amazon.com",
 //               new InterceptedSSLPassthroughProxy(executor, "api.amazon.com", 443));
 //       network.interceptedNetworkServices().registerServiceForDomain("avs-alexa-na.amazon.com",
 //               new InterceptedSSLPassthroughProxy(executor, "avs-alexa-na.amazon.com", 443));
        //network.interceptedNetworkServices().registerServiceForDomain("api.amazon.com", new InterceptedSSLSessionLogger());
        //network.interceptedNetworkServices().registerServiceForDomain("avs-alexa-na.amazon.com", new InterceptedSSLSessionLogger());


        //// google speech
        //network.interceptedNetworkServices().registerServiceForDomain("speech.googleapis.com", new InterceptedSSLSessionLogger());
        //network.interceptedNetworkServices().registerServiceForDomain("speech.googleapis.com",
        //        new InterceptedSSLPassthroughProxy(executor, "speech.googleapis.com", 443));

        // If we want to auto connect
        //device.start();
        registerReceiver(bt_state_receiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    public void onDestroy() {
        Log.i(TAG, "Service onDestroy");
        device.stop();
        //device.getEventBus().unregister(this);
        unregisterReceiver(bt_state_receiver);
        super.onDestroy();
    }

    public IBinder onBind(Intent intent) {
        return binder;
    }
}
