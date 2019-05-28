package com.exelor.laytrax;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String SHARED_PREFS_NAME = "TraxInfo";
    public static final String UNIT_ID = "unitId";
    public static final String ACCOUNT_ID = "accountId";
    public static final String EMAIL = "email";
    public static final String TRACKING_WORKER = "tracking";
    public static final String INTERVAL = "interval";

    public TextView headerText;

    private String tag;
    private String unitId;
    private String accountId;
    private String email;

    private LiveData<List<WorkInfo>> liveData;
    private boolean isStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.tag = this.getClass().getSimpleName();
        setContentView(R.layout.activity_main);

        this.headerText = (TextView) findViewById(R.id.header_text);

        this.unitId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.d(tag, "Unit ID: " + unitId);

        SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences(MainActivity.SHARED_PREFS_NAME, 0);
        this.accountId = prefs.getString(MainActivity.ACCOUNT_ID, "");
        this.email = prefs.getString(MainActivity.EMAIL, "");

        if (findViewById(R.id.fragment_container) != null) {
            checkWorker();
        }
    }

    private void checkWorker() {
        liveData = WorkManager.getInstance(getApplicationContext())
                .getWorkInfosByTagLiveData(MainActivity.TRACKING_WORKER);
        liveData.observe(this, workInfos -> {
            if (workInfos == null || workInfos.isEmpty()) {
                showStartPage();
            } else if (!isStarted) {
                showRunPage();
            }
            return;
        });
    }

    private void showStartPage() {
        Bundle bundle = new Bundle();
        bundle.putString(UNIT_ID, this.unitId);
        bundle.putString(ACCOUNT_ID, this.accountId);
        bundle.putString(EMAIL, this.email);

        StartFragment fragment = new StartFragment();
        fragment.setArguments(bundle);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void showRunPage() {
        RunFragment runFragment = new RunFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, runFragment)
                .commit();
        isStarted = true;
    }
}
