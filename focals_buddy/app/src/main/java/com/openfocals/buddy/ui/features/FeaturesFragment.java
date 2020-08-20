package com.openfocals.buddy.ui.features;

import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.util.Pair;
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

import com.openfocals.buddy.FocalsBuddyApplication;
import com.openfocals.buddy.R;
import com.openfocals.focals.Device;
import com.openfocals.focals.events.FocalsBluetoothMessageEvent;
import com.openfocals.focals.events.FocalsConnectedEvent;
import com.openfocals.focals.events.FocalsDisconnectedEvent;
import com.openfocals.focals.messages.FeatureSpec;
import com.openfocals.focals.messages.QueryFeaturesResponseItems;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class FeaturesFragment extends Fragment {

    static final String TAG = "FOCALS_UI_FEATURES";


    public static FeaturesFragment newInstance() {
        return new FeaturesFragment();
    }


    Device device_;
    FeatureListAdapter adapter_ = new FeatureListAdapter();

    ListView list_features_;
    Button button_apply_;
    TextView text_connected_;
    CheckBox check_hidden_feats_;

    boolean got_features_ = false;
    boolean features_requested_ = false;



    private static class FeatureViewItem {
        Switch switch_enable_;
        TextView text_feature_;
        TextView text_description_;

        public FeatureViewItem(View v) {
            switch_enable_ = v.findViewById(R.id.switchFeatureEnabled);
            text_feature_ = v.findViewById(R.id.textFeatureName);
            text_description_ = v.findViewById(R.id.textFeatureDescription);
        }
    }
    
    private class FeatureListAdapter extends BaseAdapter {
        public List<FeatureSpec> features;
        public List<Boolean> updated_states;
        public boolean state_changed = false;
        public boolean enabled = true;

        public void setFeatures(List<FeatureSpec> defs) {
            features = new ArrayList<>();
            updated_states = new ArrayList<>();

            for (int i = 0; i < defs.size(); ++i) {
                features.add(defs.get(i));
                updated_states.add(defs.get(i).getEnabled());
            }

            state_changed = false;
        }

        public void setEnabled(boolean val) {
            enabled = val;
        }


        public long getItemId(int i) {
            return (long) i;
        }

        FeatureListAdapter() {
            features = new ArrayList<>();
        }

        public int getCount() {
            return features.size();
        }

        public Object getItem(int i) {
            return features.get(i);
        }

        public View getView(int i, View view, ViewGroup viewGroup) {
            final int index = i;
            final FeatureSpec feature = features.get(i);
            FeatureViewItem viewItem;
            if (view == null) {
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.features_list_item, null);
                viewItem = new FeatureViewItem(view);
                view.setTag(viewItem);
            } else {
                viewItem = (FeatureViewItem)view.getTag();
            }

            viewItem.text_feature_.setText(feature.getName());
            viewItem.text_description_.setText(feature.getDescription());
            viewItem.switch_enable_.setOnCheckedChangeListener(null);

            viewItem.switch_enable_.setChecked(updated_states.get(i));
            viewItem.switch_enable_.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    FeatureListAdapter.this.state_changed = true;
                    updated_states.set(index, Boolean.valueOf(isChecked));
                }
            });

            boolean use_enabled = (feature.getEditable()) ? enabled : false;
            if (!check_hidden_feats_.isChecked()) {
                use_enabled &= feature.getVisible();
            }

            //boolean use_enabled = (feature.getEditable()) ? enabled : false;
            //boolean use_enabled = (feature.getEditable() && feature.getVisible()) ? enabled : false;
            viewItem.text_feature_.setEnabled(use_enabled);
            viewItem.text_description_.setEnabled(use_enabled);
            viewItem.switch_enable_.setEnabled(use_enabled);

            return view;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        device_ = ((FocalsBuddyApplication)getActivity().getApplication()).device;
        return inflater.inflate(R.layout.features_fragment, container, false);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        device_ = ((FocalsBuddyApplication) getActivity().getApplication()).device;

        text_connected_ = view.findViewById(R.id.textFeaturesConnected);

        check_hidden_feats_ = view.findViewById(R.id.checkEnableHiddenFeatures);
        check_hidden_feats_.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                adapter_.notifyDataSetChanged();
            }
        });

        list_features_ = view.findViewById(R.id.listFeatures);
        list_features_.setAdapter(adapter_);


        button_apply_ = view.findViewById(R.id.buttonApply);
        button_apply_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FeaturesFragment.this.sendUpdatedFeatures();
            }
        });


    }


    protected void sendUpdatedFeatures() {

        boolean anychange = false;
        if (adapter_.state_changed) {
            List<String> feats = new ArrayList<>();

            String s = " : features=";
            for (int i = 0; i < adapter_.features.size(); ++i) {
                boolean origstate = adapter_.features.get(i).getEnabled();
                boolean newstate = adapter_.updated_states.get(i);
                anychange |= (origstate != newstate);


                if (origstate != newstate) {
                    Log.i(TAG, "Feature changed: " + adapter_.features.get(i).getId() + " : old=" + origstate + " new=" + newstate);
                }
                if (adapter_.updated_states.get(i)) {
                    feats.add(adapter_.features.get(i).getId());
                    s += adapter_.features.get(i).getId() + ", ";
                }
            }


            if (anychange) {
                Log.i(TAG, "Sending updated features to focals" + s);
                device_.setEnabledFeatures(feats);
            }
        }

        if (!anychange) {
            Log.i(TAG, "No features changed - nothing to apply");
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

        if (device_.isConnected()) {
            features_requested_ = true;
            device_.getFeatureList();
            setEnabled(true);
        } else {
            setEnabled(false);
        }
    }


    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsConnected(FocalsConnectedEvent e) {
        if (!features_requested_) {
            features_requested_ = true;
            device_.getFeatureList();
        } else {
            setEnabled(got_features_);
            adapter_.notifyDataSetChanged();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsMessage(FocalsBluetoothMessageEvent e) {
        if (e.message.hasFeatures()) {
            if (e.message.getFeatures().hasFeatureItems()) {
                QueryFeaturesResponseItems r = e.message.getFeatures().getFeatureItems();
                if (!got_features_) {
                    got_features_ = true;
                    setEnabled(true);
                    adapter_.setFeatures(r.getFeatureSpecsList());
                    adapter_.notifyDataSetChanged();
                }
            }
        }
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
