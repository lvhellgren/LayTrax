package com.exelor.laytrax;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.work.Data;
import androidx.work.WorkManager;

public class RunFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    private String tag;
    private View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        tag = this.getClass().getSimpleName();
        ((MainActivity) getActivity()).headerText.setText("Tracking Service is Running");

        view = inflater.inflate(R.layout.run_fragment, container, false);

        Spinner spinner = (Spinner) view.findViewById(R.id.intervals_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.intervals_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        Button button = (Button) view.findViewById(R.id.stop_button);
        button.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.stop_button:
                stopTracking();

                showStartFragment();
                break;
        }
    }

    private void stopTracking() {
        try {
            Log.d(tag, "Stopping service");

            WorkManager.getInstance(getActivity()).cancelAllWorkByTag(MainActivity.TRACKING_WORKER);
            WorkManager.getInstance(getActivity()).pruneWork();

            Toast.makeText(getActivity(), "Service stopped", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(tag, e.toString());
        }
    }

    private void showStartFragment() {
        Activity activity = getActivity();
        Intent intent = new Intent(activity, MainActivity.class);
        activity.startActivity(intent);
        activity.finish();
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        try {
            Long intervalSeconds = Long.valueOf((String) parent.getItemAtPosition(pos));
                saveInterval(intervalSeconds);
                Log.d(tag, "*** intervalSeconds = " + intervalSeconds);
        } catch (Exception e) {
            Log.e(tag, e.toString());
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {
        Log.d(tag, "onNothingSelected");
    }

    private void saveInterval(long interval) {
        SharedPreferences pref = getActivity()
                .getApplicationContext()
                .getSharedPreferences(MainActivity.SHARED_PREFS_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putLong(MainActivity.INTERVAL, interval);
        editor.commit();
    }

}
