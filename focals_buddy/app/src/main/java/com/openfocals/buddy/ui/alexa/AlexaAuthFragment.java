package com.openfocals.buddy.ui.alexa;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amazon.identity.auth.device.api.authorization.User;
import com.openfocals.buddy.FocalsBuddyApplication;
import com.openfocals.buddy.R;


import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.api.authorization.AuthCancellation;
import com.amazon.identity.auth.device.api.authorization.AuthorizationManager;
import com.amazon.identity.auth.device.api.authorization.AuthorizeListener;
import com.amazon.identity.auth.device.api.authorization.AuthorizeRequest;
import com.amazon.identity.auth.device.api.authorization.AuthorizeResult;
import com.amazon.identity.auth.device.api.authorization.ScopeFactory;
import com.amazon.identity.auth.device.api.workflow.RequestContext;
import com.openfocals.focals.Device;
import com.openfocals.focals.events.FocalsBluetoothMessageEvent;
import com.openfocals.focals.events.FocalsDisconnectedEvent;
import com.openfocals.focals.messages.AlexaAuthActionToBuddy;
import com.openfocals.focals.messages.AlexaAuthInfoResponse;
import com.openfocals.focals.messages.AlexaDoAuthorizeResponse;
import com.openfocals.focals.messages.AlexaUser;
import com.openfocals.focals.messages.Status;
import com.openfocals.services.alexa.AlexaAuthState;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;


public class AlexaAuthFragment extends Fragment {
    private RequestContext request_context_;
    private static final String TAG = "FOCALS_ALEXA";
    private static final String CODE_CHALLENGE_METHOD = "S256";
    private static final String ALEXA_NAME = "alexa";


    // again not proper coding / organization, but I'm just doing the model here
    // rather than separating it out to get it done quickly
    // @TODO: reorg

    Device device_;

    TextView text_status_;
    Button button_deauth_;

    boolean got_focals_info_ = false;
    AlexaAuthInfoResponse focals_info_;
    AuthorizeListenerImpl auth_listener_ = new AuthorizeListenerImpl();


    public AlexaAuthFragment() {
        // Required empty public constructor
    }

    private class AuthorizeListenerImpl extends AuthorizeListener {

        /* Authorization was completed successfully. */
        @Override
        public void onSuccess(final AuthorizeResult authorizeResult) {
            final String authorizationCode = authorizeResult.getAuthorizationCode();
            final String redirectUri = authorizeResult.getRedirectURI();
            final String clientId = authorizeResult.getClientId();

            Log.i(TAG, "Got alexa auth from amazon: authcode=" + authorizationCode +
                    " redirectUri=" + redirectUri +
                    " clientid=" + clientId);

            if (device_.isConnected()) {
                User u = authorizeResult.getUser();
                if (u != null) {
                    device_.alexaDoAuthorize(authorizationCode, redirectUri, clientId,
                            AlexaUser.newBuilder().setName(u.getUserName()).setEmail(u.getUserEmail()).build()
                        );
                } else {
                    device_.alexaDoAuthorize(authorizationCode, redirectUri, clientId, null);
                }
            }
        }

        /* There was an error during the attempt to authorize the application. */
        @Override
        public void onError(final AuthError authError) {
            Log.e(TAG, "Failed to authorize alexa: " + authError.getMessage());
            errQuitNonUi("Failed to authorize alexa: " + authError.getMessage());
        }

