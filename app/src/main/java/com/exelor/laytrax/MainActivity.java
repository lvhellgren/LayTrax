package com.exelor.laytrax;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    public static final String SHARED_PREFS_NAME = "TraxInfo";
    public static final String UNIT_ID = "unitId";
    public static final String ACCOUNT_ID = "accountId";
    public static final String EMAIL = "email";

    public TextView headerText;

    private String tag;
    private String unitId;
    private String accountId;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.tag = this.getClass().getSimpleName();
        setContentView(R.layout.activity_main);

        this.headerText = (TextView) findViewById(R.id.header_text);

        this.unitId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.d(tag, "Unit ID: " + unitId);

        SharedPreferences pref = getApplicationContext()
                .getSharedPreferences(MainActivity.SHARED_PREFS_NAME, 0);

        this.accountId = pref.getString(MainActivity.ACCOUNT_ID, "");
        Log.d(tag, "unitName: " + this.unitId);
        this.email = pref.getString(MainActivity.EMAIL, "");
        Log.d(tag, "email: " + this.email);

        if (findViewById(R.id.fragment_container) != null) {

            // If we're being restored from a previous state, then we don't need to do anything and
            // should return or else we could end up with overlapping fragments.
//            if (savedInstanceState != null) {
//                return;
//            }

            if (isServiceRunning(TrackingService.class)) {
                Log.d(tag, "The service is already running");

                RunnerFragment runnerFragment = new RunnerFragment();
                getSupportFragmentManager()
                        .beginTransaction()
                        .add(R.id.fragment_container, runnerFragment)
                        .commit();
            } else {
                Log.d(tag, "Starting the service");

                Bundle bundle = new Bundle();
                bundle.putString(UNIT_ID, this.unitId);
                bundle.putString(ACCOUNT_ID, this.accountId);
                bundle.putString(EMAIL, this.email);

                ConnectorFragment fragment = new ConnectorFragment();
                fragment.setArguments(bundle);

                getSupportFragmentManager()
                        .beginTransaction()
                        .add(R.id.fragment_container, fragment)
                        .commit();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(tag, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(tag, "onResume");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(tag, "onStop");
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}
