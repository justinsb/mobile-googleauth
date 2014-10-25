package com.justinsb.mobile.googleauth;

import org.apache.cordova.CordovaActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import com.google.android.gms.common.AccountPicker;

public class GoogleAuth extends CordovaPlugin {

    static final int REQUEST_CODE_CHOOSE_ACCOUNT = 101;

    static final String TAG = "GoogleAuthPlugin";

    private CallbackContext callbackContext = null;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        // XXX: Concurrent calls?
        this.callbackContext = callbackContext;

        try {
            if (action.equals("chooseAccount")) {
                JSONObject options = args.getJSONObject(0);
                chooseAccount(options);
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
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