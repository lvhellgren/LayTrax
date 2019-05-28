package com.exelor.laytrax;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class SerialWorker extends Worker {

    private String tag;
    private WorkerParameters params;
    private boolean repeat = true;
    private Context context;

    public SerialWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
        this.params = params;
        tag = this.getClass().getSimpleName();
    }

    @Override
    public Result doWork() {
        Log.d(tag, " *** doWork");

        try {
            Data inputData = getInputData();

            String unitId = inputData.getString(MainActivity.UNIT_ID);
            String account = inputData.getString(MainActivity.ACCOUNT_ID);
            String email = inputData.getString(MainActivity.EMAIL);

//            requestLocationUpdates();

            SharedPreferences prefs = getApplicationContext()
                    .getSharedPreferences(MainActivity.SHARED_PREFS_NAME, 0);
            long intervalSeconds = prefs.getLong(MainActivity.INTERVAL, 5l);

            try {
                Thread.sleep(intervalSeconds * 1000);
            } catch (InterruptedException e) {
                Log.e(tag, e.toString());
            }

            // Start a new worker:
            String outputResult;
            if (repeat) {
                startWorker(inputData);
            }
            return Result.success();
        } catch (Exception e) {
            Log.d(tag, e.toString());

            return Result.failure();
        }
    }

    private void startWorker(Data data) {
        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SerialWorker.class)
                .setConstraints(constraints)
                .setInputData(data)
                .addTag(MainActivity.TRACKING_WORKER)
                .build();

        WorkManager.getInstance(context).enqueue(workRequest);
    }

    public void onStopped() {
        Log.d(tag, "*** Worker stopped");
        repeat = false;
    }
}
