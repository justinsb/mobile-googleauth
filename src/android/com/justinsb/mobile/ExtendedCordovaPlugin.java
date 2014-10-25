package com.justinsb.mobile;

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

public class ExtendedCordovaPlugin extends CordovaPlugin {
    static final String TAG = "ExtendedCordovaPlugin";

    public abstract class ActivityCallback {
        protected final CallbackContext callbackContext;
        public boolean runOnThreadPool = false;

        public ActivityCallback(CallbackContext callbackContext) {
            this.callbackContext = callbackContext;
        }

        public abstract void call(int resultCode, Intent intent) throws Exception;
    }

    static class ActivityTracker {
        final ActivityCallback callback;

        public ActivityTracker(ActivityCallback callback) {
            super();
            this.callback = callback;
        }
    };

    final HashMap<Integer, ActivityTracker> activityCallbacks = new HashMap<Integer, ActivityTracker>();

    private int nextRequestId = 100;

    protected void startActivityForResult(Intent intent, ActivityCallback callback) {
        ActivityTracker tracker = new ActivityTracker(callback);

        int requestId;

        synchronized (this) {
            requestId = nextRequestId++;
            activityCallbacks.put(requestId, tracker);
        }

        this.cordova.startActivityForResult(this, intent, requestId);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        final ActivityTracker tracker;
        synchronized (this) {
            tracker = activityCallbacks.remove(requestCode);
        }

        if (tracker == null) {
            Log.e(TAG, "onActivityResult with unknown requestCode");
            return;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    tracker.callback.call(resultCode, intent);
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error processing activity callback", e);
                    returnError(tracker.callback.callbackContext, e);
                }
            }
        };

        if (tracker.callback.runOnThreadPool) {
            cordova.getThreadPool().execute(runnable);
        } else {
            runnable.run();
        }
    }

    public static void returnError(CallbackContext callback, Exception e) {
        try {
            JSONObject err = mapExceptionToJson(e);
            callback.error(err);
        } catch (Exception e2) {
            Log.e(TAG, "Error mapping exception to JSON", e2);
            callback.error("Unexpected error");
        }
    }

    public static JSONObject mapExceptionToJson(Exception e) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("type", e.getClass().getName());
        result.put("typeSimpleName", e.getClass().getSimpleName());
        result.put("message", e.getMessage());
        return result;
    }

}