        /* Authorization was cancelled before it could be completed. */
        @Override
        public void onCancel(final AuthCancellation authCancellation) {
            Log.e(TAG, "Alexa authorization canceled: " + authCancellation.toString());
            errQuitNonUi("Alexa authorization canceled: " + authCancellation.toString());
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        device_ = ((FocalsBuddyApplication) getActivity().getApplication()).device;
        text_status_ = getView().findViewById(R.id.textAlexaStatus);
        button_deauth_ = getView().findViewById(R.id.buttonAlexaDeauth);

        button_deauth_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AlexaAuthState.getInstance().isAuthenticated()) {
                    device_.alexaDeauthorize();
                    quit("Alexa was disabled");
                } else {
                    startProcess();
                }
            }
        });
        button_deauth_.setVisibility(View.INVISIBLE);
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        device_ = ((FocalsBuddyApplication) getActivity().getApplication()).device;
        request_context_ = RequestContext.create(getContext());
        request_context_.registerListener(auth_listener_);
    }


    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");

        device_.getEventBus().register(this);

        //quit("This is disabled in public builds to not accidentally log you out - it sometimes works, sometimes doesn't and is still being looked into");

        if (AlexaAuthState.getInstance().isAuthenticated()) {
            text_status_.setText("Alexa is already enabled.  Click below to disable");
            button_deauth_.setText("Deauthorize Alexa");
            button_deauth_.setVisibility(View.VISIBLE);
        } else {
            text_status_.setText("Alexa is not enabled on your focals.  Click below to login and setup alexa");
            button_deauth_.setVisibility(View.VISIBLE);
            button_deauth_.setText("Login to Alexa");
            request_context_.onResume();
        }
    }


    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "onStop");
        device_.getEventBus().unregister(this);
        request_context_.unregisterListener(auth_listener_);
    }

    private void quit(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        getActivity().getSupportFragmentManager().popBackStack();
    }

    private void quitNonUi(final String msg) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                quit(msg);
            }
        });
    }

    private void errQuit(String msg) {
        quit(msg);
    }

    private void errQuitNonUi(String msg) {
        quitNonUi(msg);
    }

    private void startProcess() {
        if (device_.isConnected()) {
            device_.alexaRequestAuthInfo();
        } else {
            errQuit("Device must be connected to setup alexa");
        }
    }


    private void doAvsAuth() {
        final JSONObject scopeData = new JSONObject();
        final JSONObject productInstanceAttributes = new JSONObject();

        try {
            productInstanceAttributes.put("deviceSerialNumber", focals_info_.getDsn());
            scopeData.put("productInstanceAttributes", productInstanceAttributes);
            scopeData.put("productID", focals_info_.getProductId());

            Log.i(TAG, "Trying to do login with amazon auth:" +
                    " dsn=" + focals_info_.getDsn() +
                    " productid=" + focals_info_.getProductId() +
                    " challenge=" + focals_info_.getCodeChallenge());

            AuthorizationManager.authorize(new AuthorizeRequest.Builder(request_context_)
                    .addScope(ScopeFactory.scopeNamed("alexa:all", scopeData))
                    .forGrantType(AuthorizeRequest.GrantType.AUTHORIZATION_CODE)
                    .withProofKeyParameters(focals_info_.getCodeChallenge(), CODE_CHALLENGE_METHOD)
                    .build());
        } catch (JSONException e) {
            // handle exception here
            errQuit("Exception encountered when setting up alexa authorizaiton");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_alexa_auth, container, false);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsBluetoothMessage(FocalsBluetoothMessageEvent e) {
        Log.i(TAG, "Got focals bluetooth message: " + e.message.hasAlexaAuth() + " : " + e.message);
        if (e.message.hasAlexaAuth()) {
            AlexaAuthActionToBuddy r = e.message.getAlexaAuth();
            Log.i(TAG, "HasAuthInfo?? " + r.hasAuthInfo());
            if (r.hasAuthInfo()) {
                Log.i(TAG, "name: " + r.getAuthInfo().getName());
                if (r.getAuthInfo().getName().equals(ALEXA_NAME)) {
                    got_focals_info_ = true;
                    focals_info_ = r.getAuthInfo();
                    Log.i(TAG, "Got alexa auth info: " + r.getAuthInfo());
                    try {
                        doAvsAuth();
                    } catch (Exception ex) {
                        Log.e(TAG, "Doing login with amazon call got exception: " + ex.toString());
                        errQuit("Failed to setup alexa: " + ex.getMessage());
                    }
                }
            } else if (r.hasAuthorize()) {
                if (r.getAuthorize().getName().equals(ALEXA_NAME)) {
                    Log.i(TAG, "Got alexa authorize response : " + r.getAuthorize());
                    AlexaDoAuthorizeResponse r2 = r.getAuthorize();
                    if (r2.getResult() != Status.STATUS_OK) {
                        errQuit("Failed to update alexa authorization on focals side");
                    } else {
                        quit("Alexa set up and enabled");
                    }
                }
            } else if (r.hasState()) {
                if (r.getState().getName().equals(ALEXA_NAME)) {
                    Log.i(TAG, "Got alexa auth state: " + r.getState());
                }
            } else if (r.hasAuthUpdate()) {
                if (r.getAuthUpdate().getName().equals(ALEXA_NAME)) {
                    Log.i(TAG, "Got alexa auth update: " + r.getAuthUpdate());
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsDisconnected(FocalsDisconnectedEvent e) {
        errQuit("Device disconnected - cannot finish alexa authorization");
    }

}
