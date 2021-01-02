package com.openfocals.buddy;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.openfocals.focals.Device;
import com.openfocals.focals.events.FocalsConnectedEvent;
import com.openfocals.focals.events.FocalsDisconnectedEvent;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.widget.Toast;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "FOCALS_MAIN";

    Device device_;

    private AppBarConfiguration appbar_config_;
    DrawerLayout drawer_;
    NavigationView navview_;
    NavController navcontrol_;
    Fragment ar_receiver_fragment_;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        device_ = ((FocalsBuddyApplication)getApplication()).device;

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawer_ = findViewById(R.id.drawer_layout);
        navview_ = findViewById(R.id.nav_view);

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appbar_config_ = new AppBarConfiguration.Builder(

                R.id.nav_control) //, R.id.nav_share, R.id.nav_send)
                //R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow,
                //R.id.nav_tools, R.id.nav_control, R.id.nav_share, R.id.nav_send)
                .setDrawerLayout(drawer_)
                .build();
        navcontrol_ = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navcontrol_, appbar_config_);
        //NavigationUI.setupWithNavController(navview_, navController);


        navview_.setNavigationItemSelectedListener(this);
        //navview_.getMenu().findItem(R.id.nav_alexa).setEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    //@Override
    //public boolean
    //@Override
    //public boolean onOptionItemSelected(int featureId, MenuItem item) {
    //    Log.i(TAG, "onMenuItemSelected: " + featureId + " : " + item);
    //}

    @Override
    public void onPause() {
        super.onPause();
        device_.getEventBus().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateConnected();
        device_.getEventBus().unregister(this);
    }

    private void updateConnected() {
        //navview_.getMenu().findItem(R.id.nav_alexa).setEnabled(false);
    //    if (device_.isConnected()) {
    //        navview_.getMenu().findItem(R.id.nav_pair_loop).setEnabled(true);
    //        navview_.getMenu().findItem(R.id.nav_calibrate).setEnabled(true);
    //        navview_.getMenu().findItem(R.id.nav_features).setEnabled(true);

    //    } else {
    //        navview_.getMenu().findItem(R.id.nav_pair_loop).setEnabled(false);
    //        navview_.getMenu().findItem(R.id.nav_calibrate).setEnabled(false);
    //        navview_.getMenu().findItem(R.id.nav_features).setEnabled(false);
    //    }
    }


    @Override
    public boolean onSupportNavigateUp() {
        Log.i(TAG, "onSupportNavigateUp");
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, appbar_config_)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        Log.i(TAG, "On item selected");
        // Handle navigation view item clicks here.
        switch (item.getItemId()) {
            //case R.id.nav_alexa: {
            //    navcontrol_.navigate(R.id.nav_alexa);
            //}
            //case R.id.nav_calibrate: {
            //    navcontrol_.navigate(R.id.nav_calibrate);
            //}
            //case R.id.nav_developer_tools: {
            //    navcontrol_.navigate(R.id.nav_developer_tools);
            //}
            case R.id.nav_disconnect: {
                //do somthing
                if (device_.wantConnection()) {
                    Toast.makeText(this,
                            "Disconnected focals",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this,
                            "Focals not connected",
                            Toast.LENGTH_SHORT).show();
                }
                // always do it just for good measure
                device_.stop();
                break;
            }
            //case R.id.nav_features: {
            //    navcontrol_.navigate(R.id.nav_features);
            //}
            //case R.id.nav_pair_loop: {
            //    //do somthing
            //    if (device_.isConnected()) {
            //        device_.startLoopPairing();
            //        Toast.makeText(this,
            //                "Started loop pairing - bring the loop close to your focals and click several times",
            //                Toast.LENGTH_SHORT).show();
            //    } else {
            //        Toast.makeText(this,
            //                "Must be connected to pair loop device",
            //                Toast.LENGTH_SHORT).show();
            //    }
            //    break;
            //}
        }
        //close navigation drawer
        NavigationUI.onNavDestinationSelected(item, navcontrol_);
        drawer_.closeDrawer(GravityCompat.START);
        return true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsConnected(FocalsConnectedEvent e) {
        updateConnected();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsDisconnected(FocalsDisconnectedEvent e) {
        updateConnected();
    }


    public void setActivityResultReceiverFragment(Fragment f)
    {
        ar_receiver_fragment_ = f;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (ar_receiver_fragment_ != null)
            ar_receiver_fragment_.onActivityResult(requestCode, resultCode, data);

    }
}
