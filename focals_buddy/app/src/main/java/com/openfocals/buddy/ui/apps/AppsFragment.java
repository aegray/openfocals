package com.openfocals.buddy.ui.apps;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.openfocals.buddy.FocalsBuddyApplication;
import com.openfocals.buddy.R;
import com.openfocals.focals.Device;
import com.openfocals.focals.events.FocalsConnectedEvent;
import com.openfocals.focals.events.FocalsDisconnectedEvent;
import com.openfocals.services.network.cloudintercept.CustomFocalsAppService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class AppsFragment extends Fragment {

    static final String TAG = "FOCALS_UI_APPS";


    public static AppsFragment newInstance() {
        return new AppsFragment();
    }


    Device device_;
    AppsListAdapter adapter_ = new AppsListAdapter();

    ListView list_apps_;
    Button button_apply_;
    TextView text_connected_;
    CheckBox check_hidden_feats_;

    boolean got_features_ = false;
    boolean features_requested_ = false;




    private static class AppsViewItem {
        Switch switch_enable_;
        TextView text_feature_;
        TextView text_description_;

        public AppsViewItem(View v) {
            switch_enable_ = v.findViewById(R.id.switchFeatureEnabled);
            text_feature_ = v.findViewById(R.id.textFeatureName);
            text_description_ = v.findViewById(R.id.textFeatureDescription);
        }
    }
    
    private class AppsListAdapter extends BaseAdapter {
        public List<CustomFocalsAppService.AppDefinition> apps;
        public List<Boolean> updated_states;
        public boolean state_changed = false;
        public boolean enabled = true;

        public void setEnabled(boolean val) {
            enabled = val;
        }

        public long getItemId(int i) {
            return (long) i;
        }

        AppsListAdapter() {
            apps = CustomFocalsAppService.getInstance().getApps();
            updated_states = new ArrayList<>();
            for (int i = 0; i < apps.size(); ++i) {
                updated_states.add(apps.get(i).is_enabled);
            }
        }

        public int getCount() {
            return apps.size();
        }

        public Object getItem(int i) {
            return apps.get(i);
        }

        public View getView(int i, View view, ViewGroup viewGroup) {
            final int index = i;
            final CustomFocalsAppService.AppDefinition app = apps.get(i);
            AppsViewItem viewItem;
            if (view == null) {
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.features_list_item, null);
                viewItem = new AppsViewItem(view);
                view.setTag(viewItem);
            } else {
                viewItem = (AppsViewItem)view.getTag();
            }

            viewItem.text_feature_.setText(app.name);
            viewItem.text_description_.setText(app.description);
            viewItem.switch_enable_.setOnCheckedChangeListener(null);

            viewItem.switch_enable_.setChecked(updated_states.get(i));
            viewItem.switch_enable_.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    AppsFragment.AppsListAdapter.this.state_changed = true;
                    Log.i(TAG, "Updating enabled state of app: " + app.name + " / " + isChecked);
                    updated_states.set(index, Boolean.valueOf(isChecked));
                }
            });

            //boolean use_enabled = app.is_enabled;

            //viewItem.text_feature_.setEnabled(use_enabled);
            //viewItem.text_description_.setEnabled(use_enabled);
            //viewItem.switch_enable_.setEnabled(use_enabled);

            return view;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        device_ = ((FocalsBuddyApplication)getActivity().getApplication()).device;
        return inflater.inflate(R.layout.fragment_apps, container, false);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        device_ = ((FocalsBuddyApplication) getActivity().getApplication()).device;

        text_connected_ = view.findViewById(R.id.textAppsConnected);

        list_apps_ = view.findViewById(R.id.listApps);
        list_apps_.setAdapter(adapter_);


        button_apply_ = view.findViewById(R.id.buttonAppsApply);
        button_apply_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppsFragment.this.sendUpdatedApps();
            }
        });


    }


    protected void sendUpdatedApps() {

        boolean anychange = false;
        if (adapter_.state_changed) {
            List<String> feats = new ArrayList<>();

            String s = " : apps=";
            for (int i = 0; i < adapter_.apps.size(); ++i) {
                boolean origstate = adapter_.apps.get(i).is_enabled;
                boolean newstate = adapter_.updated_states.get(i);
                anychange |= (origstate != newstate);


                if (origstate != newstate) {
                    Log.i(TAG, "Feature changed: " + adapter_.apps.get(i).is_enabled + " : old=" + origstate + " new=" + newstate);
                }
                if (adapter_.updated_states.get(i)) {
                    feats.add(adapter_.apps.get(i).name);
                    s += adapter_.apps.get(i).name + ", ";
                }

                adapter_.apps.get(i).is_enabled = adapter_.updated_states.get(i);
            }


            if (anychange) {
                Log.i(TAG, "Sending updated apps to focals" + s);

                CustomFocalsAppService.getInstance().updateAppsEnabled(adapter_.apps);
            }
        }

        if (!anychange) {
            Log.i(TAG, "No apps changed - nothing to apply");
        }
        getFragmentManager().popBackStack();
    }

    void setEnabled(boolean enabled) {
        text_connected_.setText(enabled ? "" : "Not connected");
        button_apply_.setEnabled(enabled);
        adapter_.setEnabled(enabled);
    }


    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);

        if (!CustomFocalsAppService.getInstance().appsEnabled()) {
            Toast.makeText(getContext(),
                    "Your focals do not support custom applications." +
                    " In order to add applications, the app manager needs to be" +
                    " installed and setup on the glasses.", Toast.LENGTH_LONG).show();
            getActivity().getSupportFragmentManager().popBackStack();
        } else {
            if (device_.isConnected()) {
                setEnabled(true);
            } else {
                setEnabled(false);
            }
        }
    }


    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsConnected(FocalsConnectedEvent e) {
        setEnabled(true);
        adapter_.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsDisconnected(FocalsDisconnectedEvent e) {
        setEnabled(false);
        adapter_.notifyDataSetChanged();
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

}
