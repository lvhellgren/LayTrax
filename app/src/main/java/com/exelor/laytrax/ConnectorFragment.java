package com.exelor.laytrax;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

public class ConnectorFragment extends Fragment implements View.OnClickListener {

    private String tag;
    private View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        this.tag = this.getClass().getSimpleName();

        ((MainActivity)getActivity()).headerText.setText("Start Tracking Service");

        this.view = inflater.inflate(R.layout.connector_fragment, container, false);
        if (this.view != null) {
            Bundle arguments = this.getArguments();
            if (arguments != null) {
                ((TextView) this.view.findViewById(R.id.unit_id_field)).setText(arguments.getString(MainActivity.UNIT_ID, ""));
                ((TextView) this.view.findViewById(R.id.account_id_field)).setText(arguments.getString(MainActivity.ACCOUNT_ID, ""));
                ((TextView) this.view.findViewById(R.id.email_field)).setText(arguments.getString(MainActivity.EMAIL, ""));
            } else {
                Log.e(this.tag, "Could not get the view");
            }

        } else {
            Log.e(this.tag, "Could not get the arguments");
        }

        Button button = (Button) this.view.findViewById(R.id.start_button);
        button.setOnClickListener(this);

        return this.view;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start_button:
                savePreferences(view);
                String password = ((EditText) this.view.findViewById(R.id.password_field)).getText().toString();
                Log.d(tag, "password_field: " + password);
                startTrackingService();
                showRunnerFragment();
                break;
        }
    }

    private void savePreferences(View view) {
        SharedPreferences pref = getActivity()
                .getApplicationContext()
                .getSharedPreferences(MainActivity.SHARED_PREFS_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();

        String unitId = ((EditText) this.view.findViewById(R.id.unit_id_field)).getText().toString();
        String accountId = ((EditText) this.view.findViewById(R.id.account_id_field)).getText().toString();
        String email = ((EditText) this.view.findViewById(R.id.email_field)).getText().toString();
        editor.putString(MainActivity.UNIT_ID, unitId);
        editor.putString(MainActivity.ACCOUNT_ID, accountId);
        editor.putString(MainActivity.EMAIL, email);
        editor.commit();
    }

    private void startTrackingService() {
        try {
            Intent intent = new Intent(getActivity(), TrackingService.class);
            Log.d(tag, intent.toString());
            ComponentName comp = getActivity().startService(intent);
            Log.d(tag, "Service component name: " + comp.toString());
            Toast.makeText(getActivity(), "Service started", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(tag, e.toString());
        }
    }

    private void showRunnerFragment() {
        RunnerFragment fragment = new RunnerFragment();
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
