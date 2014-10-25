package com.justinsb.mobile.googleauth;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;

public class GoogleAuth extends CordovaPlugin {

    static final int REQUEST_CODE_CHOOSE_ACCOUNT = 101;

    static final String TAG = "GoogleAuthPlugin";

    private static final String DEFAULT_SCOPE = "oauth2:https://www.googleapis.com/auth/userinfo.profile";

    private CallbackContext callbackContext = null;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        // XXX: Concurrent calls?
        this.callbackContext = callbackContext;

        try {
            if (action.equals("chooseAccount")) {
                JSONObject options = args.getJSONObject(0);
                chooseAccount(options);
                return true;
            } else if (action.equals("getAuthToken")) {
                JSONObject options = args.getJSONObject(0);
                getAuthToken(options, callbackContext);
                return true;
            } else {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                return false;
            }
        } catch (JSONException e) {
            Log.w(TAG, "JSON parsing error", e);
            String errorMessage = e.getMessage();
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, errorMessage));
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error", e);
            String errorMessage = e.getMessage();
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, errorMessage));
            return false;
        }
    }

    void chooseAccount(JSONObject options) {
        String[] accountTypes = new String[] { "com.google" };
        Intent intent = AccountPicker.newChooseAccountIntent(null, null, accountTypes, false, null, null, null, null);
        this.cordova.startActivityForResult((CordovaPlugin) this, intent, REQUEST_CODE_CHOOSE_ACCOUNT);
    }

    void getAuthToken(JSONObject options, CallbackContext callbackContext) throws JSONException {
        String scope = options.optString("scope", DEFAULT_SCOPE);
        String accountName = options.getString("name");
        getAuthToken(accountName, scope, callbackContext);
    }

    void getAuthToken(String accountName, String scope, CallbackContext callbackContext) throws JSONException {
        // XXX: Wrap in a threadpool task to make async?

        Activity activity = this.cordova.getActivity();

        try {
            String token = GoogleAuthUtil.getToken(activity, accountName, scope);

            JSONObject result = new JSONObject();
            result.put("token", token);
            callbackContext.success(result);

            return;
        } catch (GooglePlayServicesAvailabilityException e) {
            // This is a specific type of UserRecoverableAuthException indicating that the user's current version of
            // Google Play services is outdated. Although the recommendation above for UserRecoverableAuthException also
            // works for this exception, calling startActivityForResult() will immediately send users to Google Play
            // Store to install an update, which may be confusing. So you should instead call getConnectionStatusCode()
            // and pass the result to GooglePlayServicesUtil.getErrorDialog(). This returns a Dialog that includes an
            // appropriate message and a button to take users to Google Play Store so they can install an update.

            Log.e(TAG, "Google Play Services is not available", e);
            JSONObject err = mapExceptionToJson(e);
            callbackContext.error(err);
        } catch (UserRecoverableAuthException e) {
            // This is an error that users can resolve through some verification. For example, users may need to confirm
            // that your app is allowed to access their Google data or they may need to re-enter their account password.
            // When you receive this exception, call getIntent() on the instance and pass the returned Intent to
            // startActivityForResult() to give users the opportunity to solve the problem, such as by logging in.

            callbackContext.error(mapExceptionToJson(e));
        } catch (IOException e) {
            // XXX: Offline?
            Log.e(TAG, "IOException while getting token", e);
            JSONObject err = mapExceptionToJson(e);
            callbackContext.error(err);
        } catch (GoogleAuthException e) {
            Log.e(TAG, "GoogleAuthException while getting token", e);
            JSONObject err = mapExceptionToJson(e);
            callbackContext.error(err);
        }
    }

    private JSONObject mapExceptionToJson(Exception e) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("type", e.getClass().getName());
        result.put("typeSimpleName", e.getClass().getSimpleName());
        result.put("message", e.getMessage());
        return result;
    }

    void processResultChooseAccount(int resultCode, Intent intent) throws JSONException {
        if (resultCode == Activity.RESULT_OK) {
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("name", intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
            this.callbackContext.success(result);
        } else if (resultCode == Activity.RESULT_CANCELED) {
            // The account picker dialog closed without selecting an account.
            JSONObject result = new JSONObject();
            result.put("success", false);
            this.callbackContext.success(result);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        try {
            if (requestCode == REQUEST_CODE_CHOOSE_ACCOUNT) {
                this.processResultChooseAccount(resultCode, intent);
            } else {
                Log.w(TAG, "Unknown request code in onActivityResult: " + requestCode);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error", e);
            callbackContext.error("Unexpected error");
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR));
        }
    }
}