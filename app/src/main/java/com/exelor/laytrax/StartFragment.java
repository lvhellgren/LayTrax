package com.exelor.laytrax;

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
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;

public class StartFragment extends Fragment implements View.OnClickListener {

    private String tag;
    private View view;
//    private FirebaseAuth firebaseAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        this.tag = this.getClass().getSimpleName();
//        firebaseAuth = FirebaseAuth.getInstance();

        ((MainActivity)getActivity()).headerText.setText("Start Tracking Service");

        this.view = inflater.inflate(R.layout.start_fragment, container, false);
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
    public void onStart() {
        super.onStart();
//        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
//        Log.d(tag, currentUser.toString());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start_button:
                Data data = buildData(this.view);
                savePreferences(data);
                String password = ((EditText) this.view.findViewById(R.id.password_field)).getText().toString();
                Log.d(tag, "password_field: " + password);
                startWorker(data);
                showRunFragment();
                break;
        }
    }

    private Data buildData(View view) {
        Data data = new Data.Builder()
                .putString(MainActivity.UNIT_ID,
                        ((EditText) view.findViewById(R.id.unit_id_field)).getText().toString())
                .putString(MainActivity.ACCOUNT_ID,
                        ((EditText) view.findViewById(R.id.account_id_field)).getText().toString())
                .putString(MainActivity.EMAIL,
                        ((EditText) view.findViewById(R.id.email_field)).getText().toString())
                .build();
        return data;
    }

    private void savePreferences(Data data) {
        SharedPreferences pref = getActivity()
                .getApplicationContext()
                .getSharedPreferences(MainActivity.SHARED_PREFS_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();

        editor.putString(MainActivity.UNIT_ID, data.getString(MainActivity.UNIT_ID));
        editor.putString(MainActivity.ACCOUNT_ID, data.getString(MainActivity.ACCOUNT_ID));
        editor.putString(MainActivity.EMAIL, data.getString(MainActivity.EMAIL));

        editor.commit();
    }

    /**
     * Replaces background service on newer Android releases.
     */
    private void startWorker(Data data) {
        Log.d(tag, "startWorker");

        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SerialWorker.class)
                .setConstraints(constraints)
                .setInputData(data)
                .addTag(MainActivity.TRACKING_WORKER)
                .build();

        WorkManager.getInstance(getActivity().getApplicationContext()).enqueue(workRequest);

        Toast.makeText(getActivity(), "Service started", Toast.LENGTH_SHORT).show();
    }

    private void showRunFragment() {
        RunFragment fragment = new RunFragment();
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
