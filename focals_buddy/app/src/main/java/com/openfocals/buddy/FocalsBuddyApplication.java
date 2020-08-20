package com.openfocals.buddy;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.openfocals.focals.Device;
import com.openfocals.services.DeviceService;

public class FocalsBuddyApplication extends Application {

    private static final String TAG = "FOCALS_APP";

    public Device device;

    //public Executor executor = Executors.newSingleThreadExecutor();

    //public NetworkService network; // = new NetworkService(device, executor);

    private DeviceService dev_service_ = null;
    private ServiceConnection dev_service_conn_ = new ServiceConnection(){
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(TAG, "Service: " + name + " connected");
            dev_service_ = ((DeviceService.DeviceServiceBinder)service).getService();
        }

        public void onServiceDisconnected(ComponentName name) {
            Log.v(TAG, "Service: " + name + " disconnected");
        }
    };

//    Intent bindIntent = new Intent(this,ServiceTask.class);
//    if (ServiceTools.isServiceRunning() == false){
//        Log.d(Global.TAG,"-->service will be started.");
//        startService(bindIntent);
//    }else{
//        Log.d(Global.TAG,"-->service already is running");
//    }
//    boolean bound = bindService(bindIntent,mConnection,0);

//k    public static boolean isServiceRunning(){
//k        final ActivityManager activityManager = (ActivityManager)Global.gContext.getSystemService(Global.gContext.ACTIVITY_SERVICE);
//k        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);
//k
//k        boolean isServiceFound = false;
//k
//k        for (int i = 0; i < services.size(); i++) {
//k            //Log.d(Global.TAG, "Service" + i + " :" + services.get(i).service);
//k            //Log.d(Global.TAG, "Service" + i + " package name : " + services.get(i).service.getPackageName());
//k            //Log.d(Global.TAG, "Service" + i + " class name : " + services.get(i).service.getClassName());
//k
//k            if ("com.atClass.lmt".equals(services.get(i).service.getPackageName())){
//k                if ("com.atClass.lmt.ServiceTask".equals(services.get(i).service.getClassName())){
//k                    isServiceFound = true;
//k                }
//k            }
//k        }
//k        return isServiceFound;
//k    }

    public FocalsBuddyApplication() throws Exception {

        device = DeviceService.getDevice();
//        network = new NetworkService(device, executor);
//
//
//
//
//        network.interceptedNetworkServices().registerRemapping("bynorth.com", "192.168.1.6");
//        network.interceptedNetworkServices().registerRemapping("prod.bynorth.com", "192.168.1.6");
//        network.interceptedNetworkServices().registerRemapping("cloud.bynorth.com", "192.168.1.6");

        //network.interceptedNetworkServices().registerServiceForDomain("api.uber.com", new InterceptedSSLSessionLogger());
        //network.interceptedNetworkServices().registerServiceForDomain("api.amazon.com", new InterceptedSSLSessionLogger());
        //network.interceptedNetworkServices().registerServiceForDomain("192.168.1.6", new InterceptedSSLSessionLogger());
        //network.interceptedNetworkServices().registerServiceForIP("192.168.1.6", new InterceptedSSLSessionLogger());

        //SSLInterceptDataHandler.createInterceptSSLHandler();

        //InterceptedNetworkServiceManager.InterceptedNetworkSession()

        //LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, new IntentFilter("Msg"));
        //new NotificationService(); //.setListener( this ) ;
    }


    public void setupDevice() {

    }


    //public void setupServiceListener(LocalBroadcastManager lbm) {
    //    lbm.registerReceiver(onNotice, new IntentFilter("Msg")); //"Msg"));
    //    //LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, new IntentFilter("Msg"));

    //}

    public static FocalsBuddyApplication getApplicationInstance(Context context) {
        return (FocalsBuddyApplication)context.getApplicationContext();
    }

//    private BroadcastReceiver onNotice= new BroadcastReceiver() {
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String pack = intent.getStringExtra("package");
//            String title = intent.getStringExtra("title");
//            String text = intent.getStringExtra("text");
//
//            Log.i(TAG, "Got notification: pack=" + pack + " title=" + title + " text=" + text);
////
////
////
////            TableRow tr = new TableRow(getApplicationContext());
////            tr.setLayoutParams(new TableRow.LayoutParams( TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
////            TextView textview = new TextView(getApplicationContext());
////            textview.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT,1.0f));
////            textview.setTextSize(20);
////            textview.setTextColor(Color.parseColor("#0B0719"));
////            textview.setText(Html.fromHtml(pack +"<br><b>" + title + " : </b>" + text));
////            tr.addView(textview);
////            tab.addView(tr);
////
//
//
//
//        }
//    };

}
