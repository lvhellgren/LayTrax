package com.exelor.laytrax;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class LocationWorker extends Worker {

    private String tag;
    private WorkerParameters params;

    public LocationWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params)
    {
        super(context, params);
        this.tag = this.getClass().getSimpleName();
        this.params = params;
    }

    @Override
    public Result doWork() {
        // Do the work here
        Log.d(this.tag, "doWork");
        return Result.success();
    }
}
