package com.exelor.laytrax;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

public class TrackingService extends Service {

    private String tag;

    @Override
    public void onCreate() {
        tag = this.getClass().getSimpleName();
        Log.d(tag, "Start onCreate");
    }

    /**
     * The system calls this method when another component wants to bind with the service by calling
     * bindService().
     * @return null or an IBinder object.
     */
    @Override
    public IBinder onBind(Intent intent) {return null;}

    /**
     * The system calls this method when another component, such as an activity, requests that the
     * service be started, by calling startService(). If you implement this method, it is your
     * responsibility to stop the service when its work is done, by calling stopSelf() or
     * stopService() methods.
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(tag, "Start onStartCommand");
//        resourceName = intent.getStringExtra(ResourceActivity.RESOURCE_NAME);
//        Log.d(tag, "Resource Name: " + resourceName);
//
//        resourceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Let it continue running until it is stopped:
        return START_STICKY;
    }

}